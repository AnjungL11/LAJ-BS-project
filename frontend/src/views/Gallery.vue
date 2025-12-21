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
        
        <el-button plain :icon="Filter" @click="showFilter = true" class="filter-btn">
          筛选
        </el-button>
      </div>

      <div class="actions">
        <el-tooltip content="全屏播放当前列表的所有图片" placement="bottom">
          <el-button type="success" :icon="VideoPlay" @click="playAll">
            幻灯片放映
          </el-button>
        </el-tooltip>

        <el-button 
          v-if="!isSelectionMode" 
          type="primary" 
          plain 
          :icon="List" 
          @click="enterSelectionMode"
        >
          选择图片
        </el-button>
        <el-button 
          v-else 
          plain 
          @click="exitSelectionMode"
        >
          取消选择
        </el-button>

        <el-button type="primary" :icon="Upload" @click="$router.push('/upload')">
          上传
        </el-button>
      </div>
    </div>

    <el-drawer v-model="showFilter" title="高级检索" size="300px">
       <el-form :model="searchForm" label-position="top">
        <el-form-item label="包含标签">
          <el-select v-model="searchForm.tags" multiple filterable allow-create default-first-option placeholder="输入标签回车" style="width: 100%">
            <el-option label="风景" value="风景" /><el-option label="人物" value="人物" /><el-option v-for="y in 5" :key="y" :label="`${2020+y}年`" :value="`${2020+y}年`" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备型号"><el-input v-model="searchForm.cameraModel" /></el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto"><el-button @click="resetFilter">重置</el-button><el-button type="primary" @click="handleSearch">搜索</el-button></div>
      </template>
    </el-drawer>

    <el-row :gutter="20" v-loading="loading" class="image-grid">
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
            <div class="play-overlay" v-if="!isSelectionMode">
               <el-icon><VideoPlay /></el-icon>
            </div>
          </div>
          <div class="info">
            <div class="filename" :title="img.originalFilename">{{ img.originalFilename }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-empty v-if="!loading && imageList.length === 0" description="暂无图片" />
    <div class="pagination" v-if="total > 0">
        <el-pagination layout="prev, pager, next" :total="total" :page-size="searchForm.size" :current-page="searchForm.page" @current-change="handlePageChange" />
    </div>

    <transition name="el-zoom-in-bottom">
      <div v-if="isSelectionMode" class="selection-bar">
        <div class="selection-info">
          已选择 <b>{{ selectedIds.size }}</b> 张图片
        </div>
        <div class="selection-actions">
          <el-button text bg @click="exitSelectionMode">取消</el-button>
          <el-button 
            type="success" 
            :icon="VideoPlay" 
            :disabled="selectedIds.size === 0"
            @click="playSelected"
          >
            播放选中图片
          </el-button>
          </div>
      </div>
    </transition>

    <el-dialog 
      v-model="carouselVisible" 
      fullscreen 
      append-to-body 
      class="carousel-dialog" 
      :show-close="true"
    >
      <el-carousel 
        :autoplay="true" 
        :interval="4000" 
        arrow="always" 
        height="100vh" 
        indicator-position="none"
        pause-on-hover
      >
        <el-carousel-item v-for="img in carouselList" :key="img.imageId">
          <div class="carousel-item-wrapper">
            <img :src="getImageUrl(img.storagePath, false)" class="carousel-image" />
            <div class="carousel-caption">
              <h3>{{ img.originalFilename }}</h3>
              <p>{{ img.tags ? img.tags.join(' · ') : '暂无标签' }}</p>
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
import { Search, Upload, Filter, VideoPlay, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const imageList = ref([])
const loading = ref(false)
const showFilter = ref(false)
const total = ref(0)
const dateRange = ref([])

// 状态管理
const isSelectionMode = ref(false)
const selectedIds = ref(new Set())
const carouselVisible = ref(false)
const carouselList = ref([])

const searchForm = reactive({
  filename: '',
  tags: [],
  cameraModel: '',
  page: 1,
  size: 12
})

// 数据获取
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

// 播放逻辑

// 播放所有
const playAll = () => {
  if (imageList.value.length === 0) {
    ElMessage.warning('当前列表没有图片可播放')
    return
  }
  carouselList.value = [...imageList.value] // 复制当前列表
  carouselVisible.value = true
}

// 播放选中
const playSelected = () => {
  const selected = imageList.value.filter(img => selectedIds.value.has(img.imageId))
  if (selected.length === 0) return
  
  carouselList.value = selected
  carouselVisible.value = true
}

// 选择模式逻辑

const enterSelectionMode = () => {
  isSelectionMode.value = true
}

const exitSelectionMode = () => {
  isSelectionMode.value = false
  selectedIds.value.clear()
}

const handleCardClick = (img) => {
  if (isSelectionMode.value) {
    const id = img.imageId
    if (selectedIds.value.has(id)) selectedIds.value.delete(id)
    else selectedIds.value.add(id)
  } else {
    // 正常模式点击看详情
    router.push(`/detail/${img.imageId}`)
  }
}

const toggleSelection = (id, checked) => {
  if (checked) selectedIds.value.add(id)
  else selectedIds.value.delete(id)
}

// 通用逻辑
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

const getImageUrl = (path, isThumbnail = true) => {
  if (!path) return ''
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return isThumbnail ? `/uploads/thumb/${filename}` : `/uploads/original/${filename}`
}
onMounted(fetchImages)
</script>

<style scoped>
/* 基础布局 */
.gallery-page { padding-bottom: 80px; /* 为底部悬浮栏留出空间 */ }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 10px; }
.search-group { display: flex; align-items: center; flex: 1; max-width: 600px; gap: 10px; }
.search-input { flex: 1; }

/* 卡片样式 */
.img-col { margin-bottom: 20px; }
.img-card { 
  cursor: pointer; 
  position: relative; 
  transition: all 0.2s; 
  border-radius: 8px; 
  overflow: hidden; 
  border: 1px solid #ebeef5;
}
.img-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
.img-card.is-selected { border: 2px solid #67c23a; }

/* 图片容器 */
.image-wrapper { height: 160px; overflow: hidden; background: #f5f7fa; display: flex; align-items: center; justify-content: center; position: relative; }
.image { width: 100%; height: 100%; object-fit: cover; }

/* 悬停显示播放图标 */
.play-overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.3s;
}
.play-overlay .el-icon { font-size: 40px; color: #fff; }
.img-card:hover .play-overlay { opacity: 1; }

.info { padding: 10px; }
.filename { font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.checkbox-overlay {
  position: absolute; top: 10px; right: 10px; z-index: 10;
  background: #fff; border-radius: 4px; padding: 2px 6px; box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

/* 底部悬浮操作栏 */
.selection-bar {
  position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%);
  background: #fff;
  padding: 10px 30px;
  border-radius: 50px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
  display: flex; align-items: center; gap: 20px;
  z-index: 100;
  border: 1px solid #ebeef5;
}
.selection-info { font-size: 14px; color: #606266; }

/* 轮播样式 */
/* 弹窗背景变黑，且铺满全屏 */
:deep(.carousel-dialog) {
  background: #000 !important; /* 覆盖默认白色 */
  margin: 0 !important;
  display: flex;
  flex-direction: column;
}

/* 让关闭按钮悬浮 */
:deep(.carousel-dialog .el-dialog__header) {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 9999;
  padding: 0;
  background: transparent;
  border: none;
  width: auto;
}

/* 关闭按钮变大变白 */
:deep(.carousel-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #fff !important;
  font-size: 30px;
  font-weight: bold;
  text-shadow: 0 0 5px rgba(0,0,0,0.5);
}

/* Body区域填满剩余空间，且去除内边距 */
:deep(.carousel-dialog .el-dialog__body) {
  padding: 0 !important;
  margin: 0 !important;
  height: 100vh; /* 关键：强制高度 */
  overflow: hidden;
  background: #000;
}

.carousel-item-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  background-color: #000;
}

.carousel-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain; /* 保证图片完整显示不裁剪 */
  display: block;
}

.carousel-caption {
  position: absolute;
  bottom: 40px;
  color: #fff;
  text-align: center;
  text-shadow: 0 2px 4px rgba(0,0,0,0.8);
  background: rgba(0,0,0,0.6);
  padding: 10px 30px;
  border-radius: 30px;
  pointer-events: none; /* 防止遮挡图片点击 */
}
.carousel-caption h3 { margin: 0 0 5px 0; font-weight: normal; font-size: 18px; }
.carousel-caption p { margin: 0; font-size: 14px; color: #ddd; }
/* 移动端适配 */
@media (max-width: 600px) {
  .selection-bar { width: 90%; justify-content: space-between; padding: 10px 20px; }
}
</style>