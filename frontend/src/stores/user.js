import { defineStore } from 'pinia'
import request from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  }),
  actions: {
    async login(form) {
      // 后端返回 { token: '...', user: {...} }
      const res = await request.post('/auth/login', form)
      this.token = res.token
      this.userInfo = res.user
      localStorage.setItem('token', res.token)
      localStorage.setItem('userInfo', JSON.stringify(res.user))
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      localStorage.clear()
    }
  }
})