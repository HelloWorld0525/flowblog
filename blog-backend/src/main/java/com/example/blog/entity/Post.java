package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("posts")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private String category;
    private Integer status;
    private Integer views;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // [新增] 标签列表 (数据库 posts 表里没有这个字段，是关联查出来的)
    @TableField(exist = false)
    private List<String> tags;
}