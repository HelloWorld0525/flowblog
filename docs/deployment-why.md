从“纯 Docker 容器化部署”向“GitHub Actions 自动化 + 本机原生混合部署”的部署架构大迁移。

------

### 📝 前言：为什么选择“GitHub Actions + 本机原生混合部署”？

在决定技术方案时，**“适合”永远比“先进”更重要**。之所以选择 **GitHub Actions (CI/CD) + Linux 原生运行 (Native)** 的组合，是基于flowblog当前服务器配置（2核/小内存2Gib）和项目规模（个人博客）的最优解。

#### 1. 为什么不用 Docker / K8s？

- **资源开销**：Docker 即使不运行容器，其守护进程（Daemon）也会占用几十 MB 内存。如果用 Docker 运行 MySQL + Java + Nginx，内存消耗会显著增加。对于低配服务器（如 1G/2G 内存），**原生运行（直接安装 JDK 和 Nginx）能节省出宝贵的 20%~30% 内存**给业务使用。
- **调试难度**：原生文件系统通过 `ls`、`cat` 就能直接查看（正如我们在排查图片路径时那样）。如果用 Docker，你需要 `docker exec` 进入容器，还要处理复杂的存储卷挂载（Volume Mapping），**排错门槛高，需要一定的学习成本**。

#### 2. 为什么不用 Jenkins？

- **太重了**：Jenkins 是 Java 写的老牌工具，运行它自己就需要大量的内存（通常建议 1G 以上）。你需要在服务器上专门养一个“管家”，这对于单体应用来说是本末倒置。

#### 3. 为什么选择 GitHub Actions？

- **云端运行**：它的构建（编译、打包）是在 GitHub 提供的服务器上完成的，**不消耗你自己的服务器资源**。
- **GitOps**：与代码仓库完美集成，`git push` 就能触发，无需搭建额外平台。

#### 📊 主流自动化部署方案对比表

| **方案**                      | **资源消耗 (服务器端)** | **维护复杂度**                        | **适用场景**                       | **我的情况**     |
| ----------------------------- | ----------------------- | ------------------------------------- | ---------------------------------- | ---------------- |
| **GitHub Actions + 原生部署** | ⭐ (极低)                | ⭐⭐ (需写脚本，<br />AI的发展抵消此条) | **个人博客、低配服务器、单体应用** | ✅ **最佳选择**   |
| **Docker Compose**            | ⭐⭐⭐ (中等)              | ⭐⭐ (容器编排)                         | 微服务入门、环境隔离要求高         | ❌ 内存可能吃紧   |
| **Jenkins**                   | ⭐⭐⭐⭐⭐ (极高)            | ⭐⭐⭐⭐ (运维重)                         | 企业级、复杂构建链、内网部署       | ❌ 杀鸡用牛刀     |
| **Kubernetes (K8s)**          | ⭐⭐⭐⭐⭐ (爆炸)            | ⭐⭐⭐⭐⭐ (极难)                          | 大型分布式集群、弹性伸缩           | ❌ 没必要         |
| **Jpom / 宝塔面板**           | ⭐⭐ (较低)               | ⭐ (图形化)                            | 小白运维、不想写脚本               | ❌ 缺乏CI流程控制 |

------

### 📘 第一阶段：自动化部署链路 (CI/CD)

**核心目标**：实现“本地代码提交 -> GitHub 自动打包 -> 发送到服务器 -> 自动重启”。

#### 1. 工作流文件 (`deploy.yml`) 的核心逻辑

我们需要在项目根目录的 `.github/workflows/` 下创建配置文件。这个文件告诉 GitHub 怎么干活。

- **关键动作**：
  1. **Checkout**：拉取代码。
  2. **Set up JDK**：准备 Java 环境。
  3. **Build**：使用 Maven 打包生成 `.jar` 文件。
  4. **SCP (Copy)**：通过 SSH 协议把 `.jar` 文件**从 GitHub 传送到你的服务器**。
  5. **SSH (Script)**：登录你的服务器，执行重启脚本。

#### 2. 遇到的报错与解决方案 (Troubleshooting)

在跑通这个流程时，我们遇到了三个经典问题，请务必记录：

##### 🔴 问题一：`mv` 文件冲突报错

- **现象**：GitHub Actions 日志提示 `mv: target ... is not a directory`。

