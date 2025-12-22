package com.imagemanager.service;

import com.baidu.aip.imageclassify.AipImageClassify;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class AIService {

    public static final String APP_ID = "7352380";
    public static final String API_KEY = "ZE0Thl9YVN1lepYskkAq3m4Q";
    public static final String SECRET_KEY = "HR2qR5wWUwBcyswoYKn710ENAJu3IJk9";

    private final AipImageClassify client;

    public AIService() {
        // 初始化百度客户端
        client = new AipImageClassify(APP_ID, API_KEY, SECRET_KEY);
        // 设置超时参数，防止网络卡顿时一直卡死
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
    }

    /**
     * 调用百度AI分析图片
     */
    public List<String> analyzeImage(String localPath) {
        List<String> tags = new ArrayList<>();
        try {
            // 调用通用物体识别接口
            JSONObject res = client.advancedGeneral(localPath, new HashMap<>());
            
            // 解析百度返回的JSON
            if (res.has("result")) {
                JSONArray resultArray = res.getJSONArray("result");
                for (int i = 0; i < resultArray.length(); i++) {
                    JSONObject item = resultArray.getJSONObject(i);
                    String keyword = item.getString("keyword");
                    double score = item.getDouble("score");
                    // 只取置信度高的结果，且排除无意义的词
                    if (score > 0.1 && !"图像".equals(keyword)) {
                        tags.add(keyword);
                    }
                }
            } else {
                // 如果没有result字段,打印错误码
                System.err.println("百度AI返回错误: " + res.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("AI 分析异常: " + e.getMessage());
        }
        return tags;
    }
    // 调试版
    // public List<String> analyzeImage(String localPath) {
    //     List<String> tags = new ArrayList<>();
    //     try {
    //         // 调用接口
    //         JSONObject res = client.advancedGeneral(localPath, new HashMap<>());
    //         // 新增调试代码
    //         System.out.println("------------- 百度AI 原始响应开始 -------------");
    //         System.out.println(res.toString(2)); 
    //         System.out.println("------------- 百度AI 原始响应结束 -------------");
    //         // 解析逻辑
    //         if (res.has("result")) {
    //             JSONArray resultArray = res.getJSONArray("result");
    //             // 调试日志
    //             System.out.println("识别到的物体数量: " + resultArray.length());
    //             for (int i = 0; i < resultArray.length(); i++) {
    //                 JSONObject item = resultArray.getJSONObject(i);
    //                 String keyword = item.getString("keyword");
    //                 double score = item.getDouble("score");
    //                 // 打印每一个候选项
    //                 System.out.println("候选项: " + keyword + ", 置信度: " + score);
    //                 if (score > 0.1 && !"图像".equals(keyword)) {
    //                     tags.add(keyword);
    //                 }
    //             }
    //         } else {
    //             System.err.println("响应中没有 'result' 字段！可能是报错了。");
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     return tags;
    // }
}