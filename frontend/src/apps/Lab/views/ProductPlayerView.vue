<template>
  <div class="player-fullscreen-wrapper" ref="playerWrapper">
    <!-- Loading chuẩn theo Rule 3 -->
    <Loading :visible="loading" text="Đang tải dữ liệu sản phẩm..." />

    <!-- Top floating bar -->
    <header class="player-topbar" v-if="product">
      <div class="player-brand">
        <router-link :to="LAB_ROUTES.HOME" class="btn-back">
          <i class="bi bi-arrow-left"></i> Phòng thí nghiệm
        </router-link>
        <span class="product-badge">{{ templateName }}</span>
      </div>

      <div class="player-actions">
        <button class="btn-action" @click="copyShareLink" title="Sao chép liên kết">
          <i class="bi bi-share"></i> Chia sẻ
        </button>
        <button class="btn-action" @click="toggleFullscreen" title="Toàn màn hình">
          <i class="bi bi-arrows-fullscreen"></i>
        </button>
        <button
          v-if="!product.isSaved"
          class="btn-action btn-save"
          :disabled="saving"
          @click="saveProduct"
          title="Lưu vào tài khoản của tôi"
        >
          <i class="bi bi-bookmark-check"></i> {{ saving ? 'Đang lưu...' : 'Lưu sản phẩm' }}
        </button>
      </div>
    </header>

    <!-- Nội dung chính theo template -->
    <main class="player-body" v-if="product && !loading">
      <LuckyWheelPlayer
        v-if="product.templateType === LAB_TEMPLATES.LUCKY_WHEEL"
        :product="product"
        :rows="rows"
      />
      <FlashcardPlayer
        v-else-if="product.templateType === LAB_TEMPLATES.FLASHCARD"
        :product="product"
        :rows="rows"
      />
      <MillionairePlayer
        v-else-if="product.templateType === LAB_TEMPLATES.MILLIONAIRE"
        :product="product"
        :rows="rows"
      />
      <div v-else class="text-center py-5">
        <h3>Khuôn mẫu chưa được hỗ trợ</h3>
      </div>
    </main>

    <!-- Báo lỗi không tìm thấy -->
    <div v-if="errorMsg && !loading" class="player-error">
      <i class="bi bi-exclamation-triangle display-4 text-warning mb-3"></i>
      <h3>{{ errorMsg }}</h3>
      <router-link to="/phong-thi-nghiem" class="btn btn-primary mt-3">Quay lại Phòng thí nghiệm</router-link>
    </div>
  </div>
</template>

<script>
import Loading from '@/shared/components/Loading.vue'
import LuckyWheelPlayer from '../components/LuckyWheelPlayer.vue'
import FlashcardPlayer from '../components/FlashcardPlayer.vue'
import MillionairePlayer from '../components/MillionairePlayer.vue'
import { labService } from '../services/lab.service'
import { LAB_TEMPLATES, LAB_TEMPLATE_META, LAB_TEXTS, LAB_ROUTES } from '@/shared/utils/constants'
import { alertSuccess, alertError, alertWarning } from '@/shared/utils/swal'

export default {
  name: 'ProductPlayerView',
  components: {
    Loading,
    LuckyWheelPlayer,
    FlashcardPlayer,
    MillionairePlayer
  },
  data() {
    return {
      LAB_ROUTES,
      LAB_TEMPLATES,
      loading: true,
      saving: false,
      product: null,
      rows: [],
      errorMsg: null,
      isFullscreen: false
    }
  },
  computed: {
    templateName() {
      if (!this.product) return ''
      return LAB_TEMPLATE_META[this.product.templateType]?.name || this.product.templateType
    }
  },
  async mounted() {
    await this.loadProductData()
  },
  methods: {
    async loadProductData() {
      const publicId = this.$route.params.publicId
      if (!publicId) {
        this.errorMsg = LAB_TEXTS.PRODUCT_NOT_FOUND
        this.loading = false
        return
      }

      this.loading = true
      this.errorMsg = null

      try {
        const [prodRes, rowsRes] = await Promise.all([
          labService.getPublicProduct(publicId),
          labService.getPublicProductRows(publicId)
        ])
        this.product = prodRes.data
        this.rows = rowsRes.data || []
        document.title = `${this.product.title} | Phòng thí nghiệm`
      } catch (err) {
        this.errorMsg = err.response?.data?.message || LAB_TEXTS.PRODUCT_NOT_FOUND
      } finally {
        this.loading = false
      }
    },
    async saveProduct() {
      const token = localStorage.getItem('token')
      if (!token) {
        alertWarning(LAB_TEXTS.GUEST_LOGIN_HINT)
        return
      }

      this.saving = true
      try {
        const res = await labService.saveProduct(this.product.publicId)
        this.product = res.data
        alertSuccess('Đã lưu sản phẩm thành công vào danh sách của bạn!')
      } catch (err) {
        alertError(err.response?.data?.message || 'Không thể lưu sản phẩm')
      } finally {
        this.saving = false
      }
    },
    copyShareLink() {
      const url = window.location.href
      navigator.clipboard.writeText(url)
        .then(() => alertSuccess('Đã sao chép liên kết vào bộ nhớ tạm!'))
        .catch(() => alertError('Không thể sao chép liên kết'))
    },
    toggleFullscreen() {
      const el = this.$refs.playerWrapper
      if (!document.fullscreenElement) {
        el.requestFullscreen().catch(err => console.error(err))
        this.isFullscreen = true
      } else {
        document.exitFullscreen().catch(err => console.error(err))
        this.isFullscreen = false
      }
    }
  }
}
</script>

<style scoped>
.player-fullscreen-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  color: #ffffff;
  position: relative;
}

.player-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 100;
  position: sticky;
  top: 0;
}

.player-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-back {
  color: #94a3b8;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s ease;
}

.btn-back:hover {
  color: #38bdf8;
}

.product-badge {
  background: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.player-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-action {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #ffffff;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-action:hover {
  background: rgba(255, 255, 255, 0.18);
}

.btn-save {
  background: #f59e0b;
  border-color: #d97706;
  color: #0f172a;
}

.btn-save:hover {
  background: #fbbf24;
}

.player-body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.player-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  text-align: center;
}
</style>
