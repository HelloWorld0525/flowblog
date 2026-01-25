<template>
  <div class="article-container" v-loading="loading">
    
    <!-- 1. 文章头部 -->
    <header class="article-header" v-if="post && post.title">
      <div class="title-row">
        <h1 class="title">{{ post.title }}</h1>
        <el-button v-if="isAdmin" type="primary" icon="Edit" circle plain @click="goToEdit" title="编辑文章" />
      </div>

      <div class="meta">
        <el-tag size="small" effect="plain" class="category-tag">{{ post.category }}</el-tag>
        <span class="divider">/</span>
        <span class="time">{{ formatDate(post.createTime) }}</span>
        <span class="divider">/</span>
        <span class="views">{{ post.views }} 阅读</span>
      </div>
      
      <div class="cover-box" v-if="post.coverImage">
          <img :src="post.coverImage" class="cover-image" />
      </div>
    </header>

    <!-- 2. 文章正文 (应用 github-markdown-css) -->
    <article class="markdown-body" v-if="post && post.content" v-html="renderedContent"></article>

    <!-- 404 状态 -->
    <div v-if="!loading && !post.title" class="not-found">
        <el-empty description="文章不存在或已被删除">
            <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </el-empty>
    </div>

    <el-divider v-if="post.title" />

    <!-- 3. 评论区 -->
    <div class="comment-section" v-if="post.title">
      <h3 class="section-title">
        评论 <span class="count">({{ totalCount }})</span>
      </h3>

      <div class="comment-form-card">
        <div class="user-info-row" v-if="!isLoggedIn">
          <el-input v-model="commentForm.nickname" placeholder="昵称 (必填)" style="width: 200px">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
          <span class="guest-tip">游客留言需审核</span>
        </div>
        <div class="user-info-row logged-in" v-else>
          <el-avatar :size="32" :src="currentUserAvatar">{{ currentUserName.charAt(0).toUpperCase() }}</el-avatar>
          <span class="username">{{ currentUserName }}</span>
        </div>

        <div v-if="replyTarget" class="reply-bar">
          <el-tag closable type="info" @close="cancelReply">
            回复 @{{ replyTarget.nickname }}
          </el-tag>
        </div>

        <el-input 
          ref="commentInputRef"
          v-model="commentForm.content" 
          type="textarea" 
          :rows="3" 
          :placeholder="replyTarget ? `回复 @${replyTarget.nickname}...` : '写下你的想法...'" 
          maxlength="500" 
          show-word-limit 
          class="content-input" 
        />
        
        <div class="form-footer">
          <el-button type="primary" :loading="submitting" @click="submitComment" round>
            {{ replyTarget ? '发表回复' : (isLoggedIn ? '发表评论' : '提交审核') }}
          </el-button>
        </div>
      </div>

      <div class="comment-list">
        <div v-for="rootComment in commentTree" :key="rootComment.id" class="root-comment-item">
          
          <div class="comment-main">
            <el-avatar class="avatar" :size="40" :src="rootComment.userAvatar">
               {{ rootComment.nickname ? rootComment.nickname.substring(0,1).toUpperCase() : 'G' }}
            </el-avatar>
            
            <div class="comment-content">
              <div class="comment-meta">
                <span class="nickname" :class="{ 'admin-name': rootComment.isAdmin }">
                  {{ rootComment.nickname }}
                  <el-tag v-if="rootComment.isAdmin" size="small" type="danger" effect="plain" round style="margin-left:5px; height: 18px; line-height: 16px;">博主</el-tag>
                </span>
                <span class="time">{{ formatDate(rootComment.createTime) }}</span>
              </div>
              <p class="text">{{ rootComment.content }}</p>
              
              <div class="actions">
                <el-button type="primary" link size="small" @click="handleReply(rootComment)">回复</el-button>
                <el-button v-if="isAdmin" type="danger" link size="small" @click="handleDeleteComment(rootComment.id)">删除</el-button>
              </div>
            </div>
          </div>

          <div v-if="rootComment.children && rootComment.children.length > 0" class="sub-comments">
            <div 
              v-for="child in (expandedSet.has(rootComment.id) ? rootComment.children : rootComment.children.slice(0, 2))" 
              :key="child.id" 
              class="sub-comment-item"
            >
              <el-avatar class="avatar-small" :size="24" :src="child.userAvatar">
                 {{ child.nickname ? child.nickname.substring(0,1).toUpperCase() : 'G' }}
              </el-avatar>
              
              <div class="comment-content">
                <div class="comment-meta">
                  <span class="nickname-small" :class="{ 'admin-name': child.isAdmin }">
                    {{ child.nickname }}
                    <span v-if="child.replyToUser" class="reply-text">
                        回复 <span class="at-name">@{{ child.replyToUser }}</span>
                    </span>
                  </span>
                  <span class="time">{{ formatDate(child.createTime) }}</span>
                </div>
                <p class="text">{{ child.content }}</p>
                <div class="actions">
                  <el-button type="primary" link size="small" @click="handleReply(child)">回复</el-button>
                  <el-button v-if="isAdmin" type="danger" link size="small" @click="handleDeleteComment(child.id)">删除</el-button>
                </div>
              </div>
            </div>

            <div v-if="rootComment.children.length > 2" class="expand-control">
              <el-button v-if="!expandedSet.has(rootComment.id)" type="info" link size="small" @click="toggleExpand(rootComment.id)">
                展开剩余 {{ rootComment.children.length - 2 }} 条回复 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <el-button v-else type="info" link size="small" @click="toggleExpand(rootComment.id)">
                收起回复 <el-icon><ArrowUp /></el-icon>
              </el-button>
            </div>
          </div>

        </div>
        
        <el-empty v-if="commentTree.length === 0" description="暂无评论，来坐沙发" :image-size="100" />
      </div>
    </div>

    <div class="footer-nav">
      <el-button @click="$router.push('/')" round>返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { marked } from 'marked'
