<template>
  <div class="gallery-page">
    <div class="toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索图片... (支持自然语言)" class="search-input" clearable @clear="fetchImages">
        <template #append><el-button :icon="Search" @click="handleSearch" /></template>
      </el-input>
      <div class="actions">
        <el-button type="primary" :icon="Upload" @click="$router.push('/upload')">上传</el-button>
      </div>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="img in imageList" :key="img.imageId" class="img-col">
        <el-card :body-style="{ padding: '0px' }" shadow="hover" class="img-card" @click="goDetail(img.imageId)">
          <div class="image-wrapper">
             <img :src="getImageUrl(img.thumbnailPath)" class="image" loading="lazy"/>
          </div>
          <div class="info">
            <div class="filename">{{ img.originalFilename }}</div>
            <div class="meta">{{ formatSize(img.fileSize) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && imageList.length === 0" description="暂无图片" />
    
    <div class="pagination">
        <el-pagination layout="prev, pager, next" :total="total" @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import { Search, Upload } from '@element-plus/icons-vue'

const router = useRouter()
const imageList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const total = ref(0)
const page = ref(1)

// 获取图片列表
const fetchImages = async () => {
  loading.value = true
  try {
    const res = await request.get('/images/list', {
      params: { page: page.value, size: 12, keyword: searchKeyword.value }
    })
    imageList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 搜索 (如果是复杂搜索，这里可以调用 MCP 接口)
const handleSearch = () => {
  page.value = 1
  fetchImages()
}

// 辅助函数
const getImageUrl = (path, isThumbnail = true) => {
  if (!path) return ''
  
  // 1. 获取纯文件名 (例如从 D:\data\images\thumbs\abc.jpg 提取 abc.jpg)
  const filename = path.replace(/\\/g, '/').split('/').pop()
  
  // 2. 根据是缩略图还是原图，拼接不同的 URL 前缀
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
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const goDetail = (id) => router.push(`/detail/${id}`)
const handlePageChange = (val) => { page.value = val; fetchImages() }

onMounted(fetchImages)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; }
.search-input { max-width: 400px; }
.img-col { margin-bottom: 20px; }
.img-card { cursor: pointer; transition: transform 0.2s; border-radius: 8px; overflow: hidden; }
.img-card:hover { transform: translateY(-5px); }
.image-wrapper { height: 150px; overflow: hidden; background: #f0f0f0; display: flex; align-items: center; justify-content: center; }
.image { width: 100%; height: 100%; object-fit: cover; }
.info { padding: 10px; }
.filename { font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #333; }
.meta { font-size: 12px; color: #999; margin-top: 5px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>