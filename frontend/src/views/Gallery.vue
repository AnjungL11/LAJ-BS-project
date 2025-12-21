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
        <el-button type="primary" :icon="Upload" @click="$router.push('/upload')">上传图片</el-button>
      </div>
    </div>

    <el-drawer v-model="showFilter" title="高级图片检索" size="300px">
      <el-form :model="searchForm" label-position="top">
        
        <el-form-item label="标签筛选">
          <el-select 
            v-model="searchForm.tags" 
            multiple 
            filterable 
            allow-create 
            default-first-option
            placeholder="输入标签并回车 (如: 风景)" 
            style="width: 100%"
          >
            <el-option label="风景" value="风景" />
            <el-option label="人物" value="人物" />
            <el-option label="动物" value="动物" />
            <el-option v-for="y in 2" :key="y" :label="`${2023+y}年`" :value="`${2023+y}年`" />
          </el-select>
        </el-form-item>

        <el-form-item label="拍摄设备">
          <el-input v-model="searchForm.cameraModel" placeholder="请输入相机型号 (如: Canon, iPhone)" />
        </el-form-item>

        <el-form-item label="拍摄时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <div style="flex: auto">
          <el-button @click="resetFilter">重置条件</el-button>
          <el-button type="primary" @click="handleSearch">确认搜索</el-button>
        </div>
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
          @click="goDetail(img.imageId)"
        >
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
        <el-pagination 
          layout="prev, pager, next" 
          :total="total" 
          :page-size="searchForm.size"
          :current-page="searchForm.page"
          @current-change="handlePageChange" 
        />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import { Search, Upload, Filter } from '@element-plus/icons-vue'

const router = useRouter()
const imageList = ref([])
const loading = ref(false)
const showFilter = ref(false) // 控制抽屉显示
const total = ref(0)
const dateRange = ref([]) // 单独处理日期范围数组

// 搜索表单数据模型
const searchForm = reactive({
  filename: '',
  tags: [],
  cameraModel: '',
  page: 1,
  size: 12
})

// 核心：获取图片列表 (调用后端 POST /search 接口)
const fetchImages = async () => {
  loading.value = true
  try {
    // 构造 DTO 参数
    const payload = {
      page: searchForm.page,
      size: searchForm.size,
      filename: searchForm.filename,
      tags: searchForm.tags,
      cameraModel: searchForm.cameraModel,
      // 拆解日期范围数组
      startTime: dateRange.value && dateRange.value.length === 2 ? dateRange.value[0] : null,
      endTime: dateRange.value && dateRange.value.length === 2 ? dateRange.value[1] : null
    }

    // 发送请求
    const res = await request.post('/images/search', payload)
    
    imageList.value = res.records
    total.value = res.total
  } catch (error) {
    console.error("搜索失败", error)
  } finally {
    loading.value = false
  }
}

// 触发搜索
const handleSearch = () => {
  searchForm.page = 1 // 重置到第一页
  showFilter.value = false // 关闭抽屉
  fetchImages()
}

// 重置筛选条件
const resetFilter = () => {
  searchForm.tags = []
  searchForm.cameraModel = ''
  dateRange.value = []
  // 可选：重置后立即搜索
  // handleSearch()
}

// 翻页处理
const handlePageChange = (val) => { 
  searchForm.page = val
  fetchImages() 
}

const goDetail = (id) => router.push(`/detail/${id}`)

// --- 辅助工具函数 ---

// 修复后的图片路径处理：根据后端 WebConfig 映射
const getImageUrl = (path) => {
  if (!path) return ''
  // 提取文件名 (兼容 Windows 反斜杠)
  const filename = path.replace(/\\/g, '/').split('/').pop()
  // 列表页只显示缩略图，对应后端 /uploads/thumb/ 映射
  return `/uploads/thumb/${filename}`
}

const formatSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024, sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 初始化加载
onMounted(fetchImages)
</script>

<style scoped>
.gallery-page {
  padding-bottom: 20px;
}

/* 工具栏样式 */
.toolbar { 
  display: flex; 
  justify-content: space-between; 
  margin-bottom: 20px; 
  flex-wrap: wrap;
  gap: 10px;
}
.search-group {
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 600px;
}
.search-input { 
  flex: 1; 
}

/* 卡片样式 */
.img-col { margin-bottom: 20px; }
.img-card { 
  cursor: pointer; 
  transition: transform 0.2s, box-shadow 0.2s; 
  border-radius: 8px; 
  overflow: hidden; 
  border: 1px solid #ebeef5;
}
.img-card:hover { 
  transform: translateY(-5px); 
  box-shadow: 0 10px 20px rgba(0,0,0,0.1);
}

/* 图片容器 */
.image-wrapper { 
  height: 160px; 
  overflow: hidden; 
  background: #f5f7fa; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  position: relative;
}
.image { 
  width: 100%; 
  height: 100%; 
  object-fit: cover; 
}

/* 信息区域 */
.info { padding: 10px; }
.filename { 
  font-size: 14px; 
  color: #333; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  margin-bottom: 6px;
}
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.size { 
  font-size: 12px; 
  color: #999; 
}
.mini-tag {
  transform: scale(0.9);
  margin-right: -4px;
}

/* 分页 */
.pagination { 
  display: flex; 
  justify-content: center; 
  margin-top: 20px; 
}

/* 响应式调整 */
@media (max-width: 600px) {
  .search-group {
    width: 100%;
    max-width: none;
  }
  .toolbar {
    flex-direction: column-reverse;
  }
  .actions {
    width: 100%;
    display: flex;
    justify-content: flex-end;
  }
}
</style>