- **原因**：脚本试图把 `*.jar` 重命名为 `flowblog.jar`，但目录里已经有一个旧的 `flowblog.jar` 了，或者上传了多个 jar 包，导致命令由“重命名”变成了“移动到目录”，引发逻辑错误。

- **✅ 解决方案**：

  **“先毁后建”** —— 在移动新文件前，强制删除旧文件。

  Bash

  ```
  # 在 deploy.yml 的 script 部分：
  rm -f /home/flowblog/blog-backend/flowblog.jar  # 强制删除旧包
  mv /home/flowblog/blog-backend/*.jar /home/flowblog/blog-backend/flowblog.jar
  ```

##### 🔴 问题二：脚本格式错误 (CRLF vs LF)

- **现象**：日志提示 `Syntax error: end of file unexpected (expecting "then")`。

- **原因**：**这是 Windows 用户最容易踩的坑**。

  - Windows 的换行符是 `\r\n` (CRLF)。
  - Linux 的换行符是 `\n` (LF)。
  - 由于我在Win中使用Xftp8 中编辑的脚本内容到 Linux 文件时，多余的 `\r` 字符会让 Linux 的 Shell 解析器“消化不良”。

- **✅ 解决方案**：

  使用 `sed` 命令进行“字符清洗”。(建议作为部署脚本的第一行，以此绝后患)。

  Bash

  ```
  # 修复 restart.sh 的换行符问题
  sed -i 's/\r$//' /home/flowblog/restart.sh
  ```

##### 🔴 问题三：权限被拒绝 (Permission denied)

- **现象**：脚本无法执行，提示 `Permission denied`。

- **原因**：Linux 为了安全，新创建的文件默认是**不可执行**的。

- **✅ 解决方案**：

  赋予脚本执行权限。

  Bash

  ```
  chmod +x /home/flowblog/restart.sh
  ```

#### 3. 最终落地的关键代码备忘

这两段经过无数次调试的代码，它们是这一阶段的结晶。

**📄 1. GitHub Actions 配置 (`.github/workflows/deploy.yml`)**

YAML

```
name: Deploy Flowblog
on:
  push:
    branches:
      - main  # 监听 main 分支的提交

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      # ... (省略 build 步骤，同标准 maven 构建) ...

      # 关键步骤：执行远程脚本
      - name: Execute restart script
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.HOST }}      # 引用 Secrets
          username: ${{ secrets.USERNAME }}
          password: ${{ secrets.PASSWORD }}
          script: |
            # 1. 修复可能存在的换行符问题 (保命神技)
            sed -i 's/\r$//' /home/flowblog/restart.sh
            chmod +x /home/flowblog/restart.sh
            
            # 2. 清理旧包并部署新包
            rm -f /home/flowblog/blog-backend/flowblog.jar
            mv /home/flowblog/blog-backend/*.jar /home/flowblog/blog-backend/flowblog.jar
            
            # 3. 执行重启
            sh /home/flowblog/restart.sh
```

**📄 2. 服务器重启脚本 (`/home/flowblog/restart.sh`)**

*(注：这里包含了后续阶段修正的目录逻辑，作为最终版笔记最合适)*

Bash

```
#!/bin/bash

# 1. 杀掉旧进程
pid=$(ps -ef | grep flowblog.jar | grep -v grep | awk '{print $2}')
if [ -n "$pid" ]; then
  echo "Stopping old application..."
  kill -9 $pid
fi

# 2. 🚀 关键：进入项目目录 (解决 user.dir 路径问题)
# 这一步保证了 application.yml 没写路径时，图片也能存对地方
cd /home/flowblog/blog-backend

# 3. 启动应用
echo "Starting new application..."
# nohup 后台运行，日志输出到 app.log
nohup java -jar flowblog.jar > app.log 2>&1 &

echo "Deployment finished!"
```

------

**💡 第一阶段总结**：

此时，我们已经打通了**“代码 -> 服务器”**的高速公路。只要你在本地 `git push`，3分钟后，服务器上的 jar 包就会自动更新并重启。



------

### 📘 第二阶段：网关与运行环境 (Nginx & Java)

**核心目标**：

1. **搭建运行环境**：让 Java 代码能在服务器上跑起来。
2. **配置反向代理**：让 Nginx 作为“大堂经理”，把用户的请求准确转发给后端的 Java 程序或前端的 Vue 页面。

