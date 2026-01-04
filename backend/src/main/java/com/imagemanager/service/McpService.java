package com.imagemanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagemanager.entity.Image;
import com.imagemanager.mapper.ImageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class McpService {

    @Autowired
    private ImageMapper imageMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();
            
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Map<String, Object> executeSearch(String userQuery, List<String> tags, String cameraModel, String startDate, String endDate, Long userId) {
        
        System.out.println("========== AI 深度搜索 ==========");
        System.out.println("用户输入: " + userQuery);

        // 调用 DeepSeek 分析
        Map<String, Object> aiAnalysis = callDeepSeekToAnalyze(userQuery);

        String tempKeyword = (String) aiAnalysis.getOrDefault("keyword", "");
        // 如果关键词是通用词，直接置为空，防止污染文件名搜索
        if (tempKeyword != null && (tempKeyword.contains("图片") || tempKeyword.contains("照片") || tempKeyword.contains("上传"))) {
            tempKeyword = "";
        }

        String extractedKeyword = tempKeyword;
        // String extractedKeyword = (String) aiAnalysis.getOrDefault("keyword", "");
        List<String> extractedTags = (List<String>) aiAnalysis.getOrDefault("tags", new ArrayList<>());
        String aiStartDateStr = (String) aiAnalysis.get("startDate");
        String aiEndDateStr = (String) aiAnalysis.get("endDate");
        String timeText = (String) aiAnalysis.get("timeText");

        System.out.println("AI 时间范围: " + aiStartDateStr + " ~ " + aiEndDateStr);

        // 解析时间
        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (StringUtils.hasText(aiStartDateStr)) start = LocalDateTime.parse(aiStartDateStr, fmt);
            if (StringUtils.hasText(aiEndDateStr)) end = LocalDateTime.parse(aiEndDateStr, fmt);
        } catch (Exception e) {
            System.err.println("时间解析错误: " + e.getMessage());
        }

        // 构建查询
        QueryWrapper<Image> wrapper = new QueryWrapper<>();
        
        // 内容匹配
        List<String> contentTags = new ArrayList<>();
        if (tags != null) contentTags.addAll(tags);
        if (extractedTags != null) contentTags.addAll(extractedTags);

        boolean hasContentCondition = StringUtils.hasText(extractedKeyword) || !contentTags.isEmpty();

        if (hasContentCondition) {
            wrapper.and(w -> {
                if (StringUtils.hasText(extractedKeyword)) {
                    w.like("original_filename", extractedKeyword);
                }
                if (!contentTags.isEmpty()) {
                    for (String tag : contentTags) {
                        w.or().apply("EXISTS (SELECT 1 FROM image_tags it JOIN tags t ON it.tag_id = t.tag_id WHERE t.tag_name LIKE {0} AND it.image_id = images.image_id)", "%" + tag + "%");
                    }
                    if (StringUtils.hasText(extractedKeyword)) {
                         w.or().apply("EXISTS (SELECT 1 FROM image_tags it JOIN tags t ON it.tag_id = t.tag_id WHERE t.tag_name LIKE {0} AND it.image_id = images.image_id)", "%" + extractedKeyword + "%");
                    }
                }
            });
        }

        // 时间匹配
        boolean hasTimeCondition = (start != null && end != null) || StringUtils.hasText(timeText);

        if (hasTimeCondition) {
            final LocalDateTime fStart = start;
            final LocalDateTime fEnd = end;
            
            wrapper.and(w -> {
                // 匹配标签
                if (StringUtils.hasText(timeText)) {
                    w.apply("EXISTS (SELECT 1 FROM image_tags it JOIN tags t ON it.tag_id = t.tag_id WHERE t.tag_name LIKE {0} AND it.image_id = images.image_id)", "%" + timeText + "%");
                }

                if (fStart != null && fEnd != null) {
                    // 匹配上传时间
                    w.or().between("uploaded_at", fStart, fEnd);
                    
                    // 匹配EXIF拍摄时间
                    w.or().between("taken_time", fStart, fEnd);
                }
            });
        }
    
        // 兜底
        if (!hasContentCondition && !hasTimeCondition && StringUtils.hasText(userQuery)) {
            wrapper.like("original_filename", userQuery);
        }

        // 执行
        List<Image> results;
        try {
            results = imageMapper.selectList(wrapper);
        } catch (Exception e) {
            System.err.println("SQL 查询出错，可能是 metadata 列不存在或 JSON 语法不支持: " + e.getMessage());
            // 如果出错，尝试降级查询（只查上传时间）
            if (hasTimeCondition && start != null) {
                QueryWrapper<Image> fallback = new QueryWrapper<>();
                fallback.between("uploaded_at", start, end);
                results = imageMapper.selectList(fallback);
            } else {
                results = new ArrayList<>();
            }
        }
        
        // 返回结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", results);
        
        StringBuilder reply = new StringBuilder();
        if (results.isEmpty()) {
            reply.append("未找到图片。");
            if (StringUtils.hasText(timeText)) reply.append(" 时间:").append(timeText);
        } else {
            reply.append("为您找到 ").append(results.size()).append(" 张图片。");
        }
        resultMap.put("reply", reply.toString());

        return resultMap;
    }

    private Map<String, Object> callDeepSeekToAnalyze(String userQuery) {
        Map<String, Object> result = new HashMap<>();
        try {
            String today = LocalDate.now().toString();
            // String systemPrompt = "你是一个图库搜索助手。当前日期: " + today + "。\n" +
            //         "请分析输入，提取JSON：\n" +
            //         "1. keyword: 除去时间的内容关键词\n" +
            //         "2. tags: 除去时间的内容标签\n" +
            //         "3. timeText: 用户描述时间的原始文本\n" +
            //         "4. startDate/endDate: 时间范围(yyyy-MM-dd HH:mm:ss)\n\n" +
            //         "示例: '2024年12月'\n" +
            //         "返回: {\"keyword\":\"\",\"tags\":[],\"timeText\":\"2024年12月\",\"startDate\":\"2024-12-01 00:00:00\",\"endDate\":\"2024-12-31 23:59:59\"}";
            String systemPrompt = "你是一个图库搜索助手。当前日期: " + today + "。\n" +
                    "请分析输入，提取JSON：\n" +
                    "1. keyword: 除去时间、以及'图片'、'照片'、'查找'、'搜索'、'上传'、'找一下'等无意义通用词汇后的实体关键词。如果剩余词汇无实际检索意义，请返回空字符串。\n" +
                    "2. tags: 除去时间的内容标签\n" +
                    "3. timeText: 用户描述时间的原始文本\n" +
                    "4. startDate/endDate: 时间范围(yyyy-MM-dd HH:mm:ss)\n\n" +
                    "示例1: '2024年12月'\n" +
                    "返回: {\"keyword\":\"\",\"tags\":[],\"timeText\":\"2024年12月\",\"startDate\":\"2024-12-01 00:00:00\",\"endDate\":\"2024-12-31 23:59:59\"}\n\n" +
                    "示例2: '找一下去年拍摄的红色阀门'\n" +
                    "返回: {\"keyword\":\"阀门\",\"tags\":[\"红色\",\"阀门\"],\"timeText\":\"去年\",\"startDate\":\"...\",\"endDate\":\"...\"}";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userQuery)
            ));
            requestBody.put("temperature", 0.0);
            requestBody.put("response_format", Map.of("type", "json_object"));

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                JsonNode aiData = objectMapper.readTree(content);
                if (aiData.has("keyword")) result.put("keyword", aiData.get("keyword").asText());
                if (aiData.has("timeText")) result.put("timeText", aiData.get("timeText").asText());
                if (aiData.has("startDate")) result.put("startDate", aiData.get("startDate").asText());
                if (aiData.has("endDate")) result.put("endDate", aiData.get("endDate").asText());
                if (aiData.has("tags")) {
                    List<String> tagList = new ArrayList<>();
                    aiData.get("tags").forEach(t -> tagList.add(t.asText()));
                    result.put("tags", tagList);
                }
            } else {
                result.put("keyword", userQuery);
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("keyword", userQuery);
        }
        return result;
    }
}