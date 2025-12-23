<template>
  <div class="tag-manager-page" v-loading="loading">
    <div class="toolbar">
      <div class="left">
        <h2>标签管理</h2>
        <el-checkbox v-model="showEmpty" label="显示空标签" style="margin-left: 20px;" />
        <span class="sub-text" v-if="showEmpty">拖拽卡片可调整顺序</span>
        <span class="sub-text" v-else style="color: #E6A23C">筛选模式下不可拖拽</span>
      </div>
      <div class="right">
        <el-button type="danger" plain :icon="Delete" @click="handleClearEmpty">一键清理空标签</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建标签</el-button>
      </div>
    </div>

    <draggable 
      v-model="tagList" 
      item-key="id" 
      class="tag-grid" 
      ghost-class="ghost"
      :disabled="!showEmpty"  
      @end="handleDragEnd"
    >
      <template #item="{ element }">
        <div class="tag-card-wrapper" v-if="showEmpty || element.count > 0">
          <div 
            class="tag-card" 
            :class="{ 'is-empty': element.count === 0 }"
            @click="navigateToAlbum(element)"
          >
            <div class="cover-area" :style="getCoverStyle(element)">
              <img 
                v-if="element.coverType === 'image' && element.coverUrl" 
                :src="element.coverUrl" 
                class="cover-img"
              />
              <div v-else class="color-cover">
                <el-icon :size="40" color="rgba(255,255,255,0.5)"><Collection /></el-icon>
              </div>
              
              <div class="card-actions" @click.stop>
                <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, element)">
                  <el-button circle size="small" :icon="MoreFilled" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="rename">重命名</el-dropdown-item>
                      <el-dropdown-item command="cover">设置封面</el-dropdown-item>
                      <el-dropdown-item command="delete" divided style="color: #f56c6c">删除标签</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <div class="card-footer">
              <div class="tag-name" :title="element.name">{{ element.name }}</div>
              <div class="tag-count" :style="{ color: element.count === 0 ? '#F56C6C' : '' }">
                {{ element.count }} 张图片
              </div>
            </div>
          </div>
        </div>
      </template>
    </draggable>

    <el-empty v-if="!loading && tagList.length === 0" description="暂无标签，快去创建吧" />

    <el-dialog v-model="dialogVisible" :title="dialogType === 'create' ? '新建标签' : '重命名标签'" width="400px">
      <el-form>
        <el-form-item label="标签名称">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="20" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog 
      v-model="coverDialogVisible" 
      title="设置标签封面" 
      width="600px"
      append-to-body
      @open="loadCoverImages"
    >
      <el-tabs v-model="activeCoverTab" class="cover-tabs">
        <el-tab-pane label="纯色背景" name="color">
          <div class="color-picker-container">
            <el-color-picker v-model="tempColor" size="large" show-alpha />
            <p class="hint">选择一种颜色作为封面背景</p>
            <div class="preview-box" :style="{ backgroundColor: tempColor }">
              <el-icon :size="30" color="rgba(255,255,255,0.5)"><Collection /></el-icon>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="从标签中选择图片" name="image">
          <div v-loading="coverLoading" class="image-selector">
            <div v-if="coverImageList.length > 0" class="mini-grid">
              <div 
                v-for="img in coverImageList" 
                :key="img.imageId"
                class="mini-item"
                :class="{ active: tempCoverUrl === getImageUrl(img.thumbnailPath) }"
                @click="selectCoverImage(img)"
              >
                <img :src="getImageUrl(img.thumbnailPath)" loading="lazy" />
                <div class="check-mark" v-if="tempCoverUrl === getImageUrl(img.thumbnailPath)">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
            </div>
            <el-empty v-else description="该标签下没有图片可选" :image-size="60" />
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="coverDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCover">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import draggable from 'vuedraggable'
import { useRouter } from 'vue-router'
import { Plus, MoreFilled, Collection, Delete, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const tagList = ref([])

// 状态定义
const showEmpty = ref(true)
const dialogVisible = ref(false)
const dialogType = ref('create') 
const currentTag = ref(null) 
const form = reactive({ name: '' })

// 封面弹窗状态
const coverDialogVisible = ref(false)
const activeCoverTab = ref('color')
const tempColor = ref('#409EFF')
const tempCoverUrl = ref('')
const coverImageList = ref([])
const coverLoading = ref(false)

const getImageUrl = (path) => {
  if (!path) return ''
  const filename = path.replace(/\\/g, '/').split('/').pop()
  return `/uploads/thumb/${filename}`
}

const getCoverStyle = (tag) => {
  if (tag.coverType === 'color' || !tag.coverUrl) {
    return { backgroundColor: tag.coverColor || '#909399' }
  }
  return {}
}

// 获取列表
const fetchTags = async () => {
  loading.value = true
  try {
    const res = await request.get('/tags')
    const realData = Array.isArray(res) ? res : (res.data || [])
    
    tagList.value = realData.map(item => ({
      id: item.tagId,
      name: item.tagName,
      count: item.count || 0,
      coverType: item.coverType || 'color',
      coverUrl: item.coverUrl,
      coverColor: item.coverColor || '#409EFF',
      sortOrder: item.sortOrder || 0
    }))
  } catch (error) {
    console.error("获取标签列表失败", error)
  } finally {
    loading.value = false
  }
}

// 打开封面弹窗
const openCoverDialog = (tag) => {
  // 调试日志
  console.log('正在打开封面弹窗, 标签:', tag.name)
  
  if (!tag) {
    console.error('标签对象为空！')
    return
  }

  currentTag.value = tag
  
  // 初始化回显逻辑
  if (tag.coverType === 'image' && tag.coverUrl) {
    activeCoverTab.value = 'image'
    tempCoverUrl.value = tag.coverUrl
    tempColor.value = tag.coverColor || '#409EFF'
  } else {
    activeCoverTab.value = 'color'
    tempColor.value = tag.coverColor || '#409EFF'
    tempCoverUrl.value = ''
  }
  // 先清空，防止显示旧数据
  coverImageList.value = []
  // 打开弹窗
  coverDialogVisible.value = true
}

// 删除逻辑
const handleDelete = (tag) => {
  const msg = tag.count === 0 
    ? `标签“${tag.name}”是空的，确定删除吗？` 
    : `标签“${tag.name}”下包含 ${tag.count} 张图片。删除标签不会删除图片。确定继续？`
    
  ElMessageBox.confirm(msg, '删除确认', { type: tag.count === 0 ? 'info' : 'warning' })
    .then(async () => {
      await request.delete(`/tags/${tag.id}`)
      ElMessage.success('删除成功')
      // 前端删除，不刷新
      tagList.value = tagList.value.filter(t => t.id !== tag.id)
    }).catch(() => {})
}

// 加载图片
const loadCoverImages = async () => {
  // 调试日志
  console.log('弹窗已打开，正在加载图片...')
  if (!currentTag.value) return
  coverLoading.value = true
  try {
    const res = await request.get(`/tags/${currentTag.value.id}/images`)
    coverImageList.value = Array.isArray(res) ? res : (res.data || [])
  } catch (error) {
    console.error(error)
    ElMessage.error('加载图片失败')
  } finally {
    coverLoading.value = false
  }
}

// 提交封面
const submitCover = async () => {
  if (!currentTag.value) return
  
  try {
    const payload = {}
    if (activeCoverTab.value === 'color') {
      payload.coverType = 'color'
      payload.coverColor = tempColor.value
    } else {
      if (!tempCoverUrl.value) {
        ElMessage.warning('请先选择一张图片')
        return
      }
      payload.coverType = 'image'
      payload.coverUrl = tempCoverUrl.value
    }

    await request.put(`/tags/${currentTag.value.id}/style`, payload)
    
    ElMessage.success('封面设置成功')
    coverDialogVisible.value = false
    
    // 更新本地列表数据
    currentTag.value.coverType = payload.coverType
    if (payload.coverType === 'color') {
      currentTag.value.coverColor = payload.coverColor
      currentTag.value.coverUrl = null
    } else {
      currentTag.value.coverUrl = payload.coverUrl
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('保存失败')
  }
}

const selectCoverImage = (img) => {
  tempCoverUrl.value = getImageUrl(img.thumbnailPath)
}

// 菜单点击分发
const handleCommand = (cmd, tag) => {
  // 调试日志
  console.log('点击菜单命令:', cmd)
  currentTag.value = tag
  
  if (cmd === 'rename') {
    dialogType.value = 'rename'
    form.name = tag.name
    dialogVisible.value = true
  } else if (cmd === 'cover') {
    openCoverDialog(tag)
  } else if (cmd === 'delete') {
    handleDelete(tag)
  }
}

const navigateToAlbum = (tag) => {
  if (tag.count === 0) {
    ElMessage.warning('该标签下没有图片')
    return
  }
  router.push(`/tags/${tag.id}`)
}

const handleDragEnd = async () => {
  if (!showEmpty.value) return;
  const sortedIds = tagList.value.map(t => t.id)
  try {
    await request.post('/tags/reorder', { ids: sortedIds })
  } catch (error) {
    console.error(error)
  }
}

const handleClearEmpty = () => {
  ElMessageBox.confirm('确定要删除所有【0张图片】的空标签吗？', '一键清理', { type: 'warning' })
    .then(async () => {
      await request.delete('/tags/empty')
      ElMessage.success('清理完成')
      fetchTags() 
    }).catch(() => {})
}

const openCreateDialog = () => {
  dialogType.value = 'create'
  form.name = ''
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!form.name.trim()) return ElMessage.warning('请输入名称')
  try {
    if (dialogType.value === 'create') {
      await request.post('/tags', { name: form.name })
      ElMessage.success('创建成功')
    } else {
      await request.put(`/tags/${currentTag.value.id}`, { name: form.name })
      ElMessage.success('重命名成功')
    }
    dialogVisible.value = false
    fetchTags()
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.tag-manager-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}
.sub-text { color: #909399; font-size: 13px; margin-left: 10px; }

.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 25px;
}

.tag-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #ebeef5;
}

.tag-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
}

