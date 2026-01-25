package com.example.blog.service.impl;

import com.example.blog.entity.Notification;
import com.example.blog.mapper.NotificationMapper;
import com.example.blog.service.NotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Override
    public void send(Long receiverId, String senderName, String content, Long postId) {
        send(receiverId, senderName, content, postId, 1); // 默认 1-回复
    }

    @Override
    public void send(Long receiverId, String senderName, String content, Long postId, Integer type) {
        if (receiverId == null) return;

        Notification n = new Notification();
        n.setReceiverId(receiverId);
        n.setSenderName(senderName);
        n.setContent(content);
        n.setPostId(postId);
        n.setType(type);
        n.setStatus(0); // 未读
        n.setCreateTime(LocalDateTime.now());

        this.save(n);
    }
}