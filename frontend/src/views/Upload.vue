<template>
  <div class="upload-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header"><span>上传图片</span></div>
      </template>
      <el-upload
        class="upload-drag"
        drag
        action="/api/images/upload"
        :headers="uploadHeaders"
        multiple
        :on-success="handleSuccess"
        :on-error="handleError"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽文件到此处 或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">只能上传 jpg/png 文件，且不超过 10MB</div>
        </template>
      </el-upload>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const uploadHeaders = computed(() => ({
  'Authorization': 'Bearer ' + localStorage.getItem('token')
}))

const handleSuccess = (response) => {
  ElMessage.success('图片上传成功')
}
const handleError = () => {
  ElMessage.error('上传失败，请重试')
}
</script>

<style scoped>
.upload-container { display: flex; justify-content: center; padding-top: 50px; }
.upload-card { width: 100%; max-width: 800px; }
.upload-drag { width: 100%; }
</style>