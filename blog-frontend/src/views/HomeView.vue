<template>
  <div class="common-layout">
    <!-- 1. 顶部导航 -->
    <header class="sticky-header">
      <div class="header-content">
        <!-- [新增] 移动端汉堡菜单按钮 -->
        <div class="mobile-menu-btn" @click="mobileMenuVisible = true">
            <el-icon :size="24"><Menu /></el-icon>
        </div>

        <div class="logo" @click="resetFilter">Flow<span>.</span></div>
        
        <!-- 桌面端导航 (手机隐藏) -->
        <nav class="desktop-nav">
          <a v-for="cat in categories" 
             :key="cat.key" 
             @click="currentCategory = cat.key"
             :class="{ active: currentCategory === cat.key }">
             {{ cat.name }}
          </a>
        </nav>

        <div class="header-right">
           <!-- 搜索框 (手机端为了省空间，可以考虑点击展开，这里暂保持原样或缩小) -->
           <div class="search-box">
             <el-input 
               v-model="searchKeyword" 
               placeholder="搜文章..." 
               prefix-icon="Search"
               @keyup.enter="handleSearch"
               clearable
               @clear="handleSearch"
               class="search-input"
             />
           </div>

           <!-- 消息铃铛 -->
           <div class="notify-btn" @click="openNotifyDrawer">
               <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="item">
                   <el-icon :size="20"><Bell /></el-icon>
               </el-badge>
           </div>

           <!-- 用户头像 (登录后) -->
           <div v-if="isLoggedIn" class="user-area">
              <el-dropdown trigger="click" @command="handleUserCommand">
                <div class="avatar-wrapper">
                  <el-avatar :size="32" :src="userAvatar">{{ username.charAt(0).toUpperCase() }}</el-avatar>
                  <span class="username mobile-hide">{{ username }}</span> <!-- 手机端隐藏昵称文字 -->
                  <el-icon class="mobile-hide"><CaretBottom /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="write" v-if="isAdmin">写文章</el-dropdown-item>
                    <el-dropdown-item command="dashboard" v-if="isAdmin">管理后台</el-dropdown-item>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
           </div>
           
           <!-- 未登录 -->
           <el-button v-else type="primary" round size="small" @click="$router.push('/login')" class="login-btn">
             <span class="desktop-text">登录 / 注册</span>
             <span class="mobile-text">登录</span>
           </el-button>
        </div>
      </div>

      <!-- 标签栏 -->
      <div class="tag-bar">
        <div class="tag-scroll">
          <span class="tag-label"><el-icon><CollectionTag /></el-icon> 标签:</span>
          <span class="tag-item" :class="{ active: currentTag === 'all' }" @click="selectTag('all')">全部</span>
          <span v-for="tag in availableTags" :key="tag" class="tag-item" :class="{ active: currentTag === tag }" @click="selectTag(tag)">#{{ tag }}</span>
        </div>
      </div>
    </header>

    <!-- [新增] 移动端侧滑菜单 Drawer -->
    <el-drawer v-model="mobileMenuVisible" direction="ltr" :size="'70%'" :with-header="false" custom-class="mobile-menu-drawer">
        <div class="mobile-menu-content">
            <div class="menu-logo">Flow<span>.</span></div>
            <div class="menu-list">
                <div 
                    v-for="cat in categories" 
                    :key="cat.key" 
                    class="menu-item" 
                    :class="{ active: currentCategory === cat.key }"
                    @click="selectCategoryMobile(cat.key)"
                >
                    {{ cat.name }}
                </div>
                <div class="divider"></div>
                <div class="menu-item" @click="goToPage('/archive')"><el-icon><Calendar /></el-icon> 归档</div>
                <div class="menu-item" @click="goToPage('/friends')"><el-icon><Link /></el-icon> 友链</div>
                <div class="menu-item" @click="goToPage('/about')"><el-icon><InfoFilled /></el-icon> 关于</div>
            </div>
        </div>
    </el-drawer>

    <!-- Hero 区域 -->
    <section class="hero-section" v-if="currentCategory === 'all' && !searchKeyword">
      <div class="hero-content">
        <div class="hero-text">
          <h1>Flow State.</h1>
          <p>代码如水，生活有律。<br>这里是 Flow - Z.H.X 的Blog。</p>
        </div>
        <div class="blob-image"></div>
      </div>
    </section>

    <!-- 内容区 -->
    <main class="main-container">
      <div v-if="loading && posts.length === 0" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="filteredPosts.length > 0" class="masonry-grid">
        <div v-for="post in filteredPosts" :key="post.id" class="masonry-item">
          <el-card :body-style="{ padding: '0px' }" shadow="hover" class="post-card" @click="goToDetail(post.id)">
            <div v-if="post.coverImage" class="image-wrapper">
              <img :src="post.coverImage" class="cover-image" loading="lazy" />
              <span class="category-badge" :class="post.category ? post.category.toLowerCase() : ''">{{ post.category }}</span>
            </div>
            <div class="card-content">
              <div v-if="!post.coverImage" class="mb-2">
                <el-tag size="small" :type="getCategoryType(post.category)">{{ post.category }}</el-tag>
              </div>
              <h3 class="post-title">{{ post.title }}</h3>
              <p class="post-summary">{{ post.summary }}</p>
              <div class="post-footer">
                <span class="date">{{ formatDate(post.createTime) }}</span>
                <div class="stats">
                  <span><el-icon><View /></el-icon> {{ post.views || 0 }}</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
      
      <div v-else class="empty-state">
          <el-empty description="暂无内容" />
      </div>
      
      <div class="load-more-bar" v-if="posts.length > 0">
          <el-button v-if="hasMore" :loading="loadingMore" round @click="loadMore">加载更多</el-button>
          <p v-else class="no-more">—— 到底啦 ——</p>
      </div>
    </main>

    <!-- 消息通知抽屉 (右侧) -->
    <el-drawer v-model="drawerVisible" :size="380" direction="rtl" custom-class="notify-drawer">
        <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: bold; font-size: 16px;">未读消息</span>
                <el-button type="primary" link @click="handleMarkAllRead">全部已读</el-button>
            </div>
        </template>

        <div v-loading="drawerLoading" class="drawer-content">
            <div v-if="notifications.length > 0">
                <div v-for="item in notifications" :key="item.id" 
                    class="drawer-item" :class="{ 'unread': item.status === 0 }"
                    @click="handleNotificationClick(item)">
                    <div class="d-icon" :class="getIconClass(item.type)">
                        <el-icon><component :is="getIconName(item.type)" /></el-icon>
                    </div>
                    <div class="d-info">
                        <p class="d-title">
                            <strong>{{ item.senderName }}</strong> 
                            <span class="d-time">{{ formatDateSimple(item.createTime) }}</span>
                        </p>
                        <p class="d-desc">{{ item.content }}</p>
                    </div>
                    <div class="d-dot" v-if="item.status === 0"></div>
                </div>
            </div>
            <el-empty v-else description="暂无新消息" :image-size="80" />
        </div>
        <template #footer>
            <div style="flex: auto; text-align: center;">
                <el-button link type="primary" @click="goToAllNotifications">进入消息中心</el-button>
            </div>
        </template>
    </el-drawer>

    <!-- Footer -->
    <footer class="bg-white border-t border-gray-100 py-8 mt-auto" style="text-align:center; padding: 20px 0; color:#999; font-size:14px; border-top:1px solid #eee; background:#fff;">
        <div class="max-w-7xl mx-auto px-4">
            <p>&copy; 2025 Flow. Designed with Code & Poetry.</p>
            <p style="margin-top: 15px; font-size: 12px; color: #ccc; line-height: 1.5;">
                免责声明：本资源仅供个人学习、技术交流，如版权方认为分享行为侵权，<br>请通过站内信联系，本人将立即下架资源。
            </p>
            <div class="mt-2 space-x-4 footer-links">
                <router-link to="/archive">归档</router-link>
                <router-link to="/friends">友链</router-link>
                <router-link to="/about">关于</router-link>
            </div>
        </div>
    </footer>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