.ghost {
  opacity: 0.5;
  background: #ecf5ff;
  border: 2px dashed #409EFF;
}

.cover-area {
  height: 160px;
  position: relative;
  background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.color-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tag-card:hover .card-actions {
  opacity: 1;
}

.card-footer {
  padding: 15px;
}

.tag-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-count {
  font-size: 12px;
  color: #909399;
}

.tag-card.is-empty {
  filter: grayscale(100%);
  opacity: 0.6;
}
.tag-card.is-empty:hover {
  transform: none; 
  box-shadow: none;
  cursor: default;
}

/* 弹窗样式 */
.color-picker-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}
.hint {
  color: #909399;
  font-size: 13px;
  margin: 10px 0;
}
.preview-box {
  width: 120px;
  height: 80px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eee;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.image-selector {
  height: 400px;
  overflow-y: auto;
  border: 1px solid #eee;
  padding: 10px;
  border-radius: 4px;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
}

.mini-item {
  aspect-ratio: 1/1;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  border: 2px solid transparent;
}

.mini-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mini-item:hover {
  filter: brightness(0.9);
}

.mini-item.active {
  border-color: #409EFF;
}

.check-mark {
  position: absolute;
  top: 5px;
  right: 5px;
  background: #409EFF;
  color: #fff;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
</style>