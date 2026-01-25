import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ArticleView from '../views/ArticleView.vue'
import EditorView from '../views/EditorView.vue'
import DashboardView from '../views/DashboardView.vue'
import ArchiveView from '../views/ArchiveView.vue'
import FriendsView from '../views/FriendsView.vue'
// 1. 引入新页面
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import AboutView from '../views/AboutView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/post/:id', name: 'article', component: ArticleView },
    { path: '/editor', name: 'editor', component: EditorView },
    { path: '/dashboard', name: 'dashboard', component: DashboardView },
    { path: '/archive', name: 'archive', component: ArchiveView },
    { path: '/friends', name: 'friends', component: FriendsView },
    // 2. 新增路由
    { path: '/login', name: 'login', component: LoginView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/notifications', name: 'notifications', component: NotificationsView },
    { path: '/about', name: 'about', component: AboutView }
  ]
})

export default router