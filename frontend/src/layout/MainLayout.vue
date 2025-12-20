<template>
  <el-container class="app-wrapper">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <el-icon><Picture /></el-icon> <span>图片管家</span>
      </div>
      <el-menu router :default-active="$route.path" class="el-menu-vertical">
        <el-menu-item index="/gallery">
          <el-icon><Menu /></el-icon><span>图片库</span>
        </el-menu-item>
        <el-menu-item index="/upload">
          <el-icon><UploadFilled /></el-icon><span>上传图片</span>
        </el-menu-item>
        <el-menu-item @click="handleLogout">
          <el-icon><SwitchButton /></el-icon><span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-wrapper { height: 100vh; }
.sidebar { background: #fff; border-right: 1px solid #eee; display: flex; flex-direction: column; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #764ba2; font-size: 18px; border-bottom: 1px solid #eee;}
.logo .el-icon { margin-right: 8px; font-size: 22px; }
.el-menu-vertical { border-right: none; }
.main-content { background: #f5f7fa; padding: 20px; }
</style>