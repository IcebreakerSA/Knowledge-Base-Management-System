import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store'

const routes = [
  {
    path: '/',
    redirect: '/notebook'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/notebook',
    name: 'Notebook',
    component: () => import('@/views/Notebook.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next('/login')
    } else {
      if (!userStore.userInfo) {
        try {
          await userStore.fetchUserInfo()
        } catch (error) {
          next('/login')
          return
        }
      }
      next()
    }
  } else {
    next()
  }
})

export default router 