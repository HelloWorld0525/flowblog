package com.example.blog.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.example.blog.common.Result;
import com.example.blog.entity.User;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public Result<User> getCurrentUser(@RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        User user = userService.getById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/me")
    public Result<String> updateInfo(@RequestBody User user, @RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        user.setId(userId);

        // [新增] 删除旧头像逻辑
        User oldUser = userService.getById(userId);
        if (oldUser != null && StrUtil.isNotBlank(oldUser.getAvatar()) && !oldUser.getAvatar().equals(user.getAvatar())) {
            // 如果旧头像存在且与新头像不同
            String oldAvatar = oldUser.getAvatar();
            // 简单判断是否是本地文件 (包含 /files/)
            if (oldAvatar.contains("/files/")) {
                try {
                    // 解析文件名：http://localhost:8080/files/xxx.jpg -> xxx.jpg
                    String fileName = oldAvatar.substring(oldAvatar.lastIndexOf("/") + 1);
                    String basePath = System.getProperty("user.dir") + File.separator + "files" + File.separator;
                    // 删除文件
                    FileUtil.del(basePath + fileName);
                } catch (Exception e) {
                    System.err.println("删除旧头像失败: " + e.getMessage());
                }
            }
        }

        // 禁止修改敏感字段
        user.setRole(null);
        user.setPassword(null);
        user.setUsername(null);

        userService.updateById(user);
        return Result.success("更新成功");
    }

    // ... updatePassword 和 getUserId 保持不变 ...
    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token) {
        Long userId = getUserId(token);
        String oldPwd = params.get("oldPassword");
        String newPwd = params.get("newPassword");
        if (StrUtil.isBlank(oldPwd) || StrUtil.isBlank(newPwd)) return Result.error("参数不完整");
        try {
            userService.changePassword(userId, oldPwd, newPwd);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Long getUserId(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            return Long.valueOf(jwt.getPayload("uid").toString());
        } catch (Exception e) { throw new RuntimeException("Token无效"); }
    }
}