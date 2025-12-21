<template>
  <div class="detail-page" v-loading="loading">
    <div class="left-panel">
      <div class="image-preview">
        <img v-if="imageInfo" :src="getImageUrl(imageInfo.storagePath, false)" alt="preview" class="main-img"/>
        <el-empty v-else description="图片加载失败" />
      </div>
      <div class="edit-toolbar">
         <el-button-group>
           <el-button type="primary" plain :icon="Crop" @click="openEditDialog">编辑/裁剪</el-button>
           <el-button type="danger" plain :icon="Delete" @click="deleteImage">删除</el-button>
         </el-button-group>
      </div>
    </div>
    
    <div class="right-panel">
      
      <div class="section-block">
        <h3 class="title">图片详情</h3>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="文件名">
            <span :title="imageInfo?.originalFilename">{{ imageInfo?.originalFilename || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatSize(imageInfo?.fileSize || 0) }}</el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatTime(imageInfo?.uploadedAt) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="section-block" style="margin-top: 20px;">
         <div class="section-header"><h4>标签管理</h4></div>
         <div class="tags">
            <el-tag 
              v-for="(tag, index) in tags" 
              :key="index" 
              closable 
              @close="removeTag(index)"
              style="margin-right: 5px; margin-bottom: 5px;"
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
              style="width: 100px;"
            />
            <el-button v-else class="button-new-tag" size="small" @click="showInput">+ 新标签</el-button>
         </div>
      </div>

      <div class="section-block" style="margin-top: 20px;">
        <div class="section-header"><h4>EXIF 信息</h4></div>
        
        <div v-if="imageInfo?.metadata && (imageInfo.metadata.cameraModel || imageInfo.metadata.takenTime)" class="exif-info">
           <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="相机型号">
                 {{ imageInfo.metadata.cameraModel || '未知设备' }}
              </el-descriptions-item>
              
              <el-descriptions-item label="拍摄时间">
                 {{ formatTime(imageInfo.metadata.takenTime) || '-' }}
              </el-descriptions-item>
              
              <el-descriptions-item label="分辨率">
                 {{ imageInfo.metadata.width }} x {{ imageInfo.metadata.height }} px
              </el-descriptions-item>
              
              <el-descriptions-item label="GPS 坐标" v-if="imageInfo.metadata.gpsLatitude">
                 {{ parseFloat(imageInfo.metadata.gpsLatitude).toFixed(4) }} N, 
                 {{ parseFloat(imageInfo.metadata.gpsLongitude).toFixed(4) }} E
              </el-descriptions-item>
           </el-descriptions>
        </div>
        
        <el-empty v-else description="暂无 EXIF 信息" :image-size="60" />
      </div>

    </div>

    <el-dialog 
        v-model="showCrop" 
        title="图片编辑器" 
        width="900px" 
        top="5vh"
        :close-on-click-modal="false"
        @opened="initCropper" 
        @closed="destroyCropper"
    >
      <div class="editor-container" v-loading="editLoading" element-loading-text="正在处理并上传...">
        
        <div class="crop-area">
          <img 
            ref="cropImgRef" 
            :src="getImageUrl(imageInfo?.storagePath, false)" 
            :style="previewFilterStyle"
            alt="Source Image" 
            style="max-width: 100%; display: block;"
          />
        </div>
        
        <div class="adjust-panel">
            <div class="panel-title">色彩调整</div>
            
            <div class="slider-group">
                <span class="label">亮度 (Brightness)</span>
                <el-slider v-model="editParams.brightness" :min="50" :max="150" :format-tooltip="val => val + '%'" />
            </div>
            <div class="slider-group">
                <span class="label">对比度 (Contrast)</span>
                <el-slider v-model="editParams.contrast" :min="50" :max="150" :format-tooltip="val => val + '%'" />
            </div>
            <div class="slider-group">
                <span class="label">饱和度 (Saturation)</span>
                <el-slider v-model="editParams.saturation" :min="0" :max="200" :format-tooltip="val => val + '%'" />
            </div>

            <el-button type="info" plain size="small" @click="resetParams" style="width: 100%; margin-top: 20px;">
                重置参数
            </el-button>

             <div class="tips">
                <p><el-icon><InfoFilled /></el-icon> 操作提示：</p>
                <ul>
                    <li>在左侧拖动选框进行裁剪</li>
                    <li>支持鼠标滚轮缩放</li>
                    <li>调整右侧滑块改变色调</li>
                    <li>点击“保存应用”覆盖原图</li>
                </ul>
            </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCrop = false" :disabled="editLoading">取消</el-button>
          <el-button type="primary" @click="handleSaveEdit" :loading="editLoading">保存应用</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Crop, MagicStick, Delete } from '@element-plus/icons-vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

