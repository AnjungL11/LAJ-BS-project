import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api', // 匹配vite代理
  timeout: 5000
})

// 请求拦截器，添加Token
service.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器，处理错误
service.interceptors.response.use(
  response => response.data,
  async error => {
    // 针对Blob类型的错误处理
    if (error.response && error.response.data instanceof Blob && error.response.data.type === 'application/json') {
      try {
        // 将Blob转回文本，读取里面的错误信息
        const text = await error.response.data.text()
        const json = JSON.parse(text)
        // 替换回JSON对象，方便后面读取message
        error.response.data = json
      } catch (e) {}
    }

    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.message || '服务异常')
    }
    
    // 如果后端返回了错误信息，就显示后端的message
    if (error.response && error.response.data) {
        const serverMsg = error.response.data.message;
        // 优先显示后端传来的具体错误
        ElMessage.error(serverMsg || '服务请求失败');
      } else {
        ElMessage.error('网络连接异常');
      }
      return Promise.reject(error)
  }
)

export default service