<template>
  <div class="dashboard-container">
    <div class="header">
      <h2>管理控制台</h2>
      <div class="header-actions">
          <el-button type="warning" plain @click="broadcastVisible = true">发布公告</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
      </div>
    </div>

    <el-tabs type="border-card" class="dashboard-tabs" v-model="activeTab" @tab-change="handleTabChange">
      
      <!-- 1. 文章管理 -->
      <el-tab-pane label="文章管理" name="posts">
        <el-table :data="posts" style="width: 100%" stripe v-loading="loading">
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="scope"><el-tag>{{ scope.row.category }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="views" label="阅读" width="80" align="center" />
          <el-table-column prop="createTime" label="发布时间" width="180">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button size="small" type="primary" plain @click="editPost(scope.row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="deletePost(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-box">
            <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="currentPage" @current-change="handlePageChange" />
        </div>
      </el-tab-pane>

      <!-- 2. 评论审核 -->
      <el-tab-pane label="评论审核" name="comments">
        <el-table :data="comments" style="width: 100%" stripe>
          <el-table-column prop="nickname" label="用户" width="150" />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
              <el-tag v-else type="success">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="scope">
              <el-button v-if="scope.row.status === 0" size="small" type="success" @click="passComment(scope.row.id)">通过</el-button>
              <el-button size="small" type="danger" @click="deleteComment(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 3. [新增] 公告管理 -->
      <el-tab-pane label="公告管理" name="announcements">
        <el-table :data="announcements" style="width: 100%" stripe>
          <el-table-column prop="content" label="公告内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="createTime" label="发布时间" width="180">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button size="small" type="danger" plain @click="deleteAnnouncement(scope.row.id)">撤回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 公告发布弹窗 -->
    <el-dialog v-model="broadcastVisible" title="发布全站公告" width="500px">
        <el-input v-model="broadcastContent" type="textarea" :rows="4" placeholder="请输入公告内容，所有访问者可见..." />
        <template #footer>
            <el-button @click="broadcastVisible = false">取消</el-button>
            <el-button type="primary" @click="sendBroadcast">发布</el-button>
        </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('posts')
const posts = ref([])
const comments = ref([])
const announcements = ref([]) // [新增]
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const broadcastVisible = ref(false)
const broadcastContent = ref('')

const getToken = () => localStorage.getItem('token')

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/posts', { params: { page: currentPage.value, size: pageSize.value } })
    if (res.data.code === 200) {
      posts.value = res.data.data.records
      total.value = res.data.data.total
    }
  } catch(e) {} finally { loading.value = false }
}

const handlePageChange = (page) => { currentPage.value = page; loadPosts() }

const loadComments = async () => {
  try {
    const res = await axios.get('/api/comments/admin', { headers: { 'Authorization': getToken() } })
    if (res.data.code === 200) comments.value = res.data.data
  } catch (e) {}
}

// [新增] 加载公告
const loadAnnouncements = async () => {
  try {
    const res = await axios.get('/api/notifications/admin/announcements', { headers: { 'Authorization': getToken() } })
    if (res.data.code === 200) announcements.value = res.data.data
  } catch (e) {}
}

const handleTabChange = (tabName) => {
    if (tabName === 'posts') loadPosts()
    else if (tabName === 'comments') loadComments()
    else if (tabName === 'announcements') loadAnnouncements()
}

// 增删改查逻辑
const deletePost = (id) => { ElMessageBox.confirm('确定删除?', '警告', { type: 'warning' }).then(async () => { await axios.delete(`/api/posts/${id}`, { headers: { 'Authorization': getToken() } }); ElMessage.success('删除成功'); loadPosts() }) }
const editPost = (post) => { router.push(`/editor?id=${post.id}`) }
const passComment = async (id) => { await axios.put(`/api/comments/${id}/pass`, {}, { headers: { 'Authorization': getToken() } }); ElMessage.success('审核通过'); loadComments() }
const deleteComment = async (id) => { ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' }).then(async () => { await axios.delete(`/api/comments/${id}`, { headers: { 'Authorization': getToken() } }); ElMessage.success('删除成功'); loadComments() }) }

// [新增] 删除公告
const deleteAnnouncement = (id) => {
    ElMessageBox.confirm('确定撤回这条公告吗?', '提示', { type: 'warning' }).then(async () => {
        try {
            await axios.delete(`/api/notifications/${id}`, { headers: { 'Authorization': getToken() } })
            ElMessage.success('撤回成功')
            loadAnnouncements()
        } catch(e) { ElMessage.error('操作失败') }
    })
}

const sendBroadcast = async () => {
    if(!broadcastContent.value) return ElMessage.warning('内容不能为空')
    try {
        const res = await axios.post('/api/notifications/broadcast', { content: broadcastContent.value }, { headers: { 'Authorization': getToken() } })
        if (res.data.code === 200) {
            ElMessage.success('发布成功')
            broadcastVisible.value = false
            broadcastContent.value = ''
            if (activeTab.value === 'announcements') loadAnnouncements()
        } else { ElMessage.error(res.data.msg) }
    } catch(e) { ElMessage.error('发布失败') }
}

const formatDate = (isoStr) => { return isoStr ? new Date(isoStr).toLocaleString() : '' }

onMounted(() => {
  if (!getToken()) {
    ElMessage.error('请先登录')
    router.push('/')
    return
  }
  loadPosts()
  loadComments()
  // 预加载一次公告，或者等切换 tab 再加载
  // loadAnnouncements() 
})
</script>

<style scoped>
.dashboard-container { max-width: 1000px; margin: 40px auto; padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-actions { display: flex; gap: 10px; }
.pagination-box { margin-top: 20px; display: flex; justify-content: center; }
</style>