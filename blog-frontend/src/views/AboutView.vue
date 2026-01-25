<template>
  <div class="about-container">
    <!-- 顶部导航区 -->
    <div class="nav-header">
      <div class="logo" @click="$router.push('/')">Flow<span>.</span></div>
      <el-button round @click="$router.push('/')">返回首页</el-button>
    </div>

    <!--主要内容卡片 -->
    <div class="content-wrapper">
      <div class="profile-section">
        <div class="avatar-box">
            <!-- 这里使用你的头像，如果没有登录则显示默认 -->
            <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" alt="Avatar">
        </div>
        <h1>关于 Flow</h1>
        <p class="subtitle">Code like water, live with rhythm.</p>
      </div>

      <div class="text-content">
        <h3>🌱 关于这个数字花园</h3>
        <p>
          你好，欢迎来到 <strong>Flow</strong>。这是一个由 Java + Vue 驱动的个人博客系统。
          在这里，我记录技术折腾的笔记，分享生活中的吉光片羽，也整理在这个信息爆炸时代的深度思考。
        </p>
        <p>
          “Flow” 意为“心流”，也代表代码逻辑的流动。我希望通过这个平台，连接更多有趣的灵魂。
        </p>

        <h3>🛠️ 技术栈</h3>
        <div class="tech-tags">
          <el-tag effect="dark" type="primary">Spring Boot 3</el-tag>
          <el-tag effect="dark" type="success">Vue 3</el-tag>
          <el-tag effect="dark" type="warning">MySQL 8</el-tag>
          <el-tag effect="plain">MyBatis Plus</el-tag>
          <el-tag effect="plain">Element Plus</el-tag>
          <el-tag effect="plain">Docker</el-tag>
        </div>

        <h3>The End</h3>
        <p>
          本站除特别声明外，所有文章均为原创。
          <br>如需转载请联系博主或注明出处。
        </p>
      </div>
      
      <!-- 社交链接 -->
      <div class="social-links">
        <el-button circle><i class="fab fa-github"></i></el-button>
        <el-button circle><i class="fab fa-twitter"></i></el-button>
        <el-button circle><i class="fas fa-envelope"></i></el-button>
      </div>
    </div>

    <!-- 底部版权 -->
    <footer>
        &copy; 2025 Flow. Designed with Code & Poetry.
    </footer>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.about-container {
  min-height: 100vh;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
}

.nav-header {
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  font-size: 1.5rem;
  font-weight: 800;
  cursor: pointer;
  color: #333;
}
.logo span { color: #409EFF; }

.content-wrapper {
  max-width: 800px;
  margin: 40px auto;
  background: #fff;
  padding: 60px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
  flex: 1; /* 让内容撑开 */
}

.profile-section {
  text-align: center;
  margin-bottom: 40px;
}

.avatar-box img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 4px solid #f0f2f5;
  margin-bottom: 20px;
}

h1 { font-size: 2rem; margin: 0 0 10px; color: #2c3e50; }
.subtitle { color: #999; font-style: italic; font-family: 'Georgia', serif; }

.text-content {
  color: #555;
  line-height: 1.8;
  font-size: 1.05rem;
}

.text-content h3 {
  margin-top: 30px;
  margin-bottom: 15px;
  color: #333;
  border-left: 4px solid #409EFF;
  padding-left: 12px;
}

.tech-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 30px;
}

.social-links {
  margin-top: 50px;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 20px;
}

footer {
  text-align: center;
  padding: 30px;
  color: #ccc;
  font-size: 12px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .content-wrapper {
    margin: 20px;
    padding: 30px;
  }
}
</style>