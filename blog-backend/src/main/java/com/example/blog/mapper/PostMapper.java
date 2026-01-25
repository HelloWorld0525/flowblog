package com.example.blog.mapper;

import com.example.blog.entity.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PostMapper extends BaseMapper<Post> {
    // 继承 BaseMapper，自动拥有 insert, delete, update, selectById, selectList 等方法
}