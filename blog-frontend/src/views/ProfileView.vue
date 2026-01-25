<template>
  <div class="profile-container">
    <div class="profile-header">
      <h2>个人中心</h2>
      <el-button @click="$router.push('/')">返回首页</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover" class="info-card">
          <div class="avatar-area">
            <el-upload
              class="avatar-uploader"
              action="/api/upload"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeAvatarUpload"
            >
              <img v-if="form.avatar" :src="form.avatar" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              <div class="mask"><el-icon><Camera /></el-icon></div>
            </el-upload>
            
            <!-- 显示昵称 -->
            <h3>{{ form.nickname || form.username || 'User' }}</h3>
            <p>{{ roleName }}</p>
          </div>
          <el-divider />
          <div class="stats">
            <div><strong>-</strong><br><span>文章</span></div>
            <div><strong>-</strong><br><span>评论</span></div>
            <div><strong>-</strong><br><span>获赞</span></div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="info">
              <el-form label-width="80px" class="setting-form">
                <el-form-item label="用户名">
                  <el-input v-model="form.username" disabled placeholder="不可修改" />
                </el-form-item>
                <!-- [新增] 昵称输入框 -->
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" placeholder="设置一个好听的名字" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" placeholder="绑定邮箱" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="saving" @click="saveInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="账号安全" name="security">
              <!-- 密码表单保持不变 -->
              <el-form label-width="100px" class="setting-form" :model="pwdForm">
                <el-form-item label="原密码">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" :loading="pwdSaving" @click="changePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Plus, Camera } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('info')
const saving = ref(false)
const pwdSaving = ref(false)

const form = reactive({
  id: null,
  username: '',
  nickname: '', // [新增]
  email: '',
  avatar: '',
  role: 0
})

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const roleName = computed(() => form.role === 1 ? '管理员' : '普通用户')

const loadUser = async () => {
  try {
    const token = localStorage.getItem('token')
    if (!token) { router.push('/login'); return }
    const res = await axios.get('/api/user/me', { headers: { Authorization: token } })
    if (res.data.code === 200) { Object.assign(form, res.data.data) }
  } catch (e) { ElMessage.error('获取用户信息失败') }
}

const handleAvatarSuccess = (res) => {
  if (res.code === 200) { form.avatar = res.data; ElMessage.success('头像已上传，请点击保存') } 
  else { ElMessage.error(res.msg) }
}
const beforeAvatarUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') { ElMessage.error('格式错误'); return false }
  if (rawFile.size / 1024 / 1024 > 2) { ElMessage.error('大小不能超过 2MB'); return false }
  return true
}

const saveInfo = async () => {
  saving.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.put('/api/user/me', form, { headers: { Authorization: token } })
    if (res.data.code === 200) { ElMessage.success('保存成功') } 
    else { ElMessage.error(res.data.msg) }
  } catch (e) { ElMessage.error('保存失败') } 
  finally { saving.value = false }
}

const changePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) return ElMessage.warning('请输入密码')
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return ElMessage.warning('两次新密码不一致')
  pwdSaving.value = true
  try {
    const token = localStorage.getItem('token')
    const res = await axios.put('/api/user/password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword }, { headers: { Authorization: token } })
    if (res.data.code === 200) { ElMessage.success('密码修改成功，请重新登录'); localStorage.removeItem('token'); router.push('/login') } 
    else { ElMessage.error(res.data.msg) }
  } catch (e) { ElMessage.error('请求错误') } 
  finally { pwdSaving.value = false }
}

onMounted(() => { loadUser() })
</script>

<style scoped>
/* 样式与之前保持一致 */
.profile-container { max-width: 1000px; margin: 40px auto; padding: 20px; }
.profile-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.info-card { text-align: center; }
.avatar-area { padding: 20px 0; position: relative; }
.avatar-uploader { width: 100px; height: 100px; border-radius: 50%; border: 1px dashed #d9d9d9; margin: 0 auto 15px; cursor: pointer; position: relative; overflow: hidden; display: flex; justify-content: center; align-items: center; }
.avatar-uploader:hover { border-color: #409EFF; }
.avatar-uploader:hover .mask { opacity: 1; }
.avatar { width: 100%; height: 100%; object-fit: cover; }
.avatar-uploader-icon { font-size: 28px; color: #8c939d; }
.mask { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); color: #fff; opacity: 0; transition: opacity 0.3s; display: flex; justify-content: center; align-items: center; font-size: 24px; }
.avatar-area h3 { margin: 10px 0 5px; }
.avatar-area p { color: #999; margin: 0; font-size: 13px; }
.stats { display: flex; justify-content: space-around; padding: 10px 0; }
.stats div { font-size: 14px; color: #666; }
.stats strong { font-size: 18px; color: #333; }
.setting-form { max-width: 500px; margin-top: 20px; }
</style>