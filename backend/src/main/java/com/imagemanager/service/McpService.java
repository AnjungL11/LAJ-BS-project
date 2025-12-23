package com.imagemanager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.imagemanager.dto.ImageSearchRequest;
import com.imagemanager.entity.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class McpService {

    @Autowired
    private ImageService imageService;

    // 获取当前运行端口和路径用于生成图片链接
    @Value("${server.port:8080}")
    private String serverPort;

    private static final String FRONTEND_URL = "http://localhost:3000";

    /**
     * 执行智能搜索
     * 如果提供了keyword，既查文件名，也查标签，取并集
     */
    public String executeSearch(String keyword, List<String> tags, String cameraModel, String startDate, String endDate, Long userId) {
        
        // 准备结果集合
        List<Image> finalResults = new ArrayList<>();

        // 解析时间参数
        LocalDateTime start = null;
        LocalDateTime end = null;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.hasText(startDate)) start = LocalDateTime.parse(startDate, fmt);
        if (StringUtils.hasText(endDate)) end = LocalDateTime.parse(endDate, fmt);

        // 把keyword当作"文件名"进行搜索
        ImageSearchRequest reqFilename = new ImageSearchRequest();
        reqFilename.setPage(1);
        reqFilename.setSize(20);
        // 关键词->文件名
        reqFilename.setFilename(keyword);
        // 只有显式指定的tags
        reqFilename.setTags(tags);
        reqFilename.setCameraModel(cameraModel);
        reqFilename.setStartTime(start);
        reqFilename.setEndTime(end);

        Page<Image> resultFilename = imageService.searchAdvanced(reqFilename, userId);
        finalResults.addAll(resultFilename.getRecords());

        // 如果keyword存在，把它当作"标签"再搜一次
        if (StringUtils.hasText(keyword)) {
            ImageSearchRequest reqTag = new ImageSearchRequest();
            reqTag.setPage(1);
            reqTag.setSize(20);
            // 这里不设filename，防止文件名过滤太严格
            // 合并keyword到tags列表里
            List<String> searchTags = new ArrayList<>();
            if (tags != null) searchTags.addAll(tags);
            searchTags.add(keyword);
            reqTag.setTags(searchTags);
            reqTag.setCameraModel(cameraModel);
            reqTag.setStartTime(start);
            reqTag.setEndTime(end);
            Page<Image> resultTags = imageService.searchAdvanced(reqTag, userId);
            finalResults.addAll(resultTags.getRecords());
        }
        // 结果去重
        List<Image> uniqueImages = finalResults.stream()
                .distinct()
                .collect(Collectors.toMap(Image::getImageId, img -> img, (existing, replacement) -> existing))
                .values()
                .stream()
                .toList();

        if (uniqueImages.isEmpty()) {
            return "未找到符合条件的图片。";
        }

        // 格式化为Markdown
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("找到 %d 张图片：\n\n", uniqueImages.size()));

        for (Image img : uniqueImages) {
            String filename = "";
            if (img.getStoragePath() != null) {
                String path = img.getStoragePath().replace("\\", "/");
                filename = path.substring(path.lastIndexOf("/") + 1);
            }
            
            String thumbUrl = String.format("%s/uploads/thumb/%s", FRONTEND_URL, filename);

            sb.append(String.format("### %s\n", img.getOriginalFilename()));
            sb.append(String.format("**标签**: %s\n", img.getTags() != null ? String.join(", ", img.getTags()) : "无"));
            sb.append(String.format("**时间**: %s\n", img.getMetadata() != null && img.getMetadata().getTakenTime() != null ? img.getMetadata().getTakenTime() : formatTime(img.getUploadedAt())));
            sb.append(String.format("![缩略图](%s)\n", thumbUrl));
            sb.append("---\n");
        }

        return sb.toString();
    }
    
    // 简单的辅助方法格式化时间
    private String formatTime(LocalDateTime time) {
        if (time == null) return "未知";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}