<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
</script>

<style>
/* 这里可以放一些全局重置样式，或者留空使用 style.css */
#app {
  width: 100%;
  height: 100%;
}
</style>