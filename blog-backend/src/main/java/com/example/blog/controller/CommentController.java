package com.example.blog.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.entity.Comment;
import com.example.blog.entity.Post;
import com.example.blog.entity.User;
import com.example.blog.service.CommentService;
import com.example.blog.service.NotificationService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    private static final String JWT_KEY = "my-blog-secret-key-0525";

    // 1. 获取评论列表
    @GetMapping
    public Result<List<Comment>> list(@RequestParam Long postId) {
        List<Comment> list = commentService.list(new QueryWrapper<Comment>()
                .eq("post_id", postId)
                .eq("status", 1)
                .orderByDesc("create_time"));

        // [新增] 填充最新用户信息
        fillUserInfos(list);

        return Result.success(list);
    }

    // 2. 管理员列表
    @GetMapping("/admin")
    public Result<List<Comment>> listAdmin(@RequestHeader(value = "Authorization", required = false) String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        List<Comment> list = commentService.lambdaQuery()
                .orderByAsc(Comment::getStatus)
                .orderByDesc(Comment::getCreateTime)
                .list();

        // [新增] 填充最新用户信息
        fillUserInfos(list);

        return Result.success(list);
    }

    // [新增] 辅助方法：填充头像和最新昵称
    private void fillUserInfos(List<Comment> comments) {
        if (CollUtil.isEmpty(comments)) return;

        // 提取所有 userId
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(userIds)) return;

        // 批量查询用户
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 回填数据
        for (Comment c : comments) {
            if (c.getUserId() != null && userMap.containsKey(c.getUserId())) {
                User u = userMap.get(c.getUserId());
                // 如果用户设置了昵称，覆盖评论里的旧昵称
                if (StrUtil.isNotBlank(u.getNickname())) {
                    c.setNickname(u.getNickname());
                } else {
                    c.setNickname(u.getUsername());
                }
                // 设置头像
                c.setUserAvatar(u.getAvatar());
            }
        }
    }

    // 3. 发表评论
    @PostMapping
    public Result<String> create(@RequestBody Comment comment, @RequestHeader(value = "Authorization", required = false) String token) {
        if (StrUtil.isBlank(comment.getContent())) return Result.error("内容为空");

        comment.setCreateTime(LocalDateTime.now());
        comment.setIsAdmin(0);

        Long currentUserId = null;

        if (StrUtil.isNotBlank(token) && JWTUtil.verify(token, JWT_KEY.getBytes(StandardCharsets.UTF_8))) {
            JWT jwt = JWTUtil.parseToken(token);
            currentUserId = Long.valueOf(jwt.getPayload("uid").toString());
            Integer role = Integer.valueOf(jwt.getPayload("role").toString());

            comment.setUserId(currentUserId);
            comment.setStatus(1);
            if (role == 1) comment.setIsAdmin(1);

            // 查最新昵称入库
            User currentUser = userService.getById(currentUserId);
            if (currentUser != null) {
                String displayName = StrUtil.isNotBlank(currentUser.getNickname()) ? currentUser.getNickname() : currentUser.getUsername();
                comment.setNickname(displayName);
            }
        } else {
            if (StrUtil.isBlank(comment.getNickname())) return Result.error("需填写昵称");
            comment.setUserId(null);
            comment.setStatus(0);
        }

        boolean save = commentService.save(comment);

        if (save) {
            if (comment.getStatus() == 1) {
                sendReplyNotification(comment, currentUserId);
            } else {
                sendAuditNotification(comment);
            }
        }

        return Result.success(comment.getStatus() == 1 ? "评论成功" : "已提交审核");
    }

    // ... 其他 pass, delete, sendReplyNotification, sendAuditNotification, checkAdmin 等方法保持不变 ...
    // (为了代码块完整性，请确保保留之前实现的其他方法)

    @PutMapping("/{id}/pass")
    public Result<String> pass(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        Comment comment = new Comment();
        comment.setId(id);
        comment.setStatus(1);
        commentService.updateById(comment);
        return Result.success("审核通过");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        commentService.removeById(id);
        return Result.success("删除成功");
    }

    private void sendReplyNotification(Comment currentComment, Long currentUserId) {
        if (currentComment.getParentId() != null) {
            Comment parent = commentService.getById(currentComment.getParentId());
            if (parent != null && parent.getUserId() != null) {
                if (!parent.getUserId().equals(currentUserId)) {
                    notificationService.send(parent.getUserId(), currentComment.getNickname(), "回复了你: " + StrUtil.maxLength(currentComment.getContent(), 20), currentComment.getPostId(), 1);
                }
            }
        } else {
            Post post = postService.getById(currentComment.getPostId());
            if (post != null && !post.getUserId().equals(currentUserId)) {
                notificationService.send(post.getUserId(), currentComment.getNickname(), "评论了文章: " + StrUtil.maxLength(currentComment.getContent(), 20), currentComment.getPostId(), 1);
            }
        }
    }

    private void sendAuditNotification(Comment comment) {
        List<User> admins = userService.list(new QueryWrapper<User>().eq("role", 1));
        for (User admin : admins) {
            notificationService.send(admin.getId(), "系统提醒", "新游客评论待审核: " + StrUtil.maxLength(comment.getContent(), 15), comment.getPostId(), 2);
        }
    }

    private boolean checkAdmin(String token) {
        return token != null && JWTUtil.verify(token, JWT_KEY.getBytes(StandardCharsets.UTF_8));
    }
}