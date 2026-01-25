<template>
  <div class="friends-container">
    <div class="header">
      <h2><el-icon><Link /></el-icon> 友情链接</h2>
      <el-button @click="$router.push('/')" round>返回首页</el-button>
    </div>

    <div class="friends-grid">
      <div v-for="link in links" :key="link.name" class="friend-card" @click="openLink(link.url)">
        <el-avatar :size="60" :src="link.avatar" class="friend-avatar">
            {{ link.name.substring(0,1) }}
        </el-avatar>
        <div class="friend-info">
          <h3>{{ link.name }}</h3>
          <p>{{ link.desc }}</p>
        </div>
      </div>
    </div>
    
    <div class="apply-tip">
      <p>想交换友链？请在下方留言告诉我你的博客地址。</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Link } from '@element-plus/icons-vue'

// 这里先用静态数据演示，后续可改为从后端 API 获取
const links = ref([
  { 
    name: 'Vue.js', 
    desc: '渐进式 JavaScript 框架', 
    url: 'https://vuejs.org', 
    avatar: 'https://vuejs.org/images/logo.png' 
  },
  { 
    name: 'Element Plus', 
    desc: '基于 Vue 3 的组件库', 
    url: 'https://element-plus.org', 
    avatar: 'https://element-plus.org/images/element-plus-logo.svg' 
  },
  { 
    name: 'Spring Boot', 
    desc: 'Java 后端开发利器', 
    url: 'https://spring.io/projects/spring-boot', 
    avatar: 'https://spring.io/images/spring-logo-9146a4d3298fc606328b95803203d064.svg' 
  }
])

const openLink = (url) => {
  window.open(url, '_blank')
}
</script>

<style scoped>
.friends-container {
  max-width: 900px;
  margin: 40px auto;
  padding: 20px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40px;
  border-bottom: 1px solid #eee;
  padding-bottom: 20px;
}
.header h2 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.8rem;
  color: #333;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.friend-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
  cursor: pointer;
  transition: all 0.3s;
}
.friend-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
  border-color: #409EFF;
}

.friend-avatar {
  background: #f0f2f5;
  flex-shrink: 0;
}

.friend-info h3 {
  margin: 0 0 5px;
  font-size: 1.1rem;
  color: #333;
}
.friend-info p {
  margin: 0;
  font-size: 0.85rem;
  color: #999;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.apply-tip {
  margin-top: 50px;
  text-align: center;
  color: #999;
  font-size: 0.9rem;
}
</style>