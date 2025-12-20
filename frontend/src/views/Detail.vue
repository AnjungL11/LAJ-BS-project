<template>
  <div class="detail-page" v-loading="loading">
    <div class="left-panel">
      <div class="image-preview">
        <img v-if="imageInfo" :src="getImageUrl(imageInfo.storagePath, false)" alt="preview" />
        <el-empty v-else description="图片加载失败" />
      </div>
      
      <div class="edit-toolbar">
         <el-button-group>
           <el-button type="primary" plain :icon="Crop" @click="showCrop = true">裁剪</el-button>
           <el-button type="primary" plain :icon="MagicStick" @click="generateAiTags">AI 分析</el-button>
           <el-button type="danger" plain :icon="Delete" @click="deleteImage">删除</el-button>
         </el-button-group>
      </div>
    </div>
    
    <div class="right-panel">
      <el-descriptions title="基本信息" :column="1" border>
        <el-descriptions-item label="文件名">{{ imageInfo?.originalFilename || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(imageInfo?.fileSize || 0) }}</el-descriptions-item>
        <el-descriptions-item label="上传时间">{{ formatTime(imageInfo?.uploadedAt) }}</el-descriptions-item>
      </el-descriptions>

      <div class="section-block">
        <div class="section-header">
          <h4>标签管理</h4>
        </div>
        <div class="tags">
          <el-tag 
            v-for="(tag, index) in tags" 
            :key="index" 
            class="tag-item" 
            closable 
            @close="removeTag(index)"
          >
            {{ tag }}
          </el-tag>
          
          <el-input
            v-if="inputVisible"
            ref="InputRef"
            v-model="inputValue"
            class="input-new-tag"
            size="small"
            @keyup.enter="handleInputConfirm"
            @blur="handleInputConfirm"
          />
          <el-button v-else class="button-new-tag" size="small" @click="showInput">
            + 新标签
          </el-button>
        </div>
      </div>

      <el-descriptions title="EXIF 信息" :column="1" border class="mt-20" v-if="imageInfo?.metadata">
        <el-descriptions-item label="相机型号">{{ imageInfo.metadata.cameraModel || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="拍摄时间">{{ formatTime(imageInfo.metadata.takenTime) }}</el-descriptions-item>
        <el-descriptions-item label="分辨率">{{ imageInfo.metadata.width }} x {{ imageInfo.metadata.height }}</el-descriptions-item>
        <el-descriptions-item label="GPS定位">
          {{ imageInfo.metadata.gpsLatitude ? `${imageInfo.metadata.gpsLatitude}, ${imageInfo.metadata.gpsLongitude}` : '无定位信息' }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-else class="no-exif">暂无 EXIF 信息</div>
    </div>

    <el-dialog v-model="showCrop" title="图片裁剪编辑器" width="60%">
      <div class="crop-placeholder">
        <el-icon :size="50" color="#909399"><Crop /></el-icon>
        <p>裁剪功能开发中...</p>
        <p class="sub-text">此处需集成 Cropper.js 并调用后端 /api/images/{id}/crop 接口</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCrop = false">取消</el-button>
          <el-button type="primary" @click="showCrop = false">确认裁剪</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Crop, MagicStick, Delete } from '@element-plus/icons-vue'

// 路由与状态定义
const route = useRoute()
const router = useRouter()
const imageId = route.params.id // 获取 URL 中的 id
const imageInfo = ref(null)
const loading = ref(false)

// 交互状态控制
const showCrop = ref(false) // 控制裁剪弹窗
const tags = ref(['风景', '高清', '壁纸']) // 默认标签模拟数据
const inputVisible = ref(false) // 控制标签输入框显示
const inputValue = ref('')
const InputRef = ref(null)

// 获取详情数据
const fetchDetail = async () => {
  loading.value = true
  try {
    // 调用GetImageDetail接口
    // const res = await request.get(`/images/${imageId}`)
    
    // 因为后端暂时只写了list接口，先拉取列表再过滤
    const res = await request.get('/images/list', { params: { page: 1, size: 100 } })
    const found = res.records.find(item => item.imageId.toString() === imageId.toString())
    
    if (found) {
      imageInfo.value = found
      if (found.tags && found.tags.length > 0) {
        tags.value = found.tags
      } else {
        tags.value = [] 
      }
    } else {
      ElMessage.error('未找到该图片信息')
      router.push('/gallery')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 删除图片
const deleteImage = () => {
  ElMessageBox.confirm(
    '确定要永久删除这张图片吗? 删除后无法恢复。',
    '警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
      // 调用后端删除接口（需后端实现 DeleteMapping）
      // await request.delete(`/images/${imageId}`)
      ElMessage.success('删除成功')
      router.push('/gallery')
    })
    .catch(() => {})
}

// AI 标签生成
const generateAiTags = () => {
  ElMessage.info('正在调用 AI 模型分析图片内容...')
  // 模拟异步请求
  setTimeout(() => {
    const newTags = ['AI:山脉', 'AI:自然', 'AI:蓝天']
    tags.value.push(...newTags)
    ElMessage.success(`AI 分析完成，添加了 ${newTags.length} 个标签`)
  }, 1500)
}

// 标签管理逻辑
const removeTag = (index) => {
  tags.value.splice(index, 1)
}

const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    InputRef.value.input.focus()
  })
}

const handleInputConfirm = () => {
  if (inputValue.value) {
    tags.value.push(inputValue.value)
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 工具函数
const getImageUrl = (path, isThumbnail = true) => {
  if (!path) return ''
  
  // 获取纯文件名 (例如从 D:\data\images\thumbs\abc.jpg 提取 abc.jpg)
  const filename = path.replace(/\\/g, '/').split('/').pop()
  
  // 根据是缩略图还是原图，拼接不同的 URL 前缀
  // 注意：这里的 /uploads/ 对应 vite.config.js 的代理，
  // thumb/ 和 original/ 对应后端 WebConfig 的映射
  if (isThumbnail) {
    return `/uploads/thumb/${filename}`
  } else {
    return `/uploads/original/${filename}`
  }
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatTime = (timeStr) => {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

// 初始化
onMounted(() => {
  if (imageId) {
    fetchDetail()
  }
})
</script>

<style scoped>
.detail-page {
  display: flex;
  gap: 20px;
  height: calc(100vh - 100px); /* 减去顶部 header/padding 的高度 */
  overflow: hidden;
}

/* 左侧样式 */
.left-panel {
  flex: 1;
  background: #1a1a1a; /* 深色背景突出图片 */
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-preview img {
  max-width: 95%;
  max-height: 85%;
  object-fit: contain; /* 保持比例 */
  box-shadow: 0 0 20px rgba(0,0,0,0.5);
}

.edit-toolbar {
  position: absolute;
  bottom: 30px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  transition: transform 0.3s;
}

.edit-toolbar:hover {
  transform: scale(1.05);
}

/* 右侧样式 */
.right-panel {
  width: 360px;
  background: #fff;
  padding: 24px;
  border-radius: 8px;
  overflow-y: auto; /* 内容过多可滚动 */
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.05);
}

.section-block {
  margin: 24px 0;
}

.section-header h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 16px;
  border-left: 4px solid #409EFF;
  padding-left: 8px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.input-new-tag {
  width: 90px;
}

.mt-20 {
  margin-top: 20px;
}

.no-exif {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
  background: #f5f7fa;
  border-radius: 4px;
}

.crop-placeholder {
  text-align: center;
  padding: 40px 0;
}
.sub-text {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .detail-page {
    flex-direction: column;
    height: auto;
    overflow-y: auto;
  }
  .left-panel {
    height: 300px;
  }
  .right-panel {
    width: 100%;
  }
}
</style>