<template>
  <div class="detail-page" v-loading="loading">
    <div class="left-panel">
      <div class="image-preview">
        <img v-if="imageInfo" :src="getImageUrl(imageInfo.storagePath, false)" alt="preview" class="main-img"/>
        <el-empty v-else description="图片加载失败" />
      </div>
      <div class="edit-toolbar">
         <el-button-group>
           <el-button type="primary" plain :icon="Crop" @click="openEditDialog">编辑</el-button>
           <el-button type="success" plain :icon="Download" @click="handleDownload">下载</el-button>
           <el-button type="danger" plain :icon="Delete" @click="deleteImage">删除</el-button>
         </el-button-group>
      </div>
    </div>
    
    <div class="right-panel">
      
      <div class="section-block">
        <h3 class="title">图片基本信息</h3>
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="文件名">
            <div style="display: flex; align-items: center; justify-content: space-between;">
                <span :title="imageInfo?.originalFilename" style="margin-right: 10px; word-break: break-all;">
                    {{ imageInfo?.originalFilename || '-' }}
                </span>
                <el-button link type="primary" :icon="Edit" @click="handleRename"></el-button>
            </div>
          </el-descriptions-item>
          
          <el-descriptions-item label="文件大小">
            {{ formatSize(imageInfo?.fileSize || 0) }}
          </el-descriptions-item>
          
          <el-descriptions-item label="上传时间">
            {{ formatTime(imageInfo?.uploadedAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="section-block" style="margin-top: 20px;">
        <div class="section-header" style="display: flex; justify-content: space-between; align-items: center;">
            <h4>图片标签</h4>
            <el-button 
              type="primary" 
              link 
              :icon="MagicStick" 
              :loading="analyzing" 
              @click="handleAIAnalyze"
            >
              AI 智能分析
            </el-button>
         </div>

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
        
        <div class="crop-area" :style="{ '--filter-val': filterString }">

          <img 
            ref="cropImgRef" 
            :src="getImageUrl(imageInfo?.storagePath, false)" 
            alt="Source Image" 
            style="max-width: 100%; display: block;"
          />
        </div>
        
        <div class="adjust-panel">
            <div class="panel-title">编辑控制</div>
            
            <div class="control-group">
                <span class="label">旋转</span>
                <div class="rotate-buttons">
                    <el-button-group>
                        <el-button :icon="RefreshLeft" @click="rotateLeft">向左 90°</el-button>
                        <el-button :icon="RefreshRight" @click="rotateRight">向右 90°</el-button>
                    </el-button-group>
                </div>
            </div>

            <el-divider /> <div class="panel-title" style="margin-top: 10px;">色彩调整</div>
            
            <div class="slider-group">
                <span class="label">亮度</span>
                <el-slider v-model="editParams.brightness" :min="50" :max="150" :format-tooltip="val => val + '%'" />
            </div>
            <div class="slider-group">
                <span class="label">对比度</span>
                <el-slider v-model="editParams.contrast" :min="50" :max="150" :format-tooltip="val => val + '%'" />
            </div>
            <div class="slider-group">
                <span class="label">饱和度</span>
                <el-slider v-model="editParams.saturation" :min="0" :max="200" :format-tooltip="val => val + '%'" />
            </div>
            <div class="slider-group">
                <span class="label">
                    色温
                    <el-tooltip content="左冷右暖 (-50 ~ 50)" placement="top">
                        <el-icon><InfoFilled /></el-icon>
                    </el-tooltip>
                </span>
                <el-slider 
                    v-model="editParams.temperature" 
                    :min="-50" 
                    :max="50" 
                    :marks="{0: '0', '-50': '冷', '50': '暖'}"
                />
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
                    <li>点击“下一步”选择保存图片的方式</li>
                </ul>
            </div>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCrop = false" :disabled="editLoading">取消</el-button>
          <el-button type="primary" @click="openSaveOptions" :loading="editLoading">下一步</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="saveOptionVisible"
      title="保存选项"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="选择保存方式">
          <el-radio-group v-model="saveType">
            <el-radio label="overwrite">覆盖原图</el-radio>
            <el-radio label="saveAs">另存为新图片</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="saveType === 'saveAs'" label="新文件名">
          <el-input 
            v-model="newFilename" 
            placeholder="请输入文件名" 
            clearable
          >
            <template #append>.jpg</template>
          </el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="saveOptionVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmSaveExecution" :loading="editLoading">确定保存</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="downloadDialogVisible"
      title="下载图片"
      width="400px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="请输入保存的文件名">
          <el-input 
            v-model="downloadFilename" 
            placeholder="文件名" 
            clearable
            @keyup.enter="confirmDownload"
          >
            <template #append>.jpg</template>
          </el-input>
        </el-form-item>
        <div class="tips-text">
            <el-icon><InfoFilled /></el-icon> 
            提示：点击确定后，浏览器将弹出保存窗口供您选择存储位置。
        </div>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="downloadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmDownload" :loading="downloading">
            确定下载
          </el-button>
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
import { Crop, MagicStick, Delete, InfoFilled, RefreshLeft, RefreshRight, Download, Edit } from '@element-plus/icons-vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

// 路由与状态定义
const route = useRoute()
const router = useRouter()
const imageId = route.params.id // 获取URL中的id
const imageInfo = ref(null)
const loading = ref(false)
const downloadDialogVisible = ref(false)
const downloadFilename = ref('')
const downloading = ref(false)
const analyzing = ref(false)

// 编辑器相关状态
const showCrop = ref(false)
const editLoading = ref(false)
const cropImgRef = ref(null)
const cropperInstance = ref(null)
const saveOptionVisible = ref(false)
const saveType = ref('overwrite')
const newFilename = ref('')

// 打开保存选项弹窗
const openSaveOptions = () => {
    saveOptionVisible.value = true
    saveType.value = 'overwrite' // 重置默认值
    // 默认新文件名为原文件名_edited
    const originalName = imageInfo.value.originalFilename.replace(/\.\w+$/, '')
    newFilename.value = `${originalName}_edited`
}

// 确定保存
const confirmSaveExecution = () => {
    // 简单校验
    if (saveType.value === 'saveAs' && !newFilename.value.trim()) {
        return ElMessage.warning('请输入新的文件名')
    }
    // 关闭选项弹窗，开始Loading
    saveOptionVisible.value = false
    // 调用核心处理函数
    handleSaveEdit()
}

// 调色参数
const editParams = reactive({
    brightness: 100,
    contrast: 100,
    saturation: 100,
    temperature: 0 // 默认为0，负数偏冷，正数偏暖
})

// 统一的滤镜字符串生成函数,确保预览和保存使用完全相同的算法来源
const generateSharedFilterString = () => {
    // 基础参数
    const b = editParams.brightness / 100
    const c = editParams.contrast / 100
    const s = editParams.saturation / 100
    // 构建基础滤镜串
    let str = `brightness(${b}) contrast(${c}) saturate(${s})`
    // 色温处理逻辑
    const temp = editParams.temperature
    if (temp > 0) {
        // 暖色调
        const sepiaVal = temp / 2 / 100 // 范围0.0-0.25
        // 饱和度补偿系数
        const saturateComp = (100 + temp / 2) / 100 
        
        str += ` sepia(${sepiaVal}) hue-rotate(-30deg) saturate(${saturateComp})`
    } else if (temp < 0) {
        // 冷色调
        const sepiaVal = Math.abs(temp) / 2 / 100 // 范围0.0-0.25
        
        str += ` sepia(${sepiaVal}) hue-rotate(180deg)`
    }
    
    return str
}

// 返回滤镜字符串供CSS变量使用
// CSS预览逻辑
// 使用sepia+hue-rotate进行近似模拟
const filterString = computed(() => {
    // 直接调用共享函数，保证与保存逻辑一致
    return generateSharedFilterString()
})

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

// 旋转图片
const rotateLeft = () => {
    if (cropperInstance.value) {
        cropperInstance.value.rotate(-90) // 逆时针旋转90度
    }
}

const rotateRight = () => {
    if (cropperInstance.value) {
        cropperInstance.value.rotate(90) // 顺时针旋转90度
    }
}

const resetParams = () => {
    editParams.brightness = 100
    editParams.contrast = 100
    editParams.saturation = 100
    editParams.temperature = 0
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

    // 调用同一个共享函数获取滤镜字符串
    const finalFilterStr = generateSharedFilterString()

    // 设置Canvas滤镜
    // 浏览器Canvas引擎将使用这个字符串进行像素计算
    ctx.filter = finalFilterStr

    ctx.drawImage(sourceCanvas, 0, 0, width, height)

    return filterCanvas
}

// 保存编辑
const handleSaveEdit = () => {
    if (!cropperInstance.value) return
    editLoading.value = true

    // 获取Cropper裁剪后的纯净Canvas
    // 设置maxWidth、maxHeight防止生成过大的图片导致浏览器崩溃
    const rawCroppedCanvas = cropperInstance.value.getCroppedCanvas({
        maxWidth: 4096,
        maxHeight: 4096,
        imageSmoothingEnabled: true,
        imageSmoothingQuality: 'high',
    })
    if (!rawCroppedCanvas) {
        editLoading.value = false
        return ElMessage.error('无法获取裁剪图像')
    }
    // 调用辅助函数，把新参数加到新的Canvas上，得到最终带有裁剪和调色效果的Canvas
    const finalCanvas = applyFilterToCanvas(rawCroppedCanvas)
    // 使用最终的finalCanvas导出为Blob文件
    finalCanvas.toBlob(async (blob) => {
        if (!blob) {
            editLoading.value = false
            return ElMessage.error('图像处理失败')
        }
        // 构造FormData上传
        const formData = new FormData()
        try {
            if (saveType.value === 'overwrite') {
                // 覆盖原图
                const filename = imageInfo.value.originalFilename.replace(/\.\w+$/, '.jpg')
                formData.append('file', blob, filename)
                await request.post(`/images/${imageId}/content`, formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                })
                ElMessage.success('覆盖保存成功')
                // 关闭编辑器
                showCrop.value = false
                // 刷新当前详情页
                fetchDetail()
            } else {
                // 另存为新图
                // 构造新文件名
                const finalName = `${newFilename.value}.jpg`
                formData.append('file', blob, finalName)
                // 调用上传接口
                const res = await request.post('/images/upload', formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                })
                ElMessage.success('另存成功')
                // 关闭编辑器
                showCrop.value = false
                // 询问用户是否跳转到新图片
                // res应该是后端upload接口返回的新图片ID或对象
                // 假设后端upload接口返回的是 "上传成功" 字符串，那就没法跳转；
                // 如果后端返回了新图片的ID，我们可以跳转
                ElMessageBox.confirm('图片已另存，是否返回图库查看？', '提示', {
                    confirmButtonText: '返回图库',
                    cancelButtonText: '留在本页'
                }).then(() => {
                    router.push('/gallery')
                }).catch(() => {
                    // 留在本页
                })
            }
        } catch (error) {
            console.error("保存失败", error)
        } finally {
            editLoading.value = false
            finalCanvas.remove()
            rawCroppedCanvas.remove()
        }
    }, 'image/jpeg', 0.95) // 指定输出格式为JPEG，质量0.95
}

// 重命名逻辑
const handleRename = () => {
    if (!imageInfo.value) return
    ElMessageBox.prompt('请输入新的文件名', '重命名', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: imageInfo.value.originalFilename,
        // 禁止空字符
        inputPattern: /\S/,
        inputErrorMessage: '文件名不能为空'
    }).then(async ({ value }) => {
        try {
            // 调用后端重命名接口
            await request.post(`/images/${imageId}/rename`, null, {
                params: { newName: value }
            })
            ElMessage.success('重命名成功')
            // 更新前端显示
            imageInfo.value.originalFilename = value
        } catch (error) {
            console.error(error)
        }
    }).catch(() => {
        // 取消输入不做处理
    })
}

