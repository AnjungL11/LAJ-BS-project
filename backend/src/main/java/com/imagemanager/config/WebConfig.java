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

    // 跨域配置
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径
        registry.addMapping("/**")
                // 允许所有来源
                .allowedOriginPatterns("*")
                // 允许携带Cookie/Token
                .allowCredentials(true)
                // 允许的方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许所有请求头
                .allowedHeaders("*")
                // 预检请求缓存时间
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                // 放行跨域预检
                if("OPTIONS".equals(request.getMethod())) return true;
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
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**", "/api/mcp/**");
    }
    
    // 配置静态资源映射，使前端能访问本地上传的图片
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射缩略图
        registry.addResourceHandler("/uploads/thumb/**")
                .addResourceLocations("file:" + thumbnailPath);

        // 映射原图
        registry.addResourceHandler("/uploads/original/**")
                .addResourceLocations("file:" + storagePath);
    }
}
