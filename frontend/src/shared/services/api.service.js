import axios from 'axios'

/**
 * Lấy Base URL cho API:
 * 1. Ưu tiên biến môi trường VUE_APP_API_BASE_URL (từ Vercel hoặc .env)
 * 2. Fallback tự động nhận diện hostname của trình duyệt (Dynamic Origin Detection)
 */
export function getApiBaseUrl() {
  if (process.env.VUE_APP_API_BASE_URL && !process.env.VUE_APP_API_BASE_URL.startsWith('${')) {
    return process.env.VUE_APP_API_BASE_URL
  }
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8080/api'
    }
    const rootDomain = hostname.replace(/^www\./, '')
    return `${window.location.protocol}//api.${rootDomain}/api`
  }
  return 'http://localhost:8080/api'
}

/**
 * Lấy Backend Root URL (cho uploads và tài nguyên tĩnh):
 */
export function getBackendBaseUrl() {
  if (process.env.VUE_APP_BACKEND_URL && !process.env.VUE_APP_BACKEND_URL.startsWith('${')) {
    return process.env.VUE_APP_BACKEND_URL
  }
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8080'
    }
    const rootDomain = hostname.replace(/^www\./, '')
    return `${window.location.protocol}//api.${rootDomain}`
  }
  return 'http://localhost:8080'
}

const api = axios.create({
  baseURL: getApiBaseUrl()
})

// Interceptor đính kèm Token vào Header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Interceptor xử lý phản hồi
api.interceptors.response.use(
  (response) => {
    // Nếu phản hồi có cấu trúc { status, data } từ ResponseDTO của Backend
    if (response.data && Object.prototype.hasOwnProperty.call(response.data, 'status') && Object.prototype.hasOwnProperty.call(response.data, 'data')) {
      return { ...response, data: response.data.data }
    }
    return response
  },
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default api
