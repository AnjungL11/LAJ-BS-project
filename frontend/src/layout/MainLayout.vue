<template>
  <el-container class="app-wrapper" id="app-wrapper">
    
    <el-header class="mobile-header">
      <div class="header-left">
        <el-button link @click="drawerVisible = true">
          <el-icon size="24" color="#333"><Expand /></el-icon>
        </el-button>
        <span class="mobile-title">图片管家</span>
      </div>
    </el-header>

    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="70%"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-header-row">
        <div class="drawer-logo">
          <el-icon><Picture /></el-icon> <span>图片管家</span>
        </div>
        <el-button link @click="drawerVisible = false">
          <el-icon size="24" color="#333"><Fold /></el-icon>
        </el-button>
      </div>

      <el-menu :default-active="$route.path" class="el-menu-vertical">
        <el-menu-item index="/gallery" @click="handleMobileNavigate('/gallery')">
          <el-icon><Menu /></el-icon><span>图片库</span>
        </el-menu-item>
        <el-menu-item index="/upload" @click="handleMobileNavigate('/upload')">
          <el-icon><UploadFilled /></el-icon><span>上传图片</span>
        </el-menu-item>
        <el-menu-item index="/tags" @click="$router.push('/tags')">
          <el-icon><Collection /></el-icon><span>标签管理</span>
        </el-menu-item>
        
        <el-menu-item index="/ai" @click="handleMobileNavigate('/ai')">
          <el-icon><ChatDotRound /></el-icon>
          <span style="color: #764ba2; font-weight: bold;">AI 助手</span>
        </el-menu-item>

        <el-menu-item @click="handleLogout">
          <el-icon><SwitchButton /></el-icon><span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-aside width="200px" class="sidebar desktop-sidebar">
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
        <el-menu-item index="/tags" @click="$router.push('/tags')">
          <el-icon><Collection /></el-icon><span>标签管理</span>
        </el-menu-item>

        <el-menu-item index="/ai" @click="handleMobileNavigate('/ai')">
          <el-icon><ChatDotRound /></el-icon>
          <span style="color: #764ba2; font-weight: bold;">AI 助手</span>
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, Menu, UploadFilled, SwitchButton, Expand, Fold } from '@element-plus/icons-vue'

const router = useRouter()
const drawerVisible = ref(false)

const handleLogout = () => {
  localStorage.removeItem('token')
  drawerVisible.value = false
  router.push('/login')
}

const handleMobileNavigate = (path) => {
  router.push(path)
  drawerVisible.value = false
}
</script>

<style scoped>
#app-wrapper.app-wrapper { 
  width: 100%;
  height: 100vh; 
  display: flex;
  flex-direction: row !important; /* 电脑端强制横向排列 (左侧栏+右内容) */
  overflow: hidden; 
}

/* 电脑端侧边栏 */
.sidebar { 
  background: #fff; 
  border-right: 1px solid #eee; 
  display: flex; 
  flex-direction: column; 
  height: 100%; 
  flex-shrink: 0; /* 防止侧边栏被挤压 */
}
.logo { 
  height: 60px; 
  flex-shrink: 0;
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-weight: bold; 
  color: #764ba2; 
  font-size: 18px; 
  border-bottom: 1px solid #eee;
}
.logo .el-icon { margin-right: 8px; font-size: 22px; }
.el-menu-vertical { border-right: none; flex: 1; }

/* 主内容区 */
.main-content { 
  background: #f5f7fa; 
  padding: 20px; 
  flex: 1; /* 占满剩余空间 */
  height: 100%; 
  overflow-y: auto; 
  min-width: 0; /* 防止内部宽元素撑破Flex容器 */
  display: block; /* 确保是块级显示 */
}

/* 手机端顶部栏 */
.mobile-header {
  display: none; /* 电脑端隐藏 */
  background: #fff;
  border-bottom: 1px solid #eee;
  height: 50px;
  align-items: center;
  padding: 0 15px;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 10px; height: 100%; }
.mobile-title { font-weight: bold; color: #764ba2; font-size: 16px; }

/* 抽屉样式 */
.drawer-header-row {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 0 20px;
  border-bottom: 1px solid #eee;
}
.drawer-logo { display: flex; align-items: center; font-weight: bold; color: #764ba2; font-size: 18px; }
.drawer-logo .el-icon { margin-right: 8px; }

@media (max-width: 768px) {
  
  /* 改变主容器方向,强制垂直排列 */
  #app-wrapper.app-wrapper {
    flex-direction: column !important; /* 手机端强制纵向排列 (上Header+下Content) */
  }

  /* 隐藏电脑端侧边栏 */
  .desktop-sidebar {
    display: none !important;
  }

  /* 显示手机端顶部栏 */
  .mobile-header {
    display: flex !important;
  }

  /* 调整内容区 */
  .main-content {
    padding: 10px;
    height: auto; 
    overflow-y: auto;
  }
}
</style>