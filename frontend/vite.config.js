import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // API 请求代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态图片资源代理 (后端配置的 addResourceHandlers)
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})