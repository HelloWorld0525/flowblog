package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname; // [新增] 昵称字段
    private String password;
    private String email;
    private String avatar;
    private Integer role;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}