#### 1. 架构认知的转变 (Docker vs Native)

我在这一步遇到了 **"521 Web server is down"** 的错误。这是因为我从 Docker 切换到了原生部署，产生了一个认知误区：

- **以前 (Docker)**：我们启动的是一个“自带家具的房车”（容器），里面已经预装好了 Java 环境和 Nginx。
- **现在 (Native)**：我们只是把代码搬进了一个“毛坯房”（服务器操作系统）。
- **后果**：代码有了，但是没有 Java 来运行它，也没有 Nginx 来监听 80 端口。

**🛠️ 必装的基础软件：**

Bash

```
# 1. 安装 Java 运行环境 (我的项目是 JDK 17)
apt update
apt install openjdk-17-jdk -y

# 2. 安装 Nginx (Web 服务器/反向代理)
apt install nginx -y
```

#### 2. Nginx 配置的核心逻辑 (`flowblog.conf`)

这是本阶段含金量最高的部分。我们需要在 `/etc/nginx/conf.d/` 下创建一个专属配置文件。

**核心配置代码（最终修正版）：**

Nginx

```
server {
    listen 80;
    server_name flowblog.top; # 我的域名

    # 🟢 1. 前端 Vue 页面托管
    location / {
        root /home/flowblog/blog-frontend; # 前端文件存放路径
        index index.html index.htm;
        # ⚠️ 关键点：解决 Vue History 模式刷新变 404 的问题
        try_files $uri $uri/ /index.html;
    }

    # 🔵 2. 后端接口反向代理
    location /api/ {
        # ⚠️ 关键点：末尾没有斜杠！(详见下方排坑笔记)
        proxy_pass http://127.0.0.1:8080;
        
        # 传递真实 IP 给后端 (否则后端看到的 IP 全是 127.0.0.1)
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

#### 3. 遇到的报错与排坑 (Troubleshooting)

在配置 Nginx 时，我连续踩了两个极其隐蔽的坑，这两个知识点在面试或工作中都非常高频。

##### 🔴 问题一：405 Method Not Allowed (前缀不匹配)

- **现象**：登录时点击按钮，控制台报 `405` 错误（即服务器认为 请求的方式不对）。

- **原因**：

  - 前端代码发出的请求是：`http://flowblog.top/api/login`
  - Nginx 原始配置监听的是：`location /prod-api/`
  - **结果**：Nginx 发现 `/api/` 没在配置里，于是把它当成“找文件”。因为登录是 `POST` 请求，而静态文件不允许 `POST`，所以 Nginx 报了 405。

- **✅ 修复**：

  使用 F12 观察 Network 面板，确认前端真实前缀是 `/api/`，据此修改 Nginx 的 `location`。

##### 🔴 问题二：404 Not Found (Nginx 尾部斜杠魔法)

- **现象**：Nginx 配置改好了，但是接口报 404。
- **原因**：**`proxy_pass` 末尾有没有斜杠，含义天差地别**。
  - **写法 A (错误)**：`proxy_pass http://127.0.0.1:8080/;` (有斜杠)
    - *效果*：Nginx 会**截断**匹配部分。请求 `/api/login` 会变成 `/login` 发给后端。
    - *后端反应*：“我只有 `/api/login` 接口，没有 `/login` 接口”，于是报 404。
  - **写法 B (正确)**：`proxy_pass http://127.0.0.1:8080;` (无斜杠)
    - *效果*：Nginx 会**原样转发**。请求 `/api/login` 依然是 `/api/login` 发给后端。
    - *后端反应*：“收到，处理中！”

------

**💡 第二阶段总结：**

- 访问域名 -> Nginx 接待 -> 转发给 Vue (看页面) 或 Java (调接口)。



------

### 📘 第三阶段：后端与数据库连接 (Backend & DB)

**核心目标**：

1. **打通经脉**：让运行在服务器上的 Java 程序，能够成功连接到运行在 Docker 里的 MySQL 数据库。
2. **修正配置**：把代码仓库里的“旧地址”更新为“新地址”，并成功推送到 GitHub。

#### 1. 架构认知的转变 (容器网络 vs 本机网络)

我遇到了满屏的报错日志（`Connection refused` / `HikariPool Error`），这是因为我的架构发生了微妙的变化：

