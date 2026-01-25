<template>
  <div class="editor-container" v-loading="loading">
    <header class="editor-header">
      <el-input v-model="post.title" placeholder="请输入文章标题..." class="title-input" />
      
      <div class="actions">
        <el-upload
          class="upload-btn-item"
          action="/api/upload"
          :show-file-list="false"
          :on-success="handleCoverSuccess"
          :before-upload="beforeUpload"
        >
          <el-button v-if="!post.coverImage" icon="Picture" plain>封面</el-button>
          <img v-else :src="post.coverImage" class="cover-mini-preview" title="点击更换封面" />
        </el-upload>

        <el-upload
          class="upload-btn-item"
          action="/api/upload"
          :show-file-list="false"
          :on-success="handleContentImageSuccess"
          :before-upload="beforeUpload"
        >
          <el-button icon="PictureFilled" plain>插图</el-button>
        </el-upload>

        <!-- [修复] 增加 :reserve-keyword="false" 自动清空输入 -->
        <el-select
          v-model="post.tags"
          multiple
          filterable
          allow-create
          default-first-option
          :reserve-keyword="false"
          placeholder="标签"
          style="width: 180px"
        >
          <el-option v-for="tag in allTags" :key="tag" :label="tag" :value="tag" />
        </el-select>

        <el-select v-model="post.category" placeholder="分类" style="width: 100px">
          <el-option label="技术" value="Tech" />
          <el-option label="生活" value="Life" />
          <el-option label="发现" value="Discovery" />
        </el-select>
        
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="publish">
          {{ isEdit ? '更新' : '发布' }}
        </el-button>
      </div>
    </header>

    <div class="main-area">
      <textarea 
        ref="textareaRef"
        class="markdown-input" 
        v-model="post.content" 
        placeholder="开始书写你的故事... (支持 Markdown 语法)"
      ></textarea>
      <div class="markdown-preview markdown-body" v-html="previewContent"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'
import { Picture, PictureFilled } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const submitting = ref(false)
const loading = ref(false)
const isEdit = ref(false)
const textareaRef = ref(null)

const post = reactive({
  id: null,
  title: '',
  category: '',
  content: '',
  coverImage: '',
  tags: [] 
})

const allTags = ref([]) 

const previewContent = computed(() => {
  if (!post.content) return '<p style="color:#ccc; padding:20px;">预览区域</p>'
  try { return marked.parse(post.content) } catch (e) { return '解析错误' }
})

const loadPost = async (id) => {
  loading.value = true
  try {
    const res = await axios.get(`/api/posts/${id}`)
    if (res.data.code === 200) {
      const data = res.data.data
      Object.assign(post, data)
      if (!post.tags) post.tags = []
    }
  } catch (e) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

const loadTags = async () => {
  try {
    const res = await axios.get('/api/tags')
    if (res.data.code === 200) allTags.value = res.data.data
  } catch(e){}
}

const beforeUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png' && rawFile.type !== 'image/gif') {
    ElMessage.error('仅支持 JPG/PNG/GIF 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过 5MB!')
    return false
  }
  return true
}
const handleCoverSuccess = (res) => {
  if (res.code === 200) {
    post.coverImage = res.data
    ElMessage.success('封面设置成功')
  } else {
    ElMessage.error(res.msg)
  }
}
const handleContentImageSuccess = (res) => {
  if (res.code === 200) {
    const url = res.data
    const markdownImage = `\n![图片描述](${url})\n`
    insertToTextarea(markdownImage)
    ElMessage.success('图片插入成功')
  } else {
    ElMessage.error(res.msg)
  }
}
const insertToTextarea = (textStr) => {
  const textarea = textareaRef.value
  if (!textarea) return
  const startPos = textarea.selectionStart
  const endPos = textarea.selectionEnd
  const oldVal = post.content
  post.content = oldVal.substring(0, startPos) + textStr + oldVal.substring(endPos)
  setTimeout(() => {
    textarea.focus()
    textarea.selectionStart = textarea.selectionEnd = startPos + textStr.length
  }, 10)
}

const publish = async () => {
  if (!post.title) return ElMessage.warning('标题不能为空')
  if (!post.content) return ElMessage.warning('内容不能为空')
  if (!post.category) return ElMessage.warning('请选择分类')

  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.error('请先登录')
    router.push('/login') 
    return
  }

  try {
    submitting.value = true
    const res = await axios.post('/api/posts', post, { headers: { 'Authorization': token } })
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '发布成功')
      router.push('/') 
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('网络请求错误')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTags() 
  if (route.query.id) {
    isEdit.value = true
    loadPost(route.query.id)
  }
})
</script>

<style scoped>
.editor-container { height: 100vh; display: flex; flex-direction: column; background: #fff; }
.editor-header { height: 60px; border-bottom: 1px solid #ddd; display: flex; align-items: center; padding: 0 20px; justify-content: space-between; box-shadow: 0 2px 4px rgba(0,0,0,0.05); z-index: 10; }
.title-input { font-size: 18px; font-weight: bold; flex: 1; margin-right: 20px; }
:deep(.title-input .el-input__wrapper) { box-shadow: none !important; padding-left: 0; }
.actions { display: flex; align-items: center; gap: 12px; }
.upload-btn-item { display: flex; align-items: center; }
.cover-mini-preview { height: 32px; width: 50px; object-fit: cover; border-radius: 4px; border: 1px solid #eee; cursor: pointer; }
.main-area { flex: 1; display: flex; overflow: hidden; }
.markdown-input { width: 50%; height: 100%; border: none; border-right: 1px solid #eee; padding: 20px; font-size: 16px; font-family: 'Consolas', 'Monaco', monospace; outline: none; resize: none; background-color: #fcfcfc; line-height: 1.6; }
.markdown-preview { width: 50%; height: 100%; padding: 20px 40px; overflow-y: auto; }
.markdown-body { font-family: 'Noto Serif SC', serif; line-height: 1.8; color: #2c3e50; }
.markdown-body :deep(h1), .markdown-body :deep(h2) { border-bottom: 1px solid #eee; padding-bottom: 10px; }
.markdown-body :deep(pre) { background: #f6f8fa; padding: 16px; border-radius: 6px; overflow: auto; }
.markdown-body :deep(blockquote) { border-left: 4px solid #409EFF; padding-left: 15px; color: #666; background: #f9f9f9; }
.markdown-body :deep(img) { max-width: 100%; border-radius: 6px; margin: 10px 0; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
</style>