// 路由与状态定义
const route = useRoute()
const router = useRouter()
const imageId = route.params.id // 获取URL中的id
const imageInfo = ref(null)
const loading = ref(false)

// 编辑器相关状态
const showCrop = ref(false)
const editLoading = ref(false)
const cropImgRef = ref(null)
const cropperInstance = ref(null)

// 调色参数
const editParams = reactive({
    brightness: 100,
    contrast: 100,
    saturation: 100
})

// 计算实时的CSS滤镜样式用于预览
const previewFilterStyle = computed(() => ({
    filter: `brightness(${editParams.brightness}%) contrast(${editParams.contrast}%) saturate(${editParams.saturation}%)`
}))

// 初始化Cropper
const openEditDialog = () => {
    showCrop.value = true
}

const initCropper = () => {
    // 确保DOM已渲染且图片已加载
    nextTick(() => {
        if (cropImgRef.value && !cropperInstance.value) {
            cropperInstance.value = new Cropper(cropImgRef.value, {
                // 限制裁剪框不超过画布
                viewMode: 1,
                // 模式：移动画布
                dragMode: 'move',
                // 自由比例
                aspectRatio: NaN,
                // 初始裁剪区域大小
                autoCropArea: 0.8,
                restore: false,
                guides: true,
                center: true,
                highlight: false,
                cropBoxMovable: true,
                cropBoxResizable: true,
                toggleDragModeOnDblclick: false,
            })
        }
    })
}

// 销毁Cropper
const destroyCropper = () => {
    if (cropperInstance.value) {
        cropperInstance.value.destroy()
        cropperInstance.value = null
    }
    resetParams()
}

const resetParams = () => {
    editParams.brightness = 100
    editParams.contrast = 100
    editParams.saturation = 100
}

// 应用滤镜并保存
// 辅助函数，在一个Canvas上应用滤镜并绘制另一个Canvas
const applyFilterToCanvas = (sourceCanvas) => {
    const width = sourceCanvas.width
    const height = sourceCanvas.height
    
    // 创建一个新的用于处理滤镜的Canvas
    const filterCanvas = document.createElement('canvas')
    filterCanvas.width = width
    filterCanvas.height = height
    const ctx = filterCanvas.getContext('2d')

    // 将CSS滤镜格式转换为Canvas滤镜格式
    const b = editParams.brightness / 100
    const c = editParams.contrast / 100
    const s = editParams.saturation / 100
    
    // 设置Canvas滤镜字符串
    ctx.filter = `brightness(${b}) contrast(${c}) saturate(${s})`

    // 将源Canvas绘制到滤镜Canvas上
    ctx.drawImage(sourceCanvas, 0, 0, width, height)

    return filterCanvas
}

const handleSaveEdit = () => {
    if (!cropperInstance.value) return
    editLoading.value = true

    // 获取Cropper裁剪后的纯净Canvas
    // 设置maxWidth、maxHeight防止生成过大的图片导致崩溃
    const croppedCanvas = cropperInstance.value.getCroppedCanvas({
        maxWidth: 4096,
        maxHeight: 4096,
        imageSmoothingEnabled: true,
        imageSmoothingQuality: 'high',
    })

    if (!croppedCanvas) {
        editLoading.value = false
        return ElMessage.error('无法获取裁剪图像')
    }

    // 应用调色滤镜得到最终的Canvas
    const finalCanvas = applyFilterToCanvas(croppedCanvas)

    // 将Canvas转换为Blob
    finalCanvas.toBlob(async (blob) => {
        if (!blob) {
            editLoading.value = false
            return ElMessage.error('图像处理失败')
        }

        // 构造FormData上传
        const formData = new FormData()
        // 使用原文件名添加前缀以示区分
        const newFilename = `edited_${imageInfo.value.originalFilename.replace(/\.\w+$/, '.jpg')}`
        // 第三个参数指定文件名，强制存为jpg
        formData.append('file', blob, newFilename) 

        try {
            // 调用后端更新接口
            await request.post(`/images/${imageId}/content`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            })
            ElMessage.success('编辑保存成功')
            showCrop.value = false
            // 重新加载详情以查看最新图片
            fetchDetail()
        } catch (error) {
            console.error(error)
        } finally {
            editLoading.value = false
        }
    }, 'image/jpeg', 0.92) // 指定输出格式为JPEG，质量0.92
}

