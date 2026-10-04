import axios from 'axios'
import { LAB_SSE_EVENTS } from '@/shared/utils/constants'

/**
 * Base URL của lab-service:
 * 1. Ưu tiên VUE_APP_LAB_API_BASE_URL
 * 2. Local: http://localhost:8081
 * 3. Production: lab-api.<rootDomain> (Dynamic Origin Detection giống api.service.js)
 */
export function getLabApiBaseUrl() {
  if (process.env.VUE_APP_LAB_API_BASE_URL && !process.env.VUE_APP_LAB_API_BASE_URL.startsWith('${')) {
    return process.env.VUE_APP_LAB_API_BASE_URL
  }
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
      return 'http://localhost:8081'
    }
    const rootDomain = hostname.replace(/^www\./, '')
    return `${window.location.protocol}//lab-api.${rootDomain}`
  }
  return 'http://localhost:8081'
}

// Axios riêng cho Lab: KHÔNG xoá token, KHÔNG redirect khi gặp 401/403 (khách vẫn dùng được công khai).
const labApi = axios.create({
  baseURL: `${getLabApiBaseUrl()}/api/lab`
})

labApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

labApi.interceptors.response.use((response) => {
  const body = response.data
  if (body && Object.prototype.hasOwnProperty.call(body, 'status') && Object.prototype.hasOwnProperty.call(body, 'data')) {
    return { ...response, data: body.data }
  }
  return response
})

/**
 * Parse luồng SSE từ fetch. Giữ nguyên khoảng trắng của từng chunk (không trim).
 */
function parseSseBlock(block) {
  let event = LAB_SSE_EVENTS.MESSAGE
  const dataLines = []
  block.split('\n').forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5))
    }
  })
  return { event, data: dataLines.join('\n') }
}

export const labService = {
  /**
   * Chat dạng stream. Trả về hàm abort().
   * handlers: { onMessage, onProduct, onError, onDone }
   */
  streamChat({ sessionId, message }, handlers) {
    const controller = new AbortController()
    const params = new URLSearchParams()
    if (sessionId) params.set('sessionId', sessionId)
    params.set('message', message)

    const headers = { Accept: 'text/event-stream' }
    const token = localStorage.getItem('token')
    if (token) headers.Authorization = `Bearer ${token}`

    fetch(`${labApi.defaults.baseURL}/chat/stream?${params.toString()}`, {
      method: 'GET',
      headers,
      signal: controller.signal
    })
      .then(async (response) => {
        if (!response.ok || !response.body) {
          throw new Error(`HTTP ${response.status}`)
        }
        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        for (;;) {
          const { value, done } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
          let idx
          while ((idx = buffer.indexOf('\n\n')) !== -1) {
            const block = buffer.slice(0, idx)
            buffer = buffer.slice(idx + 2)
            const { event, data } = parseSseBlock(block)
            if (event === LAB_SSE_EVENTS.MESSAGE && handlers.onMessage) handlers.onMessage(data)
            else if (event === LAB_SSE_EVENTS.SESSION && handlers.onSession) handlers.onSession(data)
            else if (event === LAB_SSE_EVENTS.PRODUCT && handlers.onProduct) handlers.onProduct(JSON.parse(data))
            else if (event === LAB_SSE_EVENTS.ERROR && handlers.onError) handlers.onError(data)
            else if (event === LAB_SSE_EVENTS.DONE && handlers.onDone) handlers.onDone()
          }
        }
        if (handlers.onDone) handlers.onDone()
      })
      .catch((err) => {
        if (err.name !== 'AbortError' && handlers.onError) handlers.onError(err.message)
      })

    return () => controller.abort()
  },

  getSessions(params) {
    return labApi.get('/sessions', { params })
  },
  getSessionMessages(sessionId) {
    return labApi.get(`/sessions/${sessionId}/messages`)
  },
  deleteSession(sessionId) {
    return labApi.delete(`/sessions/${sessionId}`)
  },
  getPublicProduct(publicId) {
    return labApi.get(`/public/products/${publicId}`)
  },
  getPublicProductRows(publicId) {
    return labApi.get(`/public/products/${publicId}/rows`)
  },
  saveProduct(publicId) {
    return labApi.post(`/products/${publicId}/save`)
  },
  getMyProducts(params) {
    return labApi.get('/products/my', { params })
  },
  updateProduct(publicId, data) {
    return labApi.put(`/products/${publicId}`, data)
  },
  deleteProduct(publicId) {
    return labApi.delete(`/products/${publicId}`)
  },
  getDatasetRows(publicId, params) {
    return labApi.get(`/products/${publicId}/rows`, { params })
  },
  addDatasetRow(publicId, data) {
    return labApi.post(`/products/${publicId}/rows`, data)
  },
  updateDatasetRow(publicId, rowId, data) {
    return labApi.put(`/products/${publicId}/rows/${rowId}`, data)
  },
  deleteDatasetRow(publicId, rowId) {
    return labApi.delete(`/products/${publicId}/rows/${rowId}`)
  },
  addRowsBatch(publicId, rowsData) {
    return labApi.post(`/products/${publicId}/rows/batch`, rowsData)
  },
  generateAiRows(publicId, count = 5) {
    return labApi.post(`/products/${publicId}/rows/ai-generate`, { count })
  }
}

export default labApi
