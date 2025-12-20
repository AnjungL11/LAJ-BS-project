package com.imagemanager.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 拦截所有Controller抛出的异常，将异常信息封装成前端能读懂的JSON格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        // 构造返回给前端的SONJ数据
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 500);
        // 将AuthController中throw new RuntimeException("...")的文字提取出来
        errorResponse.put("message", e.getMessage()); 
        errorResponse.put("timestamp", System.currentTimeMillis());

        // 返回HTTP 500状态码，并携带上述JSON
        return ResponseEntity.status(500).body(errorResponse);
    }
}