// 点击下载打开弹窗
const handleDownload = () => {
    downloadDialogVisible.value = true
    // 默认填入当前文件名
    downloadFilename.value = imageInfo.value.originalFilename.replace(/\.\w+$/, '')
}

// 确定下载
const confirmDownload = async () => {
    if (!downloadFilename.value.trim()) {
        return ElMessage.warning('请输入文件名')
    }
    downloading.value = true
    try {
        // 从后端请求文件流
        const res = await request.get(`/images/${imageId}/download`, {
            responseType: 'blob' 
        })
        // const blob = new Blob([res])
        const blob = new Blob([res], { type: 'image/jpeg' })
        const finalName = `${downloadFilename.value}.jpg`
        // 关闭弹窗
        downloadDialogVisible.value = false
        // 调用“另存为”窗口
        if (window.showSaveFilePicker) {
            try {
                const handle = await window.showSaveFilePicker({
                    suggestedName: finalName,
                    types: [{
                        description: 'JPEG Image',
                        accept: { 'image/jpeg': ['.jpg'] },
                    }],
                })
                // 用户选好位置后，写入文件
                const writable = await handle.createWritable()
                await writable.write(blob)
                await writable.close()
                ElMessage.success('保存成功')
            } catch (err) {
                // 如果用户在弹窗里点击了“取消”，会抛出AbortError
                if (err.name !== 'AbortError') {
                    console.error(err)
                    // 如果API调用失败，降级到传统方法
                    fallbackDownload(blob, finalName)
                }
            }
        } else {
            // 简单下载，取决于用户浏览器选择
            fallbackDownload(blob, finalName)
        }
    } catch (error) {
        console.error('下载出错', error)
        ElMessage.error('下载失败')
    } finally {
        downloading.value = false
    }
}