// [新增] Menu 图标, InfoFilled, Link
import { View, Search, CaretBottom, CollectionTag, Bell, ChatDotRound, Warning, Microphone, Menu, Calendar, Link, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(true)
const posts = ref([])
const currentCategory = ref('all')
const currentTag = ref('all') 
const availableTags = ref([]) 
const searchKeyword = ref('')

const currentPage = ref(1)
const pageSize = ref(9)
const hasMore = ref(true)
const loadingMore = ref(false)

const isLoggedIn = ref(false)
const isAdmin = ref(false)
const username = ref('User')
const userAvatar = ref('')
const unreadCount = ref(0)

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const notifications = ref([])

// [新增] 移动端菜单状态
const mobileMenuVisible = ref(false)

const categories = [
  { name: '全部', key: 'all' },
  { name: '生活 Life', key: 'Life' },
  { name: '技术 Tech', key: 'Tech' },
  { name: '发现 Discovery', key: 'Discovery' }
]

const fetchPosts = async (isLoadMore = false) => {
  try {
    if (isLoadMore) {
        loadingMore.value = true
    } else {
        loading.value = true
        currentPage.value = 1
        posts.value = []
    }

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      search: searchKeyword.value
    }
    if (currentTag.value !== 'all') {
      params.tag = currentTag.value
    }
    
    const res = await axios.get('/api/posts', { params })
    if (res.data.code === 200) {
      const newPosts = res.data.data.records || []
      const total = res.data.data.total
      if (isLoadMore) posts.value.push(...newPosts)
      else posts.value = newPosts
      hasMore.value = posts.value.length < total
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => { currentPage.value++; fetchPosts(true) }
const handleSearch = () => fetchPosts()
const selectTag = (tag) => { currentTag.value = tag; fetchPosts() }
const resetFilter = () => { currentCategory.value = 'all'; currentTag.value = 'all'; searchKeyword.value = ''; fetchPosts() }
const loadTags = async () => { try { const res = await axios.get('/api/tags'); if (res.data.code === 200) availableTags.value = res.data.data } catch(e){} }

const checkLoginStatus = () => {
  const token = localStorage.getItem('token')
  if (token) {
      isLoggedIn.value = true
      try {
        const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
        const payload = JSON.parse(decodeURIComponent(window.atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')))
        isAdmin.value = payload.role === 1
      } catch (e) {}
      getUserInfo(token)
      checkUnread(token)
  }
}

const getUserInfo = async (token) => {
    try {
        const res = await axios.get('/api/user/me', { headers: { Authorization: token } })
        if (res.data.code === 200) {
            const u = res.data.data
            username.value = u.nickname || u.username
            userAvatar.value = u.avatar
        }
    } catch(e) {}
}

const handleUserCommand = (command) => {
  if (command === 'logout') { localStorage.removeItem('token'); isLoggedIn.value = false; isAdmin.value = false; unreadCount.value = 0; ElMessage.info('已退出登录'); router.push('/') }
  else if (command === 'write') { router.push('/editor') } 
  else if (command === 'dashboard') { router.push('/dashboard') } 
  else if (command === 'profile') { router.push('/profile') }
}

const checkUnread = async (token) => {
    try {
        const res = await axios.get('/api/notifications/unread-count', { headers: { Authorization: token } })
        if (res.data.code === 200) unreadCount.value = res.data.data
    } catch(e) {}
}

const openNotifyDrawer = async () => {
    drawerVisible.value = true
    drawerLoading.value = true
    try {
        const token = localStorage.getItem('token')
        const headers = {}
        if (token) headers['Authorization'] = token
        
        const res = await axios.get('/api/notifications', { headers })
        if (res.data.code === 200) notifications.value = res.data.data
    } finally {
        drawerLoading.value = false
    }
}

const handleNotificationClick = async (item) => {
    if (item.status === 0) {
        try {
            const token = localStorage.getItem('token')
            await axios.put(`/api/notifications/${item.id}/read`, {}, { headers: { Authorization: token } })
            item.status = 1
            if (unreadCount.value > 0) unreadCount.value--
        } catch(e) {}
    }
    
    if (item.type === 2) { 
        router.push('/dashboard') 
    } else if (item.type === 3) {
        ElMessageBox.alert(item.content, item.senderName || '公告', {
            confirmButtonText: '知道了',
            type: 'info'
        })
    } else { 
        if (item.postId) {
            router.push(`/post/${item.postId}`) 
        } else {
            ElMessage.warning('该内容可能已被删除')
        }
    }
    drawerVisible.value = false
}

const handleMarkAllRead = async () => {
    try {
        const token = localStorage.getItem('token')
        await axios.put('/api/notifications/read-all', {}, { headers: { Authorization: token } })
        notifications.value.forEach(n => n.status = 1)
        unreadCount.value = 0
        ElMessage.success('已全部标记为已读')
    } catch(e) {}
}

const goToAllNotifications = () => { router.push('/notifications'); drawerVisible.value = false }
const formatDateSimple = (iso) => { if (!iso) return ''; const d = new Date(iso); return `${d.getMonth()+1}-${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2,'0')}` }
const goToDetail = (id) => router.push(`/post/${id}`)
const filteredPosts = computed(() => { return posts.value.filter(post => { return currentCategory.value === 'all' || post.category === currentCategory.value }) })
const getCategoryType = (cat) => cat === 'Tech' ? 'primary' : (cat === 'Life' ? 'success' : 'warning')
const formatDate = (iso) => iso ? new Date(iso).toLocaleDateString() : ''

const getIconClass = (type) => {
    if (type === 2) return 'audit' 
    if (type === 3) return 'system' 
    return '' 
}

const getIconName = (type) => {
    if (type === 2) return 'Warning'
    if (type === 3) return 'Microphone'
    return 'ChatDotRound'
}

// [新增] 移动端导航跳转
const selectCategoryMobile = (key) => {
    currentCategory.value = key
    mobileMenuVisible.value = false
}
const goToPage = (path) => {
    router.push(path)
    mobileMenuVisible.value = false
}

onMounted(() => {
  checkLoginStatus()
  loadTags()
  fetchPosts()
})
</script>

<style scoped lang="scss">
$nav-height: 64px;
$tag-bar-height: 48px;

.common-layout { min-height: 100vh; background-color: #f9fafb; display: flex; flex-direction: column; }

/* 1. 导航栏 */
.sticky-header { 
  position: sticky; top: 0; z-index: 100; 
  background: rgba(255, 255, 255, 0.75) !important; 
  backdrop-filter: blur(12px); 
  border-bottom: 1px solid rgba(0,0,0,0.05); 
  .header-content { max-width: 1200px; margin: 0 auto; height: $nav-height; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; 
    
    /* [新增] 移动端菜单按钮 */
    .mobile-menu-btn {
        display: none; /* 默认隐藏 */
        cursor: pointer;
        margin-right: 15px;
        color: #606266;
    }

    .logo { font-size: 1.5rem; font-weight: 800; cursor: pointer; span { color: #409EFF; } }
    
    .desktop-nav { display: flex; gap: 30px; a { cursor: pointer; font-weight: 500; color: #606266; transition: color 0.3s; &.active, &:hover { color: #409EFF; } } }
    
    .header-right { display: flex; align-items: center; gap: 15px; 
        .search-box { width: 160px; transition: width 0.3s; :deep(.el-input__wrapper) { border-radius: 20px; background-color: rgba(255,255,255,0.8); } &:focus-within { width: 220px; } }
        .user-area { cursor: pointer; .avatar-wrapper { display: flex; align-items: center; gap: 8px; .username { font-size: 14px; font-weight: 600; color: #333; } } }
        
        .desktop-text { display: inline; }
        .mobile-text { display: none; }
    }
  }
  .tag-bar { border-top: 1px solid rgba(0,0,0,0.03); background: rgba(255,255,255,0.6); backdrop-filter: blur(5px); .tag-scroll { max-width: 1200px; margin: 0 auto; height: $tag-bar-height; display: flex; align-items: center; overflow-x: auto; padding: 0 20px; gap: 10px; &::-webkit-scrollbar { display: none; } .tag-label { font-size: 12px; color: #909399; margin-right: 5px; flex-shrink: 0; display:flex; align-items:center; gap:4px;} .tag-item { font-size: 13px; padding: 4px 12px; background: rgba(0,0,0,0.05); border-radius: 15px; color: #606266; cursor: pointer; white-space: nowrap; transition: all 0.2s; &:hover { background: rgba(0,0,0,0.1); } &.active { background: #333; color: #fff; } } } }
}

/* [新增] 移动端样式适配 */
@media (max-width: 768px) { 
    .sticky-header .header-content {
        /* 调整padding */
        padding: 0 15px;
        
        /* 显示汉堡菜单 */
        .mobile-menu-btn { display: block; }
        
        /* 隐藏桌面导航 */
        .desktop-nav { display: none; }
        
        .header-right {
            gap: 10px;
            /* 缩小搜索框 */
            .search-box { width: 120px; &:focus-within { width: 140px; } }
            
            .login-btn {
                padding: 5px 10px;
                .desktop-text { display: none; }
                .mobile-text { display: inline; }
            }
            
            /* 隐藏头像旁的文字和图标 */
            .mobile-hide { display: none; }
        }
    }
}

/* [新增] 侧滑菜单样式 */
.mobile-menu-content {
    padding: 20px;
    .menu-logo { font-size: 1.5rem; font-weight: 800; margin-bottom: 30px; span { color: #409EFF; } }
    .menu-list {
        display: flex; flex-direction: column; gap: 15px;
        .menu-item {
            font-size: 16px; font-weight: 500; color: #606266; cursor: pointer; display: flex; align-items: center; gap: 10px;
            &.active { color: #409EFF; font-weight: 600; }
        }
        .divider { height: 1px; background: #eee; margin: 10px 0; }
    }
}

/* Hero 区域样式 */
.hero-section {
  padding: 60px 20px;
  min-height: 400px;
  .hero-content {
    max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center;
  }
  .hero-text {
    max-width: 50%;
    h1 { font-family: 'Georgia', 'Noto Serif SC', serif; font-size: 3rem; color: #2c3e50; margin-bottom: 20px; line-height: 1.2; }
    p { font-size: 1.1rem; color: #7f8c8d; line-height: 1.6; }
  }
  .blob-image {
    width: 350px; height: 350px;
    background-image: url('/images/hero-bg.jpg');
    background-size: cover; background-position: center;
    border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%;
    animation: blob-bounce 8s infinite alternate;
    box-shadow: 20px 20px 60px rgba(0,0,0,0.1);
  }
}

@keyframes blob-bounce {
  0% { border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%; }
  100% { border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%; }
}

@media (max-width: 768px) {
    .hero-section {
        .hero-content { flex-direction: column-reverse; text-align: center; }
        .hero-text { max-width: 100%; margin-top: 30px; h1 { font-size: 2.2rem; } }
        .blob-image { width: 280px; height: 280px; }
    }
}

.main-container { max-width: 1200px; margin: 0 auto; padding: 20px; flex: 1; width: 100%; }
.masonry-grid { column-count: 3; column-gap: 20px; @media (max-width: 992px) { column-count: 2; } @media (max-width: 600px) { column-count: 1; } }
.masonry-item { break-inside: avoid; margin-bottom: 20px; }
.post-card { border: none; border-radius: 12px; transition: transform 0.3s; cursor: pointer; &:hover { transform: translateY(-5px); } .image-wrapper { position: relative; overflow: hidden; .cover-image { width: 100%; height: auto; display: block; } .category-badge { position: absolute; top: 10px; left: 10px; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: bold; color: #fff; background: rgba(0,0,0,0.6); &.tech { background: #409EFF; } &.life { background: #67C23A; } } } .card-content { padding: 16px; .post-title { font-size: 1.1rem; margin: 0 0 10px; line-height: 1.4; font-family: 'Georgia', sans-serif; } .post-summary { font-size: 14px; color: #666; line-height: 1.6; margin-bottom: 15px; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; } .post-footer { display: flex; justify-content: space-between; color: #999; font-size: 12px; border-top: 1px solid #f0f0f0; padding-top: 10px; } } }

.notify-btn { cursor: pointer; display: flex; align-items: center; margin-right: 15px; color: #606266; } .notify-btn:hover { color: #409EFF; }
/* 抽屉样式 */ .drawer-item { display: flex; padding: 15px 0; border-bottom: 1px solid #f5f5f5; cursor: pointer; transition: background 0.2s; position: relative; } .drawer-item:hover { background: #fafafa; } .drawer-item.unread { background: #f0f9eb; } .d-icon { margin-right: 12px; display: flex; align-items: center; font-size: 18px; color: #409EFF; } .d-icon.audit { color: #E6A23C; } .d-icon.system { color: #F56C6C; } .d-info { flex: 1; overflow: hidden; } .d-title { font-size: 13px; margin: 0 0 4px; color: #333; display: flex; justify-content: space-between; } .d-time { font-size: 12px; color: #999; font-weight: normal; } .d-desc { margin: 0; color: #666; font-size: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; } .d-dot { position: absolute; left: -8px; top: 18px; width: 6px; height: 6px; background: #f56c6c; border-radius: 50%; }

.load-more-bar { text-align: center; margin: 30px 0; }
.no-more { color: #999; font-size: 13px; }
.empty-state { padding: 40px; text-align: center; }
.footer-links a { color: #666; cursor: pointer; text-decoration: none; margin: 0 10px; font-size: 14px; &:hover { color: #409EFF; } }
</style>