- **以前 (Docker Compose)**：Java 和 MySQL 是邻居，住在同一个 Docker 网络里。Java 喊一声 "mysql"（容器名），DNS 就能自动解析到数据库的 IP。
  - *旧配置*：`jdbc:mysql://mysql:3306/blog`
- **现在 (Native + Docker)**：Java 搬到了服务器本机运行，而 MySQL 还在 Docker 容器里。对于本机运行的 Java 来说，MySQL 就在“本机”的 3306 端口上（因为我做了端口映射）。
  - *新配置*：`jdbc:mysql://localhost:3306/blog`

#### 2. 遇到的报错与排坑 (Troubleshooting)

##### 🔴 问题一：数据库连接失败 (Connection Refused)

- **现象**：后端启动失败，日志 (`app.log`) 里出现大量 `com.mysql.cj.jdbc.ConnectionImpl.createNewIO` 和 `Communications link failure`。

- **原因**：Java 代码还在傻傻地找一个叫 `mysql` 的服务器，但本机并没有这个 host。

- **✅ 修复步骤**：

  1. **检查端口映射**：确保 Docker 里的 MySQL 把端口暴露出来了。

     Bash

     ```
     docker ps
     # 必须看到 0.0.0.0:3306->3306/tcp 才行
     ```

  2. **修改代码配置**：在本地打开 `src/main/resources/application.yml`。

     YAML

     ```
     spring:
       datasource:
         # 把 mysql 改成 localhost
         url: jdbc:mysql://localhost:3306/my_blog_db?...
         password: "数据库密码" # 确保和服务器上的一致
     ```

##### 🔴 问题二：Git Push 网络报错 (Connection was reset)

- **现象**：修改完配置文件想提交时，终端报错 `fatal: unable to access ... Recv failure: Connection was reset`。

- **原因**：本地网络连接 GitHub 不稳定，或者代理配置（VPN）与 Git 的设置冲突。

- **✅ 修复步骤**（任选其一）：

  - **方法 A（清理旧代理 - 推荐）**：

    Bash

    ```
    git config --global --unset http.proxy
    git config --global --unset https.proxy
    ```

  - **方法 B（设置新代理 - 如果你开了魔法）**：

    - 先看软件设置里的端口（比如 7890）。

    Bash

    ```
    git config --global http.proxy http://127.0.0.1:7890
    git config --global https.proxy http://127.0.0.1:7890
    ```

#### 3. 关键命令备忘 (Log Debugging)

在这一阶段，我们会了如何像医生看X光片一样看日志。这些命令在以后排查任何后端问题时都通用。

Bash

```
# 1. 查看实时滚动日志 (最常用)
# -f (follow) 表示持续监听，Ctrl+C 退出
tail -f /home/flowblog/blog-backend/app.log

# 2. 查看最后 100 行 (用于快速回顾报错堆栈)
tail -n 100 /home/flowblog/blog-backend/app.log

# 3. 配合 grep 搜索关键词 (比如搜 "ERROR" 或 "Exception")
cat /home/flowblog/blog-backend/app.log | grep "ERROR"
```

------

**💡 第三阶段总结：**

至此，我的后端已经完全“活”过来了：它能启动，能连数据库，能处理业务逻辑。登录注册功能也因此恢复正常。

但我马上就发现了新问题：**图片全都裂开了，旧文章里的图片更是显示不出来**。



------

### 📘 第四阶段：静态资源与全站优化 (Resources & Security)

**核心目标**：

1. **资源映射**：让 Nginx 能找到并读取 Java 保存的图片。
2. **权限打通**：解决 Linux 文件系统对不同用户的访问限制。
3. **安全兼容**：在 HTTPS 网站中完美加载旧的 HTTP 图片资源。

#### 1. 架构认知的转变 (文件路径与上下文)

- **以前 (Docker)**：我通常使用“挂载卷” (Volume)，把容器里的 `/app/files` 映射出来。
- **现在 (Native)**：文件直接存在服务器硬盘上。我遇到了两个经典的路径陷阱：
  1. **Nginx 找不到**：URL 是 `/files/xxx.jpg`，但 Nginx 不知道去哪个文件夹找。
  2. **Java 存错地**：Java 代码里用了 `System.getProperty("user.dir")`，导致在哪里运行脚本，文件就存到哪里（之前错存到了 `/root/files`）。

