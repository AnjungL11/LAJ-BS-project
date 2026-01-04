package com.imagemanager.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.imagemanager.entity.User;
import com.imagemanager.mapper.UserMapper;
import com.imagemanager.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    // 定义邮箱格式的正则表达式
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        // 非空检查
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }

        // 密码长度验证，大于6字节
        if (user.getPasswordHash().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        // 邮箱格式验证
        if (!Pattern.matches(EMAIL_REGEX, user.getEmail())) {
            throw new RuntimeException("邮箱格式不正确 (例如: user@example.com)");
        }

        // 检查用户名是否重复
        User existName = userMapper.selectOne(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (existName != null) {
            throw new RuntimeException("该用户名已被注册，请更换");
        }

        // 检查邮箱是否重复
        User existEmail = userMapper.selectOne(new QueryWrapper<User>().eq("email", user.getEmail()));
        if (existEmail != null) {
            throw new RuntimeException("该邮箱已被注册，请直接登录");
        }

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
