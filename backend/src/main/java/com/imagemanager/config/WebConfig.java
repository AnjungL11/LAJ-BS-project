package com.imagemanager.config;
import com.imagemanager.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig implements WebMvcConfigurer {
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
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/data/images/");
    }
}