package com.imagemanager.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.imagemanager.entity.User;
import com.imagemanager.mapper.UserMapper;
import com.imagemanager.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin // 允许跨域
public class AuthController {

    @Autowired private UserMapper userMapper;

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        // 验证逻辑省略...
        // 密码加密
        user.setPasswordHash(BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
        userMapper.insert(user);
        return "Register Success";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String password = params.get("password");
        
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", email));
        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            String token = JwtUtil.generateToken(user.getUserId(), user.getUsername());
            return Map.of("token", token, "user", user);
        }
        throw new RuntimeException("Invalid credentials");
    }
}