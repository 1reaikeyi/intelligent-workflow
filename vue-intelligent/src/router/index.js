import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        { path: '/tool', component: () => import('@/views/tool.vue') },
        { path: '/speech', component: () => import('@/views/speech.vue') }
    ]
})

export default router
