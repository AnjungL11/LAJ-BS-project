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
        { path: 'detail/:id', component: () => import('../views/Detail.vue') },
        { path: 'tags', name: 'TagManager', component: () => import('../views/TagManager.vue'),meta: { title: '标签管理' }},
        { path: 'tags/:id', name: 'TagAlbum', component: () => import('../views/TagAlbum.vue'), meta: { title: '标签相册' }},
        { path: 'ai', name: 'AiSearch', component: () => import('../views/AiSearch.vue'), meta: { title: 'AI 助手' }}
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