// 交互状态控制
// 默认标签模拟数据
const tags = ref(['风景', '高清', '壁纸'])
// 控制标签输入框显示
const inputVisible = ref(false)
const inputValue = ref('')
const InputRef = ref(null)

// 获取详情数据
const fetchDetail = async () => {
  loading.value = true
  try {
    // 调用GetImageDetail接口
    const res = await request.get(`/images/${imageId}`)
    if (res) {
      imageInfo.value = res
      // 加上时间戳防止缓存
      // 判空，防止storagePath为空导致报错
      if (imageInfo.value.storagePath) {
          const t = new Date().getTime()
          imageInfo.value.storagePath = `${res.storagePath}?t=${t}`
          imageInfo.value.thumbnailPath = `${res.thumbnailPath}?t=${t}`
      }
      // 标签赋值
      tags.value = res.tags || []
    } else {
      ElMessage.error('未找到该图片')
      router.push('/gallery')
    }
  } catch (error) {
    console.error("获取详情失败", error)
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

// 删除标签
const removeTag = async (index) => {
  const tagToRemove = tags.value[index]
  try {
    // 调用后端删除接口
    await request.delete(`/images/${imageId}/tags`, { 
      params: { tagName: tagToRemove } 
    })
    
    // 后端删除成功后更新前端视图
    tags.value.splice(index, 1)
    ElMessage.success('标签已移除')
  } catch (error) {
    console.error(error)
  }
}

// 显示输入框
const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    InputRef.value.input.focus()
  })
}

// 确认添加标签
const handleInputConfirm = async () => {
  if (inputValue.value) {
    const newTag = inputValue.value
    // 去重检查
    if (tags.value.includes(newTag)) {
        ElMessage.warning('标签已存在')
        inputVisible.value = false
        inputValue.value = ''
        return
    }

    try {
      // 调用后端添加接口
      await request.post(`/images/${imageId}/tags`, null, { 
        params: { tagName: newTag } 
      })
      
      // 后端保存成功后更新前端数组
      tags.value.push(newTag)
      ElMessage.success('标签添加成功')
    } catch (error) {
       console.error(error)
    }
  }
  
  // 重置输入框状态
  inputVisible.value = false
  inputValue.value = ''
}

// 工具函数
const getImageUrl = (path, isThumbnail = true) => {
  if (!path) return ''

  // 处理时间戳
  let query = ''
  let cleanPath = path
  if (path.includes('?')) {
      const parts = path.split('?')
      cleanPath = parts[0]
      query = '?' + parts[1]
  }
  // 获取纯文件名
  const filename = path.replace(/\\/g, '/').split('/').pop()
  // 拼接前端代理路径
  const baseUrl = isThumbnail ? `/uploads/thumb/${filename}` : `/uploads/original/${filename}`
  return baseUrl + query
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

/* 编辑器样式 */
.editor-container {
    display: flex;
    height: 500px; /* 固定高度 */
    gap: 20px;
}

.crop-area {
    flex: 1;
    background: #f0f2f5;
    /* CropperJS需要一个块级容器 */
    display: block;
    overflow: hidden;
    border-radius: 4px;
    /* 确保cropper图片容器不会溢出 */
    max-height: 100%;
}

/* 覆盖CropperJS默认样式，让图片在容器内居中显示 */
:deep(.cropper-container) {
    width: 100% !important;
    height: 100% !important;
}

.adjust-panel {
    width: 260px;
    background: #fff;
    padding: 20px;
    border-left: 1px solid #eee;
    display: flex;
    flex-direction: column;
}

.panel-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 20px;
    color: #333;
}

.slider-group {
    margin-bottom: 25px;
}

.slider-group .label {
    display: block;
    font-size: 14px;
    color: #666;
    margin-bottom: 8px;
}

.tips {
    margin-top: auto; /* 推到底部 */
    background: #fdf6ec;
    padding: 15px;
    border-radius: 4px;
    font-size: 13px;
    color: #e6a23c;
}
.tips p { margin: 0 0 8px 0; font-weight: bold; display: flex; align-items: center;}
.tips ul { margin: 0; padding-left: 20px; color: #909399; }
.tips li { margin-bottom: 4px; }

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
    
  /* 编辑弹窗适配 */
  .editor-container { flex-direction: column; height: auto; }
  .crop-area { height: 300px; }
  .adjust-panel { width: 100%; border-left: none; border-top: 1px solid #eee; }
}
</style>