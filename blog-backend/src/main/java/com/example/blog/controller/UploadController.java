package com.example.blog.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.example.blog.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class UploadController {

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        // 1. 准备存储目录 (项目根目录/files/)
        String basePath = System.getProperty("user.dir") + File.separator + "files" + File.separator;
        if (!FileUtil.exist(basePath)) {
            FileUtil.mkdir(basePath);
        }

        // 2. 生成唯一文件名 (防止重名覆盖)
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + "." + suffix;

        // 3. 保存文件
        file.transferTo(new File(basePath + fileName));

        // 4. 返回可访问的 URL
        // [修改前] String url = "http://localhost:8080/files/" + fileName;
        // [修改后] 使用相对路径，让浏览器自动适配当前 IP/域名
        String url = "/files/" + fileName;

        return Result.success(url);
    }
}