// 简化下载方式
const fallbackDownload = (blob, filename) => {
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
    ElMessage.success('已触发下载')
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

// AI标签生成
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

// AI分析处理函数
const handleAIAnalyze = async () => {
  analyzing.value = true
  try {
    // 调用后端接口
    const res = await request.post(`/images/${route.params.id}/analyze`)
    // 获取后端返回的原始标签列表
    const fetchedTags = (res && Array.isArray(res.data)) ? res.data : (Array.isArray(res) ? res : [])

    if (fetchedTags.length > 0) {
      // 计算实际新增的标签
      let addedCount = 0
      fetchedTags.forEach(tag => {
          // 只有当当前列表里没有这个标签时才添加并计数
          if (!tags.value.includes(tag)) {
              tags.value.push(tag)
              addedCount++
          }
      })
      // 根据实际新增的数量显示提示
      if (addedCount > 0) {
          ElMessage.success(`识别成功，新增 ${addedCount} 个标签`)
          // 同步更新imageInfo，防止页面切换丢失
          if (imageInfo.value) {
              imageInfo.value.tags = [...tags.value] 
          }
      } else {
          // 如果后端返回标签但都已经有了
          ElMessage.info('AI 识别结果已存在，未添加重复标签')
      }
    } else {
      ElMessage.warning('未能识别出新标签')
    }
  } catch (error) {
    console.error("AI分析出错:", error)
    ElMessage.error('AI 分析服务响应异常')
  } finally {
    analyzing.value = false
  }
}

// 文件大小格式化
const formatSize = (size) => {
    if (!size) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(size) / Math.log(k))
    return (size / Math.pow(k, i)).toPrecision(3) + ' ' + sizes[i]
}

