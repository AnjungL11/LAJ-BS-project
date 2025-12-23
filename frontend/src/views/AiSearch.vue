<template>
  <div class="ai-page-container">
    <el-card class="chat-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">AI 智能图库助手</span>
          <el-tag type="info" size="small">基于大模型检索</el-tag>
        </div>
      </template>

      <div class="chat-layout">
        <div class="messages" ref="messagesRef">
          <div 
            v-for="(msg, index) in chatHistory" 
            :key="index" 
            class="message-item"
            :class="msg.role"
          >
            <div class="avatar">
              <el-icon v-if="msg.role === 'ai'"><Cpu /></el-icon>
              <el-icon v-else><User /></el-icon>
            </div>
            <div class="content">
              <div class="bubble">
                <p v-if="msg.text" style="white-space: pre-wrap;">{{ msg.text }}</p>
              </div>
              
              <div v-if="msg.images && msg.images.length > 0" class="result-grid">
                <div 
                  v-for="img in msg.images" 
                  :key="img.imageId" 
                  class="result-card"
                  @click="goToDetail(img.imageId)"
                >
                  <img :src="getImageUrl(img.thumbnailPath)" />
                  <div class="img-info">
                    <span class="file-name">{{ img.originalFilename }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="loading" class="message-item ai">
            <div class="avatar"><el-icon><Cpu /></el-icon></div>
            <div class="content">
              <div class="bubble">正在思考并检索中... <el-icon class="is-loading"><Loading /></el-icon></div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="prompt"
              placeholder="描述你想找的图片，例如：'找一下去年拍摄的红色阀门'..."
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 4 }"
              resize="none"
              @keydown.enter.prevent="handleSend"
            />
            <el-button type="primary" :icon="Promotion" :loading="loading" @click="handleSend" class="send-btn">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, User, Promotion, Loading } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const prompt = ref('')
const loading = ref(false)
const messagesRef = ref(null)

const chatHistory = ref([
  { role: 'ai', text: '你好！我是你的图片助手。请告诉我你想找什么？' }
])

const handleSend = async () => {
  if (!prompt.value.trim() || loading.value) return
  
  const userText = prompt.value
  prompt.value = ''
  
  chatHistory.value.push({ role: 'user', text: userText })
  scrollToBottom()
  loading.value = true

  try {
    // 1. 发起请求
    const res = await request.post('/mcp/search', { keyword: userText })
    
    // ▼▼▼▼▼▼▼▼▼▼ 核心修复：数据解包 ▼▼▼▼▼▼▼▼▼▼
    console.log('后端返回:', res) // 打开 F12 看一眼结构

    // 兼容处理：你的 request.js 可能返回 response.data，也可能返回 response.data.data
    // 如果 res 里面有 code=200 且 data 存在，说明是被 Result 包裹的
    let realData = res
    if (res && (res.code === 200 || res.code === '200') && res.data) {
      realData = res.data
    }
    
    // 获取字段
    const replyText = realData.reply || 'AI 没有返回文字回复'
    const imageResults = realData.records || [] 
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    chatHistory.value.push({
      role: 'ai',
      text: replyText,
      images: imageResults
    })

  } catch (error) {
    chatHistory.value.push({ role: 'ai', text: '前端解析数据出错，请按 F12 查看控制台。' })
    console.error(error)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const getImageUrl = (path) => {
  if (!path) return ''
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return `/uploads/thumb/${filename}`
}

const goToDetail = (id) => {
  router.push(`/detail/${id}`)
}
</script>

<style scoped>
.ai-page-container {
  height: 100%; /* 填满 MainLayout 的内容区 */
  display: flex;
  flex-direction: column;
}

.chat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
  background: #fff;
  border-radius: 8px;
  overflow: hidden; /* 防止双重滚动条 */
}

/* 深度选择器修改 el-card 内部样式，使其充满 */
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0; /* 去掉内边距，让聊天区域满屏 */
  height: 0; /* 关键：配合 flex:1 实现高度自适应 */
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: #f9f9fa;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 800px; /* 限制最大宽度，便于阅读 */
}

.message-item.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.ai .avatar { background: #fff; color: #764ba2; border: 1px solid #eee; }
.user .avatar { background: #764ba2; color: #fff; }

.content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 80%;
}

.bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  color: #333;
  line-height: 1.6;
  font-size: 15px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  border: 1px solid #eee;
}

.user .bubble {
  background: #e0d4fc; /* 主题色系的浅色 */
  border-color: #d3c4f5;
  color: #4a2b75;
}

/* 结果网格优化 */
.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-top: 5px;
}

.result-card {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #fff;
  transition: all 0.2s;
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.result-card img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.img-info {
  padding: 6px;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 底部输入区优化 */
.input-area {
  background: #fff;
  padding: 20px;
  border-top: 1px solid #eee;
}

.input-wrapper {
  max-width: 800px;
  margin: 0 auto; /* 居中 */
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.send-btn {
  height: auto;
  padding: 10px 20px;
}
</style>