#### 2. 遇到的四大难题与修复方案

##### 🔴 问题一：图片 404 (Nginx 迷路)

- **现象**：访问图片 URL 返回 404 Not Found。

- **原因**：Nginx 默认只管 `/` (前端) 和 `/api/` (后端)，遇到 `/files/` 它就懵了。

- **✅ 修复**：使用 `alias` 指令建立映射。

  Nginx

  ```
  # 在 flowblog.conf 中添加
  location /files/ {
      # ⚠️ 注意：alias 必须以 / 结尾
      alias /home/flowblog/blog-backend/files/;
      expires 7d; # 开启缓存，提升加载速度
  }
  ```

##### 🔴 问题二：图片 403 (被 Linux 拒之门外)

- **现象**：映射配好了，但访问报错 `403 Forbidden`。

- **原因**：**权限隔离**。

  - 我的文件夹 `/home/flowblog` 是 `root` 用户创建的。
  - Nginx 运行用户通常是 `www-data` 或 `nginx`（普通平民）。
  - 普通用户默认没有权限进入 `root` 的地盘。

- **✅ 修复**：赋予读取和执行权限。

  Bash

  ```
  # 755 权限：所有者(rwx) / 组(rx) / 其他人(rx)
  # 关键是这个 x (execute)，对文件夹来说意味着“允许进入”
  chmod -R 755 /home/flowblog
  ```

##### 🔴 问题三：Java 存错位置 (启动上下文)

- **现象**：新上传的图片 Nginx 找不到，去服务器一搜发现跑到了 `/root/files`。

- **原因**：Java 代码逻辑依赖“当前运行目录”。如果直接在 `~` 目录下执行启动命令，Java 就把家安在了 `~`。

- **✅ 修复**：修改 `restart.sh`，启动前先“回家”。

  Bash

  ```
  # 强制进入项目目录，固定 user.dir 变量
  cd /home/flowblog/blog-backend
  nohup java -jar flowblog.jar ...
  ```

##### 🔴 问题四：旧图裂开 (混合内容 Mixed Content)

- **现象**：旧文章里的图片不显示，F12 控制台没有任何 404 报错，图片请求被浏览器静默拦截。

- **原因**：

  - **网站**：是安全的 HTTPS (`https://flowblog.top`)。
  - **旧图片链接**：是不安全的 HTTP (`http://flowblog.top/...`)。
  - **浏览器策略**：HTTPS 页面严禁加载 HTTP 资源（怕被篡改）。

- **✅ 修复**：给 Nginx 加“魔法头”，告诉浏览器自动升级链接。

  Nginx

  ```
  # 在 server 块中添加
  add_header Content-Security-Policy "upgrade-insecure-requests";
  ```

#### 3. 终极排错技巧：如何看破“缓存的谎言”

在修复过程中，我一度陷入困境：服务器修好了，但浏览器还是显示 404。这是因为 **Cloudflare (CDN)** 极其敬业地缓存了之前的“错误页面”。

**🕵️‍♂️ 验尸流程三部曲：**

1. **查文件 (`ls -lh`)**：
   - 确认文件真的在硬盘上，不是 0 字节，权限是可读的 (r)。
2. **问服务器 (`curl -I`)**：
   - `curl -I http://127.0.0.1/files/xxx.jpg`
   - 如果不经过 CDN 和浏览器，直接问 Nginx。如果它回 `200 OK`，说明**后端/服务器绝对没问题**。
3. **清缓存 (Cloudflare)**：
   - 如果步骤 2 成功，但浏览器不行，直接去 CDN 控制台 **"Purge Everything"**。
   - **小技巧**：在 URL 后面加参数 `?v=1` (如 `xxx.jpg?v=1`) 可以强制绕过缓存验证。

------

### 🎉 全剧终

通过这四个阶段的实战，我现在拥有的不仅仅是一个修好的博客，而是一套完整的**全栈运维知识体系**：

1. **CI/CD**：掌握了 GitHub Actions 自动化部署脚本的编写。
2. **Linux 运维**：熟悉了 `grep`、`tail`、`chmod`、`curl` 等核心排错命令。
3. **Nginx 专家**：理解了反向代理、路径别名 (`alias`)、HTTPS 头配置。
4. **架构思维**：深刻理解了 Docker 容器化与 Native 原生部署的区别与优劣。

