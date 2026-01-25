package com.example.blog.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.entity.Notification;
import com.example.blog.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    private static final String JWT_KEY = "my-blog-secret-key-0525";

    // 1. 获取未读数量
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader(value = "Authorization", required = false) String token) {
        if (StrUtil.isBlank(token)) return Result.success(0L);
        Long userId = getUserId(token);
        long count = notificationService.count(new QueryWrapper<Notification>().eq("receiver_id", userId).eq("status", 0));
        return Result.success(count);
    }

    // 2. 获取个人消息列表 (含全员公告)
    @GetMapping
    public Result<List<Notification>> list(@RequestHeader(value = "Authorization", required = false) String token) {
        QueryWrapper<Notification> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(token)) {
            Long userId = getUserId(token);
            wrapper.and(w -> w.eq("receiver_id", userId).or().eq("receiver_id", 0));
        } else {
            wrapper.eq("receiver_id", 0);
        }
        wrapper.orderByDesc("create_time");
        return Result.success(notificationService.list(wrapper));
    }

    // 3. [新增] 获取纯公告列表 (仅管理员)
    @GetMapping("/admin/announcements")
    public Result<List<Notification>> listAnnouncements(@RequestHeader("Authorization") String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        return Result.success(notificationService.lambdaQuery()
                .eq(Notification::getReceiverId, 0) // 0 代表全员
                .eq(Notification::getType, 3)       // 3 代表公告
                .orderByDesc(Notification::getCreateTime)
                .list());
    }

    // 4. 发布公告
    @PostMapping("/broadcast")
    public Result<String> broadcast(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        String content = params.get("content");
        if (StrUtil.isBlank(content)) return Result.error("内容为空");
        notificationService.send(0L, "站长公告", content, null, 3);
        return Result.success("公告发布成功");
    }

    // 5. 标记已读
    @PutMapping("/{id}/read")
    public Result<String> read(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        if (StrUtil.isBlank(token)) return Result.success("OK");
        Notification n = new Notification();
        n.setId(id);
        n.setStatus(1);
        notificationService.updateById(n);
        return Result.success("已读");
    }

    @PutMapping("/read-all")
    public Result<String> readAll(@RequestHeader(value = "Authorization", required = false) String token) {
        if (StrUtil.isBlank(token)) return Result.success("OK");
        Long userId = getUserId(token);
        Notification update = new Notification();
        update.setStatus(1);
        notificationService.update(update, new QueryWrapper<Notification>().eq("receiver_id", userId).eq("status", 0));
        return Result.success("操作成功");
    }

    // 6. [新增] 删除单个通知 (用于删除公告)
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (!checkAdmin(token)) return Result.error("无权操作");
        notificationService.removeById(id);
        return Result.success("删除成功");
    }

    // 7. 批量删除
    @DeleteMapping
    public Result<String> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Result.error("未选择");
        notificationService.removeByIds(ids);
        return Result.success("删除成功");
    }

    private Long getUserId(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            return Long.valueOf(jwt.getPayload("uid").toString());
        } catch (Exception e) { return 0L; }
    }

    private boolean checkAdmin(String token) {
        return token != null && JWTUtil.verify(token, JWT_KEY.getBytes(StandardCharsets.UTF_8));
    }
}