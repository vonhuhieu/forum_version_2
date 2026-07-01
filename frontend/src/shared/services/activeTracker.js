import api from '@/shared/services/api.service'

let hasInteraction = false
let trackerInterval = null

// Hàm callback cho các sự kiện tương tác
const handleUserInteraction = () => {
  hasInteraction = true
}

// Danh sách các sự kiện cần lắng nghe để xác thực người dùng đang hoạt động thực tế
const activityEvents = ['mousemove', 'scroll', 'keydown', 'click', 'input', 'change']

export const activeTracker = {
  startTracking() {
    // Ngăn chặn tạo nhiều bộ theo dõi trùng lặp
    if (trackerInterval) return

    // Đăng ký lắng nghe sự kiện tương tác trên toàn bộ cửa sổ trình duyệt
    activityEvents.forEach((event) => {
      window.addEventListener(event, handleUserInteraction, { passive: true })
    })

    // Thực hiện cuộc gọi cập nhật ngay khi vừa khởi chạy (nếu có token đăng nhập)
    const token = localStorage.getItem('token')
    if (token) {
      api.post('/users/me/active').catch(() => {})
    }

    // Thiết lập bộ đếm định kỳ gửi heartbeat lên server mỗi 60 giây
    trackerInterval = setInterval(() => {
      const currentToken = localStorage.getItem('token')
      // Chỉ gửi request khi người dùng đã đăng nhập, tab đang active (visible) và có tương tác thực tế
      if (currentToken && document.visibilityState === 'visible' && hasInteraction) {
        api.post('/users/me/active')
          .then(() => {
            // Reset cờ hiệu tương tác để chờ tương tác tiếp theo ở chu kỳ mới
            hasInteraction = false
          })
          .catch((err) => {
            console.error('Lỗi cập nhật thời gian hoạt động:', err)
          })
      }
    }, 60000)
  },

  updateActive() {
    const token = localStorage.getItem('token')
    if (token) {
      api.post('/users/me/active')
        .then(() => {
          hasInteraction = false
        })
        .catch(() => {})
    }
  },

  stopTracking() {
    if (trackerInterval) {
      clearInterval(trackerInterval)
      trackerInterval = null
    }
    activityEvents.forEach((event) => {
      window.removeEventListener(event, handleUserInteraction)
    })
  }
}
