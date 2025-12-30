<template>
  <el-container class="app-wrapper" id="app-wrapper">
    
    <el-header class="mobile-header">
      <div class="header-left">
        <el-button link @click="drawerVisible = true">
          <el-icon size="24" color="#fff"><Expand /></el-icon>
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
          <el-icon size="24" color="#fff"><Fold /></el-icon>
        </el-button>
      </div>

      <el-menu :default-active="$route.path" class="el-menu-vertical custom-mobile-menu">
        <el-menu-item index="/gallery" @click="handleMobileNavigate('/gallery')">
          <el-icon><Menu /></el-icon><span>图片库</span>
        </el-menu-item>
        <el-menu-item index="/upload" @click="handleMobileNavigate('/upload')">
          <el-icon><UploadFilled /></el-icon><span>上传图片</span>
        </el-menu-item>
        <el-menu-item index="/tags" @click="handleMobileNavigate('/tags')">
          <el-icon><Collection /></el-icon><span>标签管理</span>
        </el-menu-item>
        
        <el-menu-item index="/ai" @click="handleMobileNavigate('/ai')">
          <el-icon><ChatDotRound /></el-icon><span>AI 助手</span>
        </el-menu-item>

        <el-menu-item @click="handleLogout">
          <el-icon><SwitchButton /></el-icon><span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>

    <el-aside width="220px" class="sidebar desktop-sidebar">
      <div class="logo">
        <el-icon><Picture /></el-icon> <span>图片管家</span>
      </div>
      
      <el-menu 
        router 
        :default-active="$route.path" 
        class="el-menu-vertical custom-menu"
      >
        <el-menu-item index="/gallery">
          <el-icon><Menu /></el-icon><span>图片库</span>
        </el-menu-item>
        <el-menu-item index="/upload">
          <el-icon><UploadFilled /></el-icon><span>上传图片</span>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><Collection /></el-icon><span>标签管理</span>
        </el-menu-item>

        <el-menu-item index="/ai">
          <el-icon><ChatDotRound /></el-icon><span>AI 助手</span>
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
import { Picture, Menu, UploadFilled, SwitchButton, Expand, Fold, Collection, ChatDotRound } from '@element-plus/icons-vue'

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
  flex-direction: row !important; 
  overflow: hidden; 
}

/* 电脑端侧边栏 */
.sidebar { 
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex; 
  flex-direction: column; 
  height: 100%; 
  flex-shrink: 0; 
  box-shadow: 2px 0 8px rgba(0,0,0,0.1); 
}

.logo { 
  height: 60px; 
  flex-shrink: 0;
  display: flex; 
  align-items: center; 
  justify-content: center; 
  font-weight: bold; 
  color: #fff; 
  font-size: 18px; 
  border-bottom: 1px solid rgba(255, 255, 255, 0.1); 
}
.logo .el-icon { margin-right: 8px; font-size: 22px; }
.el-menu-vertical { border-right: none; flex: 1; }

/* 电脑端菜单样式 */
:deep(.custom-menu) { background-color: transparent; border-right: none; }
:deep(.custom-menu .el-menu-item) { color: rgba(255, 255, 255, 0.85); }
:deep(.custom-menu .el-menu-item:hover) { background-color: rgba(255, 255, 255, 0.1); color: #fff; }
:deep(.custom-menu .el-menu-item.is-active) { background-color: rgba(255, 255, 255, 0.2); color: #fff; font-weight: bold; border-right: 3px solid #ffd04b; }
:deep(.custom-menu .el-menu-item .el-icon) { color: rgba(255, 255, 255, 0.9); }
:deep(.custom-menu .el-menu-item.is-active .el-icon) { color: #fff; }


/* 主内容区 */
.main-content { 
  background: #f5f7fa; 
  padding: 20px; 
  flex: 1; 
  height: 100%; 
  overflow-y: auto; 
  min-width: 0; 
  display: block; 
}

/* 手机端Header */
.mobile-header {
  display: none; 
  background: linear-gradient(90deg, #667eea, #764ba2);
  height: 50px;
  align-items: center;
  padding: 0 15px;
  flex-shrink: 0;
  color: #fff;
}
.header-left { display: flex; align-items: center; gap: 10px; height: 100%; }
.mobile-title { font-weight: bold; color: #fff; font-size: 16px; }

/* 抽屉背景 */
:deep(.mobile-drawer .el-drawer__body) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  display: flex;
  flex-direction: column;
}

/* 抽屉头部 */
.drawer-header-row {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}
/* Logo文字颜色 */
.drawer-logo { 
  display: flex; 
  align-items: center; 
  font-weight: bold; 
  color: #fff;
  font-size: 18px; 
}
.drawer-logo .el-icon { margin-right: 8px; }

/* 抽屉内菜单样式 */
:deep(.custom-mobile-menu) {
  background-color: transparent !important;
  border-right: none;
}
:deep(.custom-mobile-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.85);
}
:deep(.custom-mobile-menu .el-menu-item:hover) {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}
:deep(.custom-mobile-menu .el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.2);
  color: #fff;
}
:deep(.custom-mobile-menu .el-menu-item .el-icon) {
  color: #fff;
}

@media (max-width: 768px) {
  #app-wrapper.app-wrapper {
    flex-direction: column !important; 
  }
  .desktop-sidebar { display: none !important; }
  .mobile-header { display: flex !important; }
  .main-content { padding: 10px; height: auto; overflow-y: auto; }
}
</style>