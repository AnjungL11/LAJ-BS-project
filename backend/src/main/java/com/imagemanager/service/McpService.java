package com.imagemanager.service;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class McpService {
    
    /**
     * 模拟 MCP 协议：将自然语言转换为 SQL 查询条件
     * 用户输入: "帮我找上个月拍的风景照"
     * 转换逻辑: Date range = last month, Tag = 'scenery'
     */
    public List<String> parseNaturalLanguage(String prompt) {
        // 实际开发中，这里会调用 LLM API (如 GPT-4) 进行意图识别
        // 这里返回 Mock 数据
        if (prompt.contains("狗")) return List.of("dog", "animal");
        if (prompt.contains("风景")) return List.of("scenery", "landscape");
        return List.of();
    }
}