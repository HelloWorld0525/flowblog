package com.example.blog.service;

import com.example.blog.entity.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

public interface NotificationService extends IService<Notification> {
    // 原有方法 (默认类型1)
    void send(Long receiverId, String senderName, String content, Long postId);

    // [新增] 支持指定类型
    void send(Long receiverId, String senderName, String content, Long postId, Integer type);
}