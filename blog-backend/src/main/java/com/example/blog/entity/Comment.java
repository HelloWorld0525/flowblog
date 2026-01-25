package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 引入 TableField
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;
    private String nickname;
    private String email;
    private String content;
    private Long parentId;
    private Integer isAdmin;
    private Integer status;

    private LocalDateTime createTime;

    // [新增] 用于给前端展示最新头像，数据库中不存在此列
    @TableField(exist = false)
    private String userAvatar;
}