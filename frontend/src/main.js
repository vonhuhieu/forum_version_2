import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { activeTracker } from '@/shared/services/activeTracker'

activeTracker.startTracking()

const app = createApp(App)

app.config.warnHandler = (msg, instance, trace) => {
  console.log('[Vue warn]: ' + msg + '\n' + trace)
}

app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue error]:', err, info)
}

app.use(router)
app.mount('#app')