// 时间格式化
const formatTime = (timeStr) => {
    if (!timeStr) return '-'
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
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

.tips-text {
    font-size: 12px;
    color: #909399;
    margin-top: -10px;
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 4px;
}

/* 穿透Cropper的DOM结构找到所有内部图片 */
/* .cropper-view-box img：裁剪框内的高亮图 */
/* .cropper-canvas img：背景里的暗色图 */
.crop-area :deep(.cropper-view-box img),
.crop-area :deep(.cropper-canvas img) {
    /* 使用在Template里绑定的CSS变量 */
    filter: var(--filter-val) !important;
    
    /* 添加过渡效果，让滑块拖动时变化更丝滑 */
    transition: filter 0.1s linear;
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
    position: relative; /* 确保 CSS 变量作用域正确 */
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
    overflow-y: auto;   /* 内容过多时显示垂直滚动条 */
    overflow-x: hidden; /* 隐藏水平滚动条 */
}

.adjust-panel::-webkit-scrollbar {
    width: 6px;
}
.adjust-panel::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
}
.adjust-panel::-webkit-scrollbar-track {
    background: transparent;
}

.panel-title {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 15px;
    color: #333;
}

/* 旋转按钮区样式 */
.control-group {
    margin-bottom: 15px;
}
.control-group .label {
    display: block;
    font-size: 14px;
    color: #666;
    margin-bottom: 8px;
}
.rotate-buttons {
    display: flex;
    justify-content: space-between;
}
/* 让按钮组占满宽度 */
.rotate-buttons .el-button-group {
    display: flex;
    width: 100%;
}
.rotate-buttons .el-button {
    flex: 1; /* 两个按钮平分宽度 */
}

