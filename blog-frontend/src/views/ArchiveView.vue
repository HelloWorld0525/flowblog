<template>
  <div class="archive-container" v-loading="loading">
    <div class="header">
      <h2><el-icon><Calendar /></el-icon> 归档时光</h2>
      <el-button @click="$router.push('/')" round>返回首页</el-button>
    </div>

    <div class="timeline-wrapper">
      <el-timeline>
        <el-timeline-item
          v-for="(post, index) in posts"
          :key="post.id"
          :timestamp="formatDate(post.createTime)"
          placement="top"
          :type="index === 0 ? 'primary' : ''"
          :hollow="index === 0"
        >
          <el-card class="timeline-card" shadow="hover" @click="goToDetail(post.id)">
            <div class="card-content">
              <h4>{{ post.title }}</h4>
              <el-tag size="small" effect="plain">{{ post.category }}</el-tag>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      
      <el-empty v-if="!loading && posts.length === 0" description="暂无文章" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { Calendar } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(true)
const posts = ref([])

const loadPosts = async () => {
  try {
    // [修复] 增加分页参数，size 设大一点以显示更多归档
    const res = await axios.get('/api/posts', {
        params: {
            page: 1,
            size: 100 // 归档页通常展示较多数据
        }
    })
    
    if (res.data.code === 200) {
      // [修复] 从 records 中获取数组，而不是直接取 res.data.data
      posts.value = res.data.data.records || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id) => {
    if (id) {
        router.push(`/post/${id}`)
    } else {
        ElMessage.warning('文章链接无效')
    }
}

const formatDate = (isoStr) => {
  if (!isoStr) return ''
  return new Date(isoStr).toLocaleDateString()
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped>
.archive-container {
  max-width: 800px;
  margin: 40px auto;
  padding: 20px;
  min-height: 80vh; /* 保证页面高度 */
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

.timeline-wrapper {
  padding: 0 20px;
}

.timeline-card {
  cursor: pointer;
  transition: transform 0.2s;
  border-radius: 8px;
}
.timeline-card:hover {
  transform: translateX(5px);
  border-color: #409EFF;
}

.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-content h4 {
  margin: 0;
  font-size: 1.1rem;
  color: #303133;
  font-weight: 500;
}
</style>