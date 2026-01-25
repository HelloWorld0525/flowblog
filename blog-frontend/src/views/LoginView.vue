<template>
  <div class="login-container">
    <!-- 背景遮罩，确保文字清晰 -->
    <div class="bg-overlay"></div>

    <el-card class="login-card" shadow="never">
      <div class="card-header">
        <h2 @click="$router.push('/')">Flow<span>.</span></h2>
        <p>Code like water, live with rhythm.</p>
      </div>

      <el-tabs v-model="activeTab" stretch class="custom-tabs">
        <!-- 登录 Tab -->
        <el-tab-pane label="登 录" name="login">
          <el-form :model="loginForm" size="large" class="auth-form" @submit.prevent>
            <el-form-item>
              <el-input 
                v-model="loginForm.username" 
                placeholder="用户名 / 手机号" 
                prefix-icon="User" 
                class="glass-input"
              />
            </el-form-item>
            <el-form-item>
              <el-input 
                v-model="loginForm.password" 
                type="password" 
                placeholder="密码" 
                prefix-icon="Lock" 
                show-password 
                @keyup.enter="handleLogin" 
                class="glass-input"
              />
            </el-form-item>
            <el-button type="primary" :loading="loading" class="full-btn" round @click="handleLogin">
              进入花园
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- 注册 Tab -->
        <el-tab-pane label="注 册" name="register">
          
          <!-- 注册提示信息 -->
          <div class="tips-box">
            <el-alert
              title="注册须知"
              type="warning"
              :closable="false"
              show-icon
              description="1. 建议使用手机号作为用户名，方便记忆。
              2. 本站暂未开通邮件找回功能，忘记密码请联系站长重置。"
            />
          </div>

          <el-form :model="registerForm" size="large" class="auth-form" @submit.prevent>
            <el-form-item>
              <el-input 
                v-model="registerForm.username" 
                placeholder="用户名 (建议手机号)" 
                prefix-icon="User" 
                class="glass-input"
              />
            </el-form-item>
            
            <!-- [修改] 昵称设置增加 (选填) -->
            <el-form-item>
              <el-input 
                v-model="registerForm.nickname" 
                placeholder="昵称 (选填)" 
                prefix-icon="Postcard" 
                class="glass-input"
              />
            </el-form-item>

            <el-form-item>
              <el-input 
                v-model="registerForm.password" 
                type="password" 
                placeholder="设置密码" 
                prefix-icon="Lock" 
                show-password 
                class="glass-input"
              />
            </el-form-item>
            
            <el-button type="success" :loading="loading" class="full-btn" round @click="handleRegister">
              创建账号
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="social-login">
        <!-- [修改] 优化分割线样式 -->
        <div class="glass-divider">
            <span>第三方登录 (待开发)</span>
        </div>
        <div class="icons">
          <div class="icon-btn github"><i class="fab fa-github"></i></div>
          <div class="icon-btn google"><i class="fab fa-google"></i></div>
          <div class="icon-btn wechat"><i class="fab fa-weixin"></i></div>
        </div>
      </div>
    </el-card>
    
    <!-- 底部版权 -->
    <div class="login-footer">
        &copy; 2025 Flow. Designed by Zhang Hongxiang.
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue' 

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '' })

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入完整')
  loading.value = true
  try {
    const res = await axios.post('/api/login', loginForm)
    if (res.data.code === 200) {
      localStorage.setItem('token', res.data.data)
      ElMessage.success('欢迎回来')
      router.push('/')
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch(e) {
      ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  try {
    const res = await axios.post('/api/register', registerForm)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.username = registerForm.username
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch(e) {
      ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  /* [修改] 推荐使用本地图片 (请确保 public/images/login-bg.jpg 存在) */
  /* 如果没有本地图片，可以暂时把 url 改回之前的 Unsplash 链接 */
  background: url('/images/login-bg.jpg') no-repeat center center;
  background-size: cover;
  /* 备用背景色，防止图片加载失败 */
  background-color: #eef2f3; 
  flex-direction: column;
}

.bg-overlay {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    background: rgba(0, 0, 0, 0.2); /* 稍微调浅一点，让背景图更透亮 */
    z-index: 0;
}

/* 玻璃拟态卡片 */
.login-card {
  width: 420px;
  padding: 30px 20px;
  border-radius: 16px;
  /* 背景透明度调整，增加磨砂感 */
  background: rgba(255, 255, 255, 0.75); 
  backdrop-filter: blur(20px); 
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 1;
  position: relative;
  /* [新增] 入场动画 */
  animation: fade-in-up 0.8s cubic-bezier(0.2, 0.8, 0.2, 1);
  
  :deep(.el-card__body) { padding: 0; }
}

/* [新增] 动画定义 */
@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header {
  text-align: center;
  margin-bottom: 30px;
  
  h2 {
    font-size: 2.2rem;
    font-weight: 800;
    margin: 0;
    cursor: pointer;
    color: #2c3e50;
    letter-spacing: -1px;
    transition: transform 0.3s;
    &:hover { transform: scale(1.05); } /* Logo 微动效 */
    span { color: #409EFF; }
  }
  
  p {
    color: #606266;
    margin: 8px 0 0;
    font-size: 0.9rem;
    font-family: 'Georgia', serif;
    font-style: italic;
  }
}

.tips-box {
    margin-bottom: 20px;
    :deep(.el-alert) {
        background-color: rgba(230, 162, 60, 0.1);
        border: 1px solid rgba(230, 162, 60, 0.2);
        .el-alert__description {
            color: #E6A23C;
            font-size: 12px;
            line-height: 1.5;
            margin-top: 5px;
            white-space: pre-line;
        }
    }
}

.auth-form {
  margin-top: 10px;
  
  /* 输入框样式微调 */
  :deep(.el-input__wrapper) {
      background-color: rgba(255, 255, 255, 0.5); /* 更通透 */
      box-shadow: 0 0 0 1px #dcdfe6 inset;
      transition: all 0.3s ease;
      
      /* [新增] 聚焦时的微动效 */
      &:hover {
          background-color: rgba(255, 255, 255, 0.8);
      }
      &.is-focus {
          background-color: #fff;
          box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3) inset, 0 0 0 1px #409EFF inset;
          transform: translateY(-1px);
      }
  }
}

.full-btn {
  width: 100%;
  margin-top: 10px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 2px;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  border: none;
  transition: all 0.3s;
  
  &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(64, 158, 255, 0.4);
  }
  &:active { transform: scale(0.98); }
}

.social-login {
  margin-top: 40px;
  
  /* [修改] 自定义分割线样式，解决背景突兀问题 */
  .glass-divider {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      position: relative;
      
      &::before, &::after {
          content: '';
          flex: 1;
          height: 1px;
          background: rgba(0,0,0,0.1); /* 淡淡的线条 */
      }
      
      span {
          padding: 0 15px;
          color: #909399;
          font-size: 12px;
          font-weight: 500;
      }
  }

  .icons {
      display: flex;
      justify-content: center;
      gap: 25px;
      
      .icon-btn {
          width: 40px; height: 40px; border-radius: 50%;
          background: rgba(255, 255, 255, 0.8);
          display: flex; justify-content: center; align-items: center;
          cursor: pointer;
          font-size: 20px;
          color: #606266;
          box-shadow: 0 2px 8px rgba(0,0,0,0.05);
          transition: all 0.3s;
          
          &:hover { 
              transform: translateY(-3px) rotate(10deg); /* 增加旋转动效 */
              box-shadow: 0 5px 15px rgba(0,0,0,0.15); 
              color: #409EFF; 
              background: #fff;
          }
      }
  }
}

.login-footer {
    position: absolute;
    bottom: 20px;
    color: rgba(255, 255, 255, 0.8);
    font-size: 12px;
    z-index: 1;
    text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
</style>