package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("post_tags")
public class PostTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long tagId;
}