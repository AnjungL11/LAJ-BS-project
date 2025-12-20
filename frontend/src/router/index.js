import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('../views/Login.vue')
    },
    {
      path: '/',
      component: Layout,
      redirect: '/gallery',
      children: [
        { path: 'gallery', component: () => import('../views/Gallery.vue') },
        { path: 'upload', component: () => import('../views/Upload.vue') },
        { path: 'detail/:id', component: () => import('../views/Detail.vue') }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) next('/login')
  else next()
})

export default router