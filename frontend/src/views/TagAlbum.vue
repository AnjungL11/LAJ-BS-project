<template>
  <div class="tag-album-page" v-loading="loading">
    <div class="album-header">
      <div class="header-left">
        <el-button link @click="goBack" class="back-btn">
          <el-icon :size="20"><ArrowLeft /></el-icon> 返回标签列表
        </el-button>
        <div class="album-info" v-if="tagInfo">
          <h2 class="album-title">{{ tagInfo.tagName }}</h2>
          <span class="album-count">{{ imageList.length }} 张照片</span>
        </div>
      </div>
      <div class="header-right">
        </div>
    </div>

    <div class="album-grid" v-if="imageList.length > 0">
      <div 
        class="photo-item" 
        v-for="img in imageList" 
        :key="img.imageId"
        @click="goToDetail(img.imageId)"
      >
        <div class="img-wrapper">
          <img :src="getImageUrl(img.thumbnailPath)" loading="lazy" />
        </div>
        <div class="img-name">{{ img.originalFilename }}</div>
      </div>
    </div>

    <el-empty v-else description="该标签下暂无图片" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const tagId = route.params.id

const loading = ref(false)
const tagInfo = ref(null)
const imageList = ref([])

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    // 1. 获取标签详情（为了显示标题）
    // 注意：这里需要后端 getTag 接口返回的是对象，前端做了字段映射处理的话要注意字段名
    const tagRes = await request.get(`/tags/${tagId}`)
    tagInfo.value = tagRes.data || tagRes // 兼容处理

    // 2. 获取图片列表
    const imgRes = await request.get(`/tags/${tagId}/images`)
    imageList.value = Array.isArray(imgRes) ? imgRes : (imgRes.data || [])
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 辅助函数：拼图片路径
const getImageUrl = (path) => {
  if (!path) return ''
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return `/uploads/thumb/${filename}`
}

// 核心跳转逻辑
const goBack = () => {
  router.push('/tags') // 强制回退到标签管理页
}

const goToDetail = (imgId) => {
  // 跳转到详情页
  router.push(`/detail/${imgId}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.tag-album-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.album-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.back-btn {
  font-size: 16px;
  color: #606266;
}
.back-btn:hover { color: #409EFF; }

.album-title { margin: 0; font-size: 24px; color: #303133; }
.album-count { color: #909399; margin-left: 10px; font-size: 14px; margin-top: 8px;}

/* 网格布局 */
.album-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
  overflow-y: auto;
}

.photo-item {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s;
}

.photo-item:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.img-wrapper {
  aspect-ratio: 1 / 1; /* 正方形缩略图 */
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.img-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.img-name {
  padding: 8px;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: center;
}
</style>