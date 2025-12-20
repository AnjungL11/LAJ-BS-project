package com.imagemanager.config;
import com.imagemanager.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 读取 application.yml 中的路径配置
    @Value("${app.storage-path}")
    private String storagePath; // D:/data/images/original/
    
    @Value("${app.thumbnail-path}")
    private String thumbnailPath; // D:/data/images/thumbs/

    @PostConstruct
    public void printConfig() {
        System.out.println("========================================");
        System.out.println("【调试日志】原图存储路径: " + storagePath);
        System.out.println("【调试日志】缩略图存储路径: " + thumbnailPath);
        System.out.println("========================================");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if("OPTIONS".equals(request.getMethod())) return true; // 放行跨域预检
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    try {
                        Claims claims = JwtUtil.parseToken(authHeader.substring(7));
                        request.setAttribute("userId", claims.get("userId").toString());
                        return true;
                    } catch (Exception e) {}
                }
                response.setStatus(401);
                return false;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**");
    }
    
    // 配置静态资源映射，使前端能访问本地上传的图片
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 映射缩略图：访问 http://localhost:8080/uploads/thumb/xxx.jpg -> 本地 D:/data/images/thumbs/xxx.jpg
        registry.addResourceHandler("/uploads/thumb/**")
                .addResourceLocations("file:" + thumbnailPath);

        // 2. 映射原图：访问 http://localhost:8080/uploads/original/xxx.jpg -> 本地 D:/data/images/original/xxx.jpg
        registry.addResourceHandler("/uploads/original/**")
                .addResourceLocations("file:" + storagePath);
    }
}