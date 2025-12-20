<template>
  <div class="login-container">
    <div class="login-box">
      <div class="header">
        <h2>图片管家</h2>
        <p>安全存储您的美好回忆</p>
      </div>
      
      <el-form :model="form" :rules="rules" ref="loginFormRef" class="login-form">
        
        <el-form-item prop="username" v-if="isRegister">
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名" 
            :prefix-icon="UserFilled" 
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input 
            v-model="form.email" 
            placeholder="请输入电子邮箱" 
            :prefix-icon="Message" 
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码 (至少6位)" 
            :prefix-icon="Lock" 
            show-password 
          />
        </el-form-item>

        <el-button type="primary" class="login-btn" @click="handleAction" :loading="loading">
          {{ isRegister ? '注册账户' : '立即登录' }}
        </el-button>
        
        <div class="footer-links">
          <span @click="toggleMode">{{ isRegister ? '已有账号？去登录' : '注册新账号' }}</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { UserFilled, Lock, Message } from '@element-plus/icons-vue' // 引入图标
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const isRegister = ref(false)
const loading = ref(false)
const loginFormRef = ref(null) // 获取表单实例

const form = reactive({ 
  email: '', 
  password: '', 
  username: '' 
})

// 定义验证规则
const rules = computed(() => {
  const commonRules = {
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
    ]
  }
  // 注册时额外验证用户名
  if (isRegister.value) {
    return {
      ...commonRules,
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ]
    }
  }
  return commonRules
})

// 切换模式时重置表单
const toggleMode = () => {
  isRegister.value = !isRegister.value
  loginFormRef.value?.resetFields()
}

const handleAction = async () => {
  if (!loginFormRef.value) return
  
  // 提交前进行表单验证
  await loginFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      loading.value = true
      try {
        if (isRegister.value) {
          // 注册逻辑
          await request.post('/auth/register', { 
            username: form.username,
            email: form.email,
            passwordHash: form.password // 后端接收的是 passwordHash 字段
          })
          ElMessage.success('注册成功，请登录')
          toggleMode() // 注册成功后切换回登录模式
        } else {
          // 登录逻辑
          await userStore.login({
            email: form.email,
            password: form.password
          })
          router.push('/gallery')
        }
      } catch (error) {
        // 4. 这里的 error 会被 request.js 拦截器捕获并显示后端返回的具体 message
        console.error(error)
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.warning('请检查输入项是否符合要求')
      return false
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
}
.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
}
.header h2 { text-align: center; color: #333; margin-bottom: 10px; }
.header p { text-align: center; color: #999; margin-bottom: 30px; font-size: 14px; }
.login-btn { width: 100%; background: linear-gradient(90deg, #667eea, #764ba2); border: none; margin-top: 10px;}
.footer-links { text-align: center; margin-top: 15px; font-size: 12px; color: #667eea; cursor: pointer; }
</style>