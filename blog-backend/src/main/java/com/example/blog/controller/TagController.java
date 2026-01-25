package com.example.blog.controller;

import com.example.blog.common.Result;
import com.example.blog.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    // 获取所有标签（用于前端标签云）
    @GetMapping
    public Result<List<String>> list() {
        // [修改] 只返回有关联文章的标签
        return Result.success(tagMapper.selectTagsWithPosts());
    }
}