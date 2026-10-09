import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCN from 'element-plus/dist/locale/zh-cn.js'
createApp(App).mount('#app')
const app = createApp(App)

import router from './router'
app.use(ElementPlus,{locale:zhCN})
app.use(router)
app.mount('#app')