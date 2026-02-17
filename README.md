# FlowBlog - 全栈个人博客系统

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7+-green)
![Vue](https://img.shields.io/badge/Vue.js-3.0-4FC08D)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Nginx](https://img.shields.io/badge/Nginx-Reverse%20Proxy-009639)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-Automated-2088FF)

> 一个基于 **Spring Boot + Vue 3** 前后端分离架构开发的个人博客系统。
> 特色在于采用了 **GitHub Actions + Linux Native (原生)** 的自动化部署方案，在低配服务器上实现了高性能、低内存占用的持续集成交付。
>
> 👉 [点击查看为什么我选择这个部署架构](docs/deployment-why.md)
>
> 👉[备份迁移](docs/备份迁移.md)
>
> 成果展示：[flowblog.top](https://flowblog.top/)

---

## ✨ 项目亮点 (Key Features)

- **全栈架构**：标准的前后端分离设计，RESTful API 风格交互。
- **自动化运维**：基于 GitHub Actions 实现的双流水线（前端/后端独立部署），提交即上线。
- **原生部署优化**：摒弃 Docker 容器化的重资源消耗，直接在 Linux 宿主机运行 Nginx 与 Java，最大化利用服务器性能。
- **静态资源映射**：通过 Nginx `alias` 指令与 Java 本地文件系统打通，实现高效的图片存储与访问。
- **安全增强**：
  - Nginx 反向代理隐藏后端端口。
  - 配置 `Content-Security-Policy` 自动升级 HTTP 资源，解决 HTTPS 环境下的混合内容 (Mixed Content) 问题。

## 🛠 技术栈 (Tech Stack)

### 前端 (Frontend)

- **核心框架**: Vue.js 3
- **构建工具**: Vite / Webpack
- **路由管理**: Vue Router (History 模式)
- **HTTP 客户端**: Axios

### 后端 (Backend)

- **开发语言**: Java 17
- **核心框架**: Spring Boot
- **ORM 框架**: MyBatis-Plus
- **数据库**: MySQL (数据库名: `my_blog_db`)
- **工具库**: Hutool, Lombok

### 部署与运维 (DevOps)

- **Web 服务器**: Nginx (反向代理 + 静态资源托管)
- **CI/CD**: GitHub Actions
- **脚本**: Shell Scripting (自动化重启与文件清洗)

---

## 🏗️ 系统架构与部署 (Architecture)

### 1. 目录结构

服务器部署路径：`/home/flowblog/`

```text
/home/flowblog/
├── blog-frontend/       # [前端] 存放 Vue 打包后的 dist 静态文件
├── blog-backend/        # [后端] 存放 Java Jar 包
│   ├── flowblog.jar     # 运行的主程序
│   └── files/           # [存储] 用户上传的图片/文件 (由 Nginx 直接映射读取)
├── restart.sh           # [脚本] 后端自动化重启脚本
└── app.log              # [日志] 运行日志
```

### 2. CI/CD 流水线设计

本项目采用 **双流水线独立触发** 机制，确保高效构建：

- **🟢 前端流水线 (`deploy-frontend.yml`)**:
  - 监听 `blog-frontend/` 目录变动。
  - GitHub Runner 执行 `npm install` & `npm run build`。
  - 通过 SCP 将 `dist` 产物推送到 Nginx 托管目录。
  - **特点**: 秒级更新，无需重启后端服务。
- **🔵 后端流水线 (`deploy-backend.yml`)**:
  - 监听 `blog-backend/` 目录变动。
  - GitHub Runner 执行 `mvn clean package`。
  - SCP 传输 Jar 包 -> 执行 `restart.sh` 脚本。
  - **脚本逻辑**: 强制进入项目目录 (解决 `user.dir` 路径问题) -> Kill 旧进程 -> 启动新进程。

------

## 🚀 快速开始 (Quick Start)

如果你想在本地运行此项目，请参照以下步骤：

### 1. 环境准备

- JDK 17+
- Node.js 18+ (推荐 20/22)
- MySQL 8.0+

### 2. 数据库设置

创建数据库 `my_blog_db` 并导入项目提供的 SQL 脚本。

SQL

```
CREATE DATABASE my_blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

### 3. 后端启动

1. 进入 `blog-backend` 目录。

2. 修改 `src/main/resources/application.yml` 中的数据库配置：

   YAML

   ```
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/my_blog_db?...
       username: root
       password: your_password
   ```

3. 运行项目：

   Bash

   ```
   mvn spring-boot:run
   ```

### 4. 前端启动

1. 进入 `blog-frontend` 目录。

2. 安装依赖并启动：

   Bash

   ```
   npm install
   npm run dev
   ```

------

## ⚙️ Nginx 核心配置参考

生产环境 Nginx 配置 (`/etc/nginx/conf.d/flowblog.conf`) 关键片段：

Nginx

```
server {
    listen 80;
    server_name yourdomain.com;

    # 1. 自动升级混合内容 (HTTP -> HTTPS)
    add_header Content-Security-Policy "upgrade-insecure-requests";

    # 2. 前端静态页面
    location / {
        root /home/flowblog/blog-frontend;
        try_files $uri $uri/ /index.html; # 解决 Vue History 模式刷新 404
    }

    # 3. 后端接口反向代理
    location /api/ {
        proxy_pass [http://127.0.0.1:8080](http://127.0.0.1:8080);
        proxy_set_header Host $host;
    }

    # 4. 本地文件存储映射 (关键)
    location /files/ {
        alias /home/flowblog/blog-backend/files/; # 映射到服务器硬盘路径
        expires 7d;
    }
}
```

------

## 🤝 贡献与反馈

欢迎提交 Issue 或 Pull Request。如果你觉得这个项目对你有帮助，请给一个 Star ⭐️！