import { User, Edit, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 1. 引入样式库
import 'github-markdown-css/github-markdown-light.css' // GitHub 风格
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css' // 代码高亮样式 (可换 atom-one-dark 等)

const route = useRoute()
const router = useRouter()
const post = ref({})
const rawComments = ref([]) 
const loading = ref(true)
const submitting = ref(false)
const isLoggedIn = ref(false)
const isAdmin = ref(false)
const commentInputRef = ref(null)

const currentUserAvatar = ref('')
const currentUserName = ref('User')

const commentForm = reactive({ nickname: '', content: '', parentId: null })
const replyTarget = ref(null)
const expandedSet = ref(new Set())

// 2. 配置 marked 支持高亮
const renderer = new marked.Renderer()
marked.setOptions({
  renderer: renderer,
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  langPrefix: 'hljs language-', 
  gfm: true,
  breaks: true
})

const renderedContent = computed(() => {
  if (!post.value || !post.value.content) return ''
  try { return marked.parse(post.value.content) } catch (e) { return post.value.content }
})

const commentTree = computed(() => {
  const map = {}
  const roots = []
  const comments = JSON.parse(JSON.stringify(rawComments.value))
  comments.forEach(c => {
    c.children = []
    map[c.id] = c
  })
  comments.forEach(c => {
    if (c.parentId && map[c.parentId]) {
      let root = findRoot(c, map)
      if (root) {
        c.replyToUser = map[c.parentId].nickname 
        if (root.id !== c.id) {
            root.children.push(c)
        }
      }
    } else {
      roots.push(c)
    }
  })
  roots.forEach(r => {
      r.children.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
  })
  return roots
})

const findRoot = (comment, map) => {
    let current = comment
    while(current.parentId && map[current.parentId]) {
        current = map[current.parentId]
    }
    return current
}

const toggleExpand = (id) => {
    if (expandedSet.value.has(id)) {
        expandedSet.value.delete(id)
    } else {
        expandedSet.value.add(id)
    }
}

const totalCount = computed(() => rawComments.value.length)

const initData = async () => {
  loading.value = true
  const id = route.params.id
  checkLogin()
  try {
    const [postRes, commentRes] = await Promise.all([
      axios.get(`/api/posts/${id}`),
      axios.get(`/api/comments?postId=${id}`)
    ])
    
    if (postRes.data.code === 200) {
        if (!postRes.data.data) {
            ElMessage.error('文章不存在或已被删除')
            setTimeout(() => router.push('/'), 1500)
            return
        }
        post.value = postRes.data.data
    } else {
        ElMessage.error('无法获取文章信息')
        setTimeout(() => router.push('/'), 1500)
        return
    }

    if (commentRes.data.code === 200) rawComments.value = commentRes.data.data
  } catch (error) {
    console.error(error)
    ElMessage.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const checkLogin = () => {
  const token = localStorage.getItem('token')
  if (token) {
    isLoggedIn.value = true
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
      const payload = JSON.parse(decodeURIComponent(window.atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')))
      isAdmin.value = payload.role === 1
    } catch (e) {}
    getUserInfo(token)
  }
}

const getUserInfo = async (token) => {
    try {
        const res = await axios.get('/api/user/me', { headers: { Authorization: token } })
        if (res.data.code === 200) {
            const u = res.data.data
            currentUserName.value = u.nickname || u.username
            currentUserAvatar.value = u.avatar
        }
    } catch(e) {}
}

const goToEdit = () => router.push(`/editor?id=${post.value.id}`)

const handleReply = (item) => {
  replyTarget.value = item
  commentForm.parentId = item.id
  document.querySelector('.comment-form-card').scrollIntoView({ behavior: 'smooth', block: 'center' })
  nextTick(() => commentInputRef.value.focus())
}

const handleDeleteComment = (id) => {
    ElMessageBox.confirm('确定要删除这条评论吗？', '警告', { type: 'warning' }).then(async () => {
        try {
            const token = localStorage.getItem('token')
            const res = await axios.delete(`/api/comments/${id}`, { headers: { Authorization: token } })
            if (res.data.code === 200) {
                ElMessage.success('删除成功')
                rawComments.value = rawComments.value.filter(c => c.id !== id)
            } else {
                ElMessage.error(res.data.msg)
            }
        } catch(e) {
            ElMessage.error('操作失败')
        }
    })
}

const cancelReply = () => {
  replyTarget.value = null
  commentForm.parentId = null
}

const submitComment = async () => {
  if (!commentForm.content.trim()) return ElMessage.warning('请输入评论内容')
  if (!isLoggedIn.value && !commentForm.nickname.trim()) return ElMessage.warning('游客请填写昵称')

  submitting.value = true
  try {
    const payload = {
      postId: post.value.id,
      content: commentForm.content,
      nickname: isLoggedIn.value ? currentUserName.value : commentForm.nickname,
      parentId: commentForm.parentId
    }
    const headers = {}
    const token = localStorage.getItem('token')
    if (token) headers['Authorization'] = token

    const res = await axios.post('/api/comments', payload, { headers })
    if (res.data.code === 200) {
      ElMessage.success(res.data.data)
      if (isLoggedIn.value) {
        const newItem = {
          id: Date.now(), 
          nickname: payload.nickname, 
          content: payload.content, 
          createTime: new Date().toISOString(), 
          isAdmin: isAdmin.value ? 1 : 0,
          parentId: payload.parentId,
          userAvatar: currentUserAvatar.value 
        }
        rawComments.value.unshift(newItem)
      }
      commentForm.content = ''
      cancelReply() 
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    submitting.value = false
  }
}

const formatDate = (iso) => iso ? new Date(iso).toLocaleString() : ''

onMounted(() => {
  initData()
})
</script>

<style scoped>
/* 容器美化 */
.article-container { 
  max-width: 800px; 
  margin: 40px auto; 
  padding: 40px; 
  min-height: 80vh; 
  background: #fff; /* 增加背景色 */
  border-radius: 12px; /* 圆角 */
  box-shadow: 0 4px 20px rgba(0,0,0,0.05); /* 阴影 */
}

.article-header { text-align: center; margin-bottom: 40px; }
.title-row { display: flex; align-items: center; justify-content: center; gap: 15px; margin-bottom: 20px; }
.title { font-size: 2.5rem; font-weight: 800; color: #1a1a1a; margin: 0; line-height: 1.2; font-family: 'Georgia', serif; }

.meta { color: #666; font-size: 0.9rem; margin-bottom: 30px; display: flex; justify-content: center; align-items: center; gap: 10px; }
.divider { color: #ddd; }
.category-tag { font-weight: 600; }

.cover-box { margin-top: 20px; border-radius: 12px; overflow: hidden; box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.cover-image { width: 100%; max-height: 400px; object-fit: cover; display: block; }

/* Markdown 样式容器：注意这里不需要太多自定义 CSS，github-markdown-css 会接管 */
.markdown-body { 
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif; 
  margin-bottom: 60px;
  background: transparent; /* 防止背景冲突 */
}

/* 评论区样式保持不变 */
.comment-section { margin-top: 40px; }
.section-title { font-size: 1.5rem; margin-bottom: 20px; font-weight: 700; border-left: 5px solid #409EFF; padding-left: 15px; }
.count { font-size: 1rem; color: #999; font-weight: normal; }
.comment-form-card { background: #f9fafb; padding: 20px; border-radius: 12px; margin-bottom: 40px; border: 1px solid #eee; }
.user-info-row { display: flex; align-items: center; margin-bottom: 15px; gap: 10px; }
.guest-tip { font-size: 12px; color: #909399; margin-left: 10px; }
.logged-in .username { font-weight: 600; font-size: 14px; color: #333; }
.reply-bar { margin-bottom: 10px; }
.content-input { margin-bottom: 15px; }
.form-footer { text-align: right; }
.comment-list { margin-top: 20px; }
.root-comment-item { margin-bottom: 30px; border-bottom: 1px solid #f0f0f0; padding-bottom: 20px; }
.root-comment-item:last-child { border-bottom: none; }
.comment-main { display: flex; gap: 15px; }
.comment-content { flex: 1; }
.comment-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.nickname { font-weight: 600; color: #333; font-size: 14px; display: flex; align-items: center; }
.nickname-small { font-weight: 600; color: #666; font-size: 13px; }
.admin-name { color: #f56c6c; }
.time { font-size: 12px; color: #999; }
.text { font-size: 14px; color: #555; line-height: 1.6; margin-bottom: 5px; white-space: pre-wrap; }
.reply-text { font-weight: normal; color: #999; margin-left: 5px; font-size: 12px; }
.at-name { color: #409EFF; }
.sub-comments { margin-top: 15px; margin-left: 55px; background: #f9f9f9; padding: 15px; border-radius: 8px; }
.sub-comment-item { display: flex; gap: 10px; margin-bottom: 15px; }
.sub-comment-item:last-child { margin-bottom: 0; }
.avatar-small { flex-shrink: 0; }
.expand-control { margin-top: 10px; font-size: 12px; }
.footer-nav { margin-top: 60px; padding-top: 40px; border-top: 1px solid #eee; text-align: center; padding-bottom: 40px; }
.not-found { padding: 50px 0; text-align: center; }
.actions { margin-top: 5px; }

/* 移动端适配 */
@media (max-width: 768px) {
    .article-container { padding: 20px; margin: 0; border-radius: 0; box-shadow: none; }
    .title { font-size: 1.8rem; }
}
</style>