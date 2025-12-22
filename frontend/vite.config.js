import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    // 允许局域网访问
    host: '0.0.0.0',
    port: 3000,
    open: true,

    proxy: {
      // API请求代理
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态图片资源代理
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})