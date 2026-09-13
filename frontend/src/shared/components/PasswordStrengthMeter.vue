<template>
  <div v-if="hasValue" class="password-strength-container">
    <div class="meter-track">
      <div
        class="meter-fill"
        :style="{
          width: strength.percent + '%',
          backgroundColor: strength.color
        }"
      ></div>
    </div>
    <div class="strength-label" :style="{ color: strength.color }">
      {{ strength.text }}
    </div>
  </div>
</template>

<script>
import zxcvbn from 'zxcvbn'

// Bộ từ điển ánh xạ thông báo phản hồi của zxcvbn theo chuẩn XenForo tiếng Việt
const FEEDBACK_TRANSLATIONS = {
  'This is a top-10 common password': 'Mật khẩu này xuất hiện trong danh sách 10 mật khẩu phổ biến nhất.',
  'This is a top-100 common password': 'Mật khẩu này xuất hiện trong danh sách 100 mật khẩu phổ biến nhất.',
  'This is a very common password': 'Mật khẩu này rất phổ biến và dễ đoán.',
  'This is similar to a commonly used password': 'Mật khẩu này tương tự với mật khẩu thường dùng.',
  'A word by itself is easy to guess': 'Một từ đơn lẻ rất dễ đoán.',
  'Names and surnames by themselves are easy to guess': 'Tên hoặc họ đơn lẻ rất dễ đoán.',
  'Common names and surnames are easy to guess': 'Tên họ phổ biến rất dễ đoán.',
  'Repeats like "aaa" are easy to guess': 'Repeated characters such as "aaa" are easy to guess. (Ký tự lặp lại rất dễ đoán)',
  'Repeats like "abcabcabc" are easy to guess': 'Các chuỗi lặp lại như "abcabc" rất dễ đoán.',
  'Sequences like abc or 6543 are easy to guess': 'Chuỗi tăng dần hoặc liên tiếp như "1234" hay "abcd" rất dễ đoán.',
  'Recent years are easy to guess': 'Các năm gần đây rất dễ đoán.',
  'Dates are often easy to guess': 'Ngày tháng năm sinh thường rất dễ đoán.',
  'Straight rows of keys are easy to guess': 'Các phím bấm liền kề nhau trên bàn phím rất dễ đoán.',
  'Short keyboard patterns are easy to guess': 'Mẫu bàn phím ngắn rất dễ đoán.'
}

export default {
  name: 'PasswordStrengthMeter',
  props: {
    password: {
      type: String,
      default: ''
    }
  },
  computed: {
    hasValue() {
      return Boolean(this.password && this.password.length > 0)
    },
    strength() {
      const pwd = this.password || ''
      if (!pwd) {
        return { score: 0, percent: 0, text: '', color: 'transparent' }
      }

      // 1. Nếu quá ngắn dưới 4 ký tự
      if (pwd.length < 4) {
        return {
          score: 0,
          percent: 15,
          text: 'Mật khẩu quá ngắn.',
          color: '#e74c3c'
        }
      }

      // 2. Chạy thuật toán zxcvbn chuẩn công nghiệp của Dropbox & XenForo
      const result = zxcvbn(pwd)
      const score = result.score // Giá trị từ 0 đến 4
      const warning = result.feedback?.warning

      // Dịch thông điệp cảnh báo nếu có
      let translatedWarning = ''
      if (warning) {
        translatedWarning = FEEDBACK_TRANSLATIONS[warning] || warning
      }

      // Phân tầng trạng thái theo điểm zxcvbn
      switch (score) {
        case 0:
        case 1:
          return {
            score: score,
            percent: 25,
            text: translatedWarning || 'Mật khẩu quá yếu và dễ đoán.',
            color: '#e74c3c' // Đỏ
          }
        case 2:
          return {
            score: 2,
            percent: 50,
            text: translatedWarning || 'Mật khẩu được chọn có thể mạnh hơn nữa.',
            color: '#f39c12' // Vàng cam chuẩn XenForo
          }
        case 3:
          return {
            score: 3,
            percent: 75,
            text: 'Mật khẩu an toàn.',
            color: '#27ae60' // Xanh lá
          }
        case 4:
          return {
            score: 4,
            percent: 100,
            text: 'Mật khẩu rất mạnh và an toàn.',
            color: '#219653' // Xanh lục đậm
          }
        default:
          return {
            score: 2,
            percent: 50,
            text: 'Mật khẩu được chọn có thể mạnh hơn nữa.',
            color: '#f39c12'
          }
      }
    }
  }
}
</script>

<style scoped>
.password-strength-container {
  margin-top: 6px;
  margin-bottom: 8px;
  animation: fadeIn 0.2s ease-in-out;
}

.meter-track {
  height: 4px;
  width: 100%;
  background-color: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.meter-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.3s ease;
}

.strength-label {
  font-size: 0.78rem;
  margin-top: 5px;
  line-height: 1.3;
  font-weight: 500;
  transition: color 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-2px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
