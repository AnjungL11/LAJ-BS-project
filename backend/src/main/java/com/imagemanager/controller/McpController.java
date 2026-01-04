package com.imagemanager.controller;

import com.imagemanager.common.Result;
import com.imagemanager.service.McpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    @Autowired
    private McpService mcpService;

    /**
     * 接收MCP脚本的搜索请求
     */
    @PostMapping("/search")
    public Result<Map<String, Object>> mcpSearch(@RequestBody Map<String, Object> params) {
        try {
            // 把前端传过来的映射给keyword
            String keyword = (String) params.get("keyword");
            
            // 兼容性处理
            if (keyword == null && params.containsKey("query")) {
                keyword = (String) params.get("query");
            }

            List<String> tags = (List<String>) params.get("tags");
            String cameraModel = (String) params.get("cameraModel");
            String startDate = (String) params.get("startDate");
            String endDate = (String) params.get("endDate");
            Long userId = 1L; 

            // 调用Service
            Map<String, Object> response = mcpService.executeSearch(keyword, tags, cameraModel, startDate, endDate, userId);
            
            return Result.success(response);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索失败: " + e.getMessage());
        }
    }
}
