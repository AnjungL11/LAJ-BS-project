<template>
  <div class="gallery-page">
    <div class="toolbar">
      <div class="search-group">
        <el-input 
          v-model="searchForm.filename" 
          placeholder="搜索文件名..." 
          class="search-input" 
          clearable 
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
        
        <el-button type="primary" plain :icon="Filter" @click="showFilter = true" style="margin-left: 10px;">
          高级筛选
        </el-button>
      </div>

      <div class="actions">
        <el-button 
          v-if="!isSelectionMode" 
          type="warning" 
          plain 
          icon="List" 
          @click="isSelectionMode = true"
        >
          批量选择
        </el-button>
        
        <template v-else>
          <el-button type="info" plain @click="exitSelectionMode">取消选择</el-button>
          <el-button 
            type="success" 
            icon="VideoPlay" 
            :disabled="selectedIds.size === 0"
            @click="openCarousel"
          >
            播放选中 ({{ selectedIds.size }})
          </el-button>
        </template>

        <el-button type="primary" :icon="Upload" @click="$router.push('/upload')" style="margin-left: 10px;">
          上传图片
        </el-button>
      </div>
    </div>

    <el-drawer v-model="showFilter" title="高级图片检索" size="300px">
      <el-form :model="searchForm" label-position="top">
        <el-form-item label="标签筛选">
          <el-select v-model="searchForm.tags" multiple filterable allow-create default-first-option placeholder="输入标签" style="width: 100%">
            <el-option label="风景" value="风景" /><el-option label="人物" value="人物" /><el-option v-for="y in 3" :key="y" :label="`${2022+y}年`" :value="`${2022+y}年`" />
          </el-select>
        </el-form-item>
        <el-form-item label="拍摄设备"><el-input v-model="searchForm.cameraModel" /></el-form-item>
        <el-form-item label="拍摄时间范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto"><el-button @click="resetFilter">重置</el-button><el-button type="primary" @click="handleSearch">搜索</el-button></div>
      </template>
    </el-drawer>

    <el-row :gutter="20" v-loading="loading">
      <el-col 
        :xs="12" :sm="8" :md="6" :lg="4" 
        v-for="img in imageList" 
        :key="img.imageId" 
        class="img-col"
      >
        <el-card 
          :body-style="{ padding: '0px' }" 
          shadow="hover" 
          class="img-card" 
          :class="{ 'is-selected': selectedIds.has(img.imageId) }"
          @click="handleCardClick(img)"
        >
          <div v-if="isSelectionMode" class="checkbox-overlay">
            <el-checkbox 
              :model-value="selectedIds.has(img.imageId)" 
              @change="(val) => toggleSelection(img.imageId, val)"
              @click.stop
            />
          </div>

          <div class="image-wrapper">
            <img :src="getImageUrl(img.thumbnailPath)" class="image" loading="lazy"/>
          </div>
          <div class="info">
            <div class="filename" :title="img.originalFilename">{{ img.originalFilename }}</div>
            <div class="meta-row">
              <span class="size">{{ formatSize(img.fileSize) }}</span>
              <el-tag v-if="img.tags && img.tags.length > 0" size="small" effect="plain" class="mini-tag">
                {{ img.tags[0] }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && imageList.length === 0" description="暂无符合条件的图片" />
    <div class="pagination" v-if="total > 0">
        <el-pagination layout="prev, pager, next" :total="total" :page-size="searchForm.size" :current-page="searchForm.page" @current-change="handlePageChange" />
    </div>

    <el-dialog 
      v-model="carouselVisible" 
      fullscreen 
      append-to-body 
      class="carousel-dialog"
    >
      <el-carousel :autoplay="false" arrow="always" height="100%" indicator-position="none">
        <el-carousel-item v-for="img in carouselList" :key="img.imageId">
          <div class="carousel-item-wrapper">
            <img :src="getImageUrl(img.storagePath, false)" class="carousel-image" />
            <div class="carousel-caption">
              <h3>{{ img.originalFilename }}</h3>
              <p>{{ img.tags ? img.tags.join(', ') : '' }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import { Search, Upload, Filter, VideoPlay, List } from '@element-plus/icons-vue' // 引入新图标

const router = useRouter()
const imageList = ref([])
const loading = ref(false)
const showFilter = ref(false)
const total = ref(0)
const dateRange = ref([])

// 状态
// 是否处于多选模式
const isSelectionMode = ref(false)
// 存储选中的图片ID
const selectedIds = ref(new Set())
// 轮播弹窗开关
const carouselVisible = ref(false)
// 轮播列表数据
const carouselList = ref([])

const searchForm = reactive({
  filename: '',
  tags: [],
  cameraModel: '',
  page: 1,
  size: 12
})

const fetchImages = async () => {
  loading.value = true
  try {
    const payload = {
      page: searchForm.page,
      size: searchForm.size,
      filename: searchForm.filename,
      tags: searchForm.tags,
      cameraModel: searchForm.cameraModel,
      startTime: dateRange.value && dateRange.value.length === 2 ? dateRange.value[0] : null,
      endTime: dateRange.value && dateRange.value.length === 2 ? dateRange.value[1] : null
    }
    const res = await request.post('/images/search', payload)
    imageList.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

// 交互逻辑
const handleSearch = () => {
  searchForm.page = 1
  showFilter.value = false
  fetchImages()
}

const resetFilter = () => {
  searchForm.tags = []
  searchForm.cameraModel = ''
  dateRange.value = []
}

const handlePageChange = (val) => { 
  searchForm.page = val
  fetchImages() 
}

// 卡片点击与多选逻辑

const handleCardClick = (img) => {
  if (isSelectionMode.value) {
    // 选图模式下，点击卡片 = 切换选中状态
    const id = img.imageId
    if (selectedIds.value.has(id)) {
      selectedIds.value.delete(id)
    } else {
      selectedIds.value.add(id)
    }
  } else {
    // 普通模式下点击卡片进入详情
    router.push(`/detail/${img.imageId}`)
  }
}

// 勾选框变更事件
const toggleSelection = (id, checked) => {
  if (checked) selectedIds.value.add(id)
  else selectedIds.value.delete(id)
}

// 退出选择模式
const exitSelectionMode = () => {
  isSelectionMode.value = false
  selectedIds.value.clear()
}

// 打开轮播
const openCarousel = () => {
  // 过滤出选中的图片对象
  const selected = imageList.value.filter(img => selectedIds.value.has(img.imageId))
  
  if (selected.length > 0) {
    carouselList.value = selected
    carouselVisible.value = true
  }
}

// 辅助函数
const getImageUrl = (path, isThumbnail = true) => {
  if (!path) return ''
  const filename = path.replace(/\\/g, '/').split('/').pop()
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

onMounted(fetchImages)
</script>

<style scoped>
/* 保持原有的样式 ... */
.gallery-page { padding-bottom: 20px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.search-group { display: flex; align-items: center; flex: 1; max-width: 600px; }
.search-input { flex: 1; }
.img-col { margin-bottom: 20px; }

/* 卡片样式增强 */
.img-card { 
  cursor: pointer; 
  position: relative; /* 为了定位勾选框 */
  transition: transform 0.2s, box-shadow 0.2s, border 0.2s; 
  border-radius: 8px; 
  overflow: hidden; 
  border: 1px solid #ebeef5;
}
.img-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }

/* 选中状态的卡片高亮 */
.img-card.is-selected {
  border: 2px solid #67c23a; /* 绿色边框 */
  box-shadow: 0 0 10px rgba(103, 194, 58, 0.3);
}

/* 勾选框浮层 */
.checkbox-overlay {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  padding: 2px 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.image-wrapper { height: 160px; overflow: hidden; background: #f5f7fa; display: flex; align-items: center; justify-content: center; }
.image { width: 100%; height: 100%; object-fit: cover; }
.info { padding: 10px; }
.filename { font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-bottom: 6px; }
.meta-row { display: flex; justify-content: space-between; align-items: center; }
.size { font-size: 12px; color: #999; }
.mini-tag { transform: scale(0.9); margin-right: -4px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }

/* 轮播样式 */
/* 这里使用了 :deep 来穿透 Element Plus 的内部样式 */
:deep(.carousel-dialog .el-dialog__body) {
  padding: 0;
  height: 100%;
  background-color: #000; /* 黑色背景 */
}
:deep(.carousel-dialog .el-dialog__header) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2000;
  background: transparent;
  border: none;
}
:deep(.carousel-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #fff; /* 关闭按钮白色 */
  font-size: 24px;
}

.carousel-item-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
}

.carousel-image {
  max-width: 90%;
  max-height: 85vh;
  object-fit: contain; /* 保持图片比例 */
  box-shadow: 0 0 30px rgba(255,255,255,0.1);
}

.carousel-caption {
  position: absolute;
  bottom: 20px;
  color: #fff;
  text-align: center;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
  background: rgba(0,0,0,0.5);
  padding: 10px 20px;
  border-radius: 20px;
}
.carousel-caption h3 { margin: 0 0 5px 0; font-weight: normal; font-size: 18px; }
.carousel-caption p { margin: 0; font-size: 14px; color: #ccc; }

/* 响应式 */
@media (max-width: 600px) {
  .search-group { width: 100%; max-width: none; }
  .toolbar { flex-direction: column-reverse; }
  .actions { width: 100%; display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 5px; }
}
</style>