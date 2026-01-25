package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private String senderName;
    private String content;
    private Long postId;
    private Integer type;   // 1-回复
    private Integer status; // 0-未读
    private LocalDateTime createTime;
}