.slider-group {
    margin-bottom: 35px;
}

:deep(.el-slider__marks-text) {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
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

/* 移动端适配 */
@media (max-width: 768px) {
  /* 详情页整体布局改为垂直排列，高度自动 */
  .detail-page {
    flex-direction: column; /* 上下排列 */
    height: auto !important; /* 取消固定高度，允许滚动 */
    gap: 10px;
    padding-bottom: 20px;
  }

  /* 左侧图片预览区高度固定，宽度100% */
  .left-panel {
    width: 100%;
    height: 50vh; /* 占据屏幕一半高度 */
    flex: none;   /* 禁止自动伸缩 */
  }
  
  .main-img {
    max-height: 90%; /* 防止图片撑破 */
  }

  /* 工具栏移动到图片下方，按钮变小 */
  .edit-toolbar {
    width: 90%;
    bottom: 10px;
    padding: 5px 10px;
  }
  
  /* 强制按钮图标和文字变小 */
  .edit-toolbar .el-button {
    padding: 5px 8px;
    font-size: 12px;
  }

  /* 右侧信息区宽度100%，取消滚动限制 */
  .right-panel {
    width: 100%;
    height: auto;
    box-shadow: none;
    padding: 15px;
  }

  /* 编辑器弹窗适配 */
  /* 编辑器容器改为垂直排列 */
  .editor-container {
    flex-direction: column;
    height: 70vh; /* 限制总高度 */
  }

  /* 裁剪区域 */
  .crop-area {
    width: 100%;
    flex: 2; /* 裁剪区占2/3空间 */
    min-height: 200px;
  }

  /* 调节面板 */
  .adjust-panel {
    width: 100%;
    flex: 1; /* 调节区占1/3空间 */
    border-left: none;
    border-top: 1px solid #eee;
    padding: 10px;
    overflow-y: auto; /* 允许内部滚动 */
  }
  
  /* 调整滑块的间距，使其更紧凑 */
  .slider-group {
    margin-bottom: 15px;
  }
  
  /* 旋转按钮组 */
  .rotate-buttons .el-button {
    padding: 8px;
  }
}

/* 全局弹窗适配 */
/* 强制所有Dialog在手机端宽度为95% */
:deep(.el-dialog) {
  width: 95% !important;
  max-width: 95% !important;
  margin-top: 5vh !important; /* 稍微靠上 */
}

/* 调整Dialog内容区域边距 */
:deep(.el-dialog__body) {
  padding: 15px !important;
}
</style>