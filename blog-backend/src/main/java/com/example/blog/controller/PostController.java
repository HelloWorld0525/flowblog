package com.example.blog.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.Result;
import com.example.blog.entity.Post;
import com.example.blog.entity.PostTag;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.PostTagMapper;
import com.example.blog.mapper.TagMapper;
import com.example.blog.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private PostTagMapper postTagMapper;

    private static final String JWT_KEY = "my-blog-secret-key-0525";

    // 1. 分页获取列表
    @GetMapping
    public Result<Page<Post>> list(@RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) String tag) {

        Page<Post> pageParam = new Page<>(page, size);

        List<Long> tagPostIds = null;
        if (StrUtil.isNotBlank(tag)) {
            Tag t = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tag));
            if (t == null) return Result.success(pageParam);

            List<PostTag> pts = postTagMapper.selectList(new QueryWrapper<PostTag>().eq("tag_id", t.getId()));
            tagPostIds = pts.stream().map(PostTag::getPostId).collect(Collectors.toList());
            if (tagPostIds.isEmpty()) return Result.success(pageParam);
        }

        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(search)) {
            wrapper.and(w -> w.like("title", search).or().like("summary", search));
        }
        if (tagPostIds != null) {
            wrapper.in("id", tagPostIds);
        }
        wrapper.orderByDesc("create_time");

        Page<Post> result = postService.page(pageParam, wrapper);
        result.getRecords().forEach(p -> p.setTags(tagMapper.selectTagsByPostId(p.getId())));

        return Result.success(result);
    }

    // 2. [修改] 获取详情 (增加浏览量)
    @GetMapping("/{id}")
    public Result<Post> getDetail(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post != null) {
            // [新增逻辑] 浏览量 +1 并保存
            post.setViews(post.getViews() + 1);
            postService.updateById(post);

            // 填充标签
            post.setTags(tagMapper.selectTagsByPostId(id));
        }
        return Result.success(post);
    }

    // 3. 发布/更新文章
    @PostMapping
    @Transactional
    public Result<String> create(@RequestBody Post post, @RequestHeader(value = "Authorization", required = false) String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");

        if (post.getId() == null) {
            JWT jwt = JWTUtil.parseToken(token);
            Long userId = Long.valueOf(jwt.getPayload("uid").toString());
            post.setUserId(userId);
            post.setCreateTime(LocalDateTime.now());
            post.setViews(0);
        }
        post.setUpdateTime(LocalDateTime.now());
        if (post.getStatus() == null) post.setStatus(1);

        postService.saveOrUpdate(post);

        if (post.getTags() != null) {
            Long postId = post.getId();
            postTagMapper.delete(new QueryWrapper<PostTag>().eq("post_id", postId));
            for (String tagName : post.getTags()) {
                if (StrUtil.isBlank(tagName)) continue;
                Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagMapper.insert(tag);
                }
                PostTag pt = new PostTag();
                pt.setPostId(postId);
                pt.setTagId(tag.getId());
                postTagMapper.insert(pt);
            }
        }
        return Result.success("操作成功");
    }

    // 4. 删除文章 (包含文件清理)
    @DeleteMapping("/{id}")
    @Transactional
    public Result<String> delete(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");

        Post post = postService.getById(id);
        if (post != null) {
            cleanUpPostFiles(post);
            postService.removeById(id);
            postTagMapper.delete(new QueryWrapper<PostTag>().eq("post_id", id));
        }

        return Result.success("删除成功");
    }

    private void cleanUpPostFiles(Post post) {
        deleteLocalFile(post.getCoverImage());
        if (StrUtil.isNotBlank(post.getContent())) {
            Pattern pattern = Pattern.compile("/files/([a-zA-Z0-9.-]+)");
            Matcher matcher = pattern.matcher(post.getContent());
            while (matcher.find()) {
                String fileName = matcher.group(1);
                String fullPath = System.getProperty("user.dir") + File.separator + "files" + File.separator + fileName;
                FileUtil.del(fullPath);
            }
        }
    }

    private void deleteLocalFile(String url) {
        if (StrUtil.isBlank(url) || !url.contains("/files/")) return;
        try {
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            String basePath = System.getProperty("user.dir") + File.separator + "files" + File.separator;
            FileUtil.del(basePath + fileName);
        } catch (Exception e) {}
    }

    private boolean checkAdmin(String token) {
        return token != null && JWTUtil.verify(token, JWT_KEY.getBytes(StandardCharsets.UTF_8));
    }
}