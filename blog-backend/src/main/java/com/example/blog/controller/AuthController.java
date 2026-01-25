package com.example.blog.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt; // 关键：必须引入这个
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.dto.LoginRequest;
import com.example.blog.entity.User;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        try {
            String token = userService.login(request);
            return Result.success(token);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
            return Result.error("用户名或密码不能为空");
        }

        long count = userService.count(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (count > 0) {
            return Result.error("用户名已存在");
        }

        user.setRole(0);
        user.setCreateTime(LocalDateTime.now());

        // [重点修复] 注册时，密码必须加密存入数据库！
        String hashedPassword = BCrypt.hashpw(user.getPassword());
        user.setPassword(hashedPassword);

        userService.save(user);
        return Result.success("注册成功，请登录");
    }
}