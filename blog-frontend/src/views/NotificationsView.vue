<template>
  <div class="notify-container">
    <div class="header">
      <h2>消息中心</h2>
      <div class="actions">
        <!-- 批量操作按钮 -->
        <template v-if="selectedIds.length > 0">
            <el-button type="primary" plain size="small" @click="batchRead">标记已读</el-button>
            <el-button type="danger" plain size="small" @click="batchDelete">删除选中</el-button>
            <el-divider direction="vertical" />
        </template>
        <el-button v-if="notifications.length > 0" type="primary" link @click="markAllRead">全部已读</el-button>
      </div>
    </div>

    <div v-loading="loading" class="notify-list">
      <div v-for="item in notifications" :key="item.id" class="notify-item-wrapper" :class="{ 'unread': item.status === 0 }">
        <!-- 多选框 -->
        <div class="checkbox-area">
            <el-checkbox v-model="item.selected" @change="handleSelectionChange" />
        </div>
        
        <!-- 内容区域 -->
        <div class="notify-content" @click="goToPost(item)">
            <div class="icon-box" :class="item.type === 2 ? 'audit' : ''">
              <el-icon><component :is="item.type === 2 ? 'Warning' : 'ChatDotRound'" /></el-icon>
            </div>
            <div class="content">
              <p class="title">
                <span class="name">{{ item.senderName }}</span> 
                {{ formatDate(item.createTime) }}
              </p>
              <p class="desc">{{ item.content }}</p>
            </div>
            <div class="status-dot" v-if="item.status === 0"></div>
        </div>
      </div>
      
      <el-empty v-if="!loading && notifications.length === 0" description="暂无消息" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ChatDotRound, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(true)
const notifications = ref([])

// 计算选中的ID
const selectedIds = computed(() => notifications.value.filter(n => n.selected).map(n => n.id))

const loadData = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) return
    const res = await axios.get('/api/notifications', { headers: { Authorization: token } })
    if (res.data.code === 200) {
      // 增加 selected 字段用于前端状态
      notifications.value = res.data.data.map(item => ({ ...item, selected: false }))
    }
  } finally {
    loading.value = false
  }
}

// 批量标记已读
const batchRead = async () => {
    const token = localStorage.getItem('token')
    // 简单起见，循环调用或后端加批量已读接口。这里演示循环调用前端更新
    // 建议后端加 /api/notifications/read-batch
    for(const id of selectedIds.value) {
        await axios.put(`/api/notifications/${id}/read`, {}, { headers: { Authorization: token } })
    }
    notifications.value.forEach(n => {
        if(n.selected) {
            n.status = 1
            n.selected = false
        }
    })
    ElMessage.success('已标记')
}

// 批量删除
const batchDelete = async () => {
    ElMessageBox.confirm(`确定删除这 ${selectedIds.value.length} 条消息吗？`, '提示', { type: 'warning' }).then(async () => {
        const token = localStorage.getItem('token')
        const res = await axios.delete('/api/notifications', { 
            data: selectedIds.value, // 传参 Body
            headers: { Authorization: token } 
        })
        if (res.data.code === 200) {
            ElMessage.success('删除成功')
            loadData() // 刷新列表
        }
    })
}

const markAllRead = async () => {
  const token = localStorage.getItem('token')
  await axios.put('/api/notifications/read-all', {}, { headers: { Authorization: token } })
  notifications.value.forEach(n => n.status = 1)
  ElMessage.success('已全部标记为已读')
}

const goToPost = (item) => {
  // 点击后也可以顺便标已读
  if (item.type === 2) {
      router.push('/dashboard')
  } else {
      router.push(`/post/${item.postId}`)
  }
}

const handleSelectionChange = () => {
    // 触发 computed 更新
}

const formatDate = (iso) => new Date(iso).toLocaleString()

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.notify-container { max-width: 800px; margin: 40px auto; padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.notify-list { background: #fff; border-radius: 8px; border: 1px solid #eee; overflow: hidden; }

/* 列表项改为 flex 布局以容纳复选框 */
.notify-item-wrapper { display: flex; border-bottom: 1px solid #f5f5f5; transition: background 0.2s; }
.notify-item-wrapper:hover { background: #fafafa; }
.notify-item-wrapper.unread { background: #f0f9eb; }
.notify-item-wrapper:last-child { border-bottom: none; }

.checkbox-area { padding: 0 10px; display: flex; align-items: center; justify-content: center; }
.notify-content { flex: 1; display: flex; padding: 20px 20px 20px 0; cursor: pointer; position: relative; }

.icon-box { margin-right: 15px; display: flex; align-items: center; color: #409EFF; font-size: 20px; }
.icon-box.audit { color: #E6A23C; }
.content { flex: 1; }
.title { font-size: 13px; color: #999; margin: 0 0 5px; }
.name { color: #333; font-weight: bold; margin-right: 10px; }
.desc { margin: 0; color: #555; font-size: 15px; }
.status-dot { position: absolute; right: 20px; top: 50%; transform: translateY(-50%); width: 8px; height: 8px; background: #f56c6c; border-radius: 50%; }
</style>