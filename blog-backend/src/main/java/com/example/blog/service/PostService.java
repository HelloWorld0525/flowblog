package com.example.blog.service;

import com.example.blog.entity.Post;
import com.baomidou.mybatisplus.extension.service.IService;

public interface PostService extends IService<Post> {
    // 这里可以定义复杂的业务方法，简单增删改查 IService 已经包办了
}