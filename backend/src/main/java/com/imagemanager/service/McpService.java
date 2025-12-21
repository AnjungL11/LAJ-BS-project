package com.imagemanager.service;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class McpService {
    
    /**
     * 模拟MCP协议：将自然语言转换为SQL查询条件
     */
    public List<String> parseNaturalLanguage(String prompt) {
        if (prompt.contains("狗")) return List.of("dog", "animal");
        if (prompt.contains("风景")) return List.of("scenery", "landscape");
        return List.of();
    }
}