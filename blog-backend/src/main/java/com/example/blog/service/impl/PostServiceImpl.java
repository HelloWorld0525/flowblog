package com.example.blog.service.impl;

import com.example.blog.entity.Post;
import com.example.blog.mapper.PostMapper;
import com.example.blog.service.PostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {
    // ServiceImpl 帮你实现了 IService 中定义的方法
}