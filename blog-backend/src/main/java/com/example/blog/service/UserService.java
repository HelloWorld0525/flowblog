package com.example.blog.service;

import com.example.blog.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    String login(com.example.blog.dto.LoginRequest request);

    // [新增] 修改密码
    void changePassword(Long userId, String oldPwd, String newPwd);
}