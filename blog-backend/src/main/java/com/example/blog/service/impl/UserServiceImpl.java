package com.example.blog.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.digest.BCrypt; // 关键：必须引入这个
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.dto.LoginRequest;
import com.example.blog.entity.User;
import com.example.blog.mapper.UserMapper;
import com.example.blog.service.UserService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String JWT_KEY = "my-blog-secret-key-0525";

    @Override
    public String login(LoginRequest request) {
        User user = this.getOne(new QueryWrapper<User>().eq("username", request.getUsername()));

        // [重点修复] 使用 BCrypt.checkpw 进行校验
        // 如果数据库没查到用户，或者密码校验失败，抛出异常
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        Map<String, Object> payload = MapUtil.newHashMap();
        payload.put("uid", user.getId());
        payload.put("role", user.getRole());
        payload.put("exp", System.currentTimeMillis() + 1000 * 60 * 60 * 24);

        return JWTUtil.createToken(payload, JWT_KEY.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        User user = this.getById(userId);
        if (user == null) throw new RuntimeException("用户不存在");

        // [重点修复] 修改密码时也要验证旧密码是否正确
        if (!BCrypt.checkpw(oldPwd, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // [重点修复] 新密码加密后存入
        user.setPassword(BCrypt.hashpw(newPwd));
        this.updateById(user);
    }
}