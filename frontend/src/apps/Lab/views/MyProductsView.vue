<template>
  <div class="container-fluid py-4 my-products-page">
    <div class="container">
      <!-- Breadcrumb / Header -->
      <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
          <h1 class="page-main-title mb-1">
            <i class="bi bi-boxes text-primary me-2"></i>Sản phẩm của tôi
          </h1>
          <p class="text-muted small mb-0">Quản lý các trò chơi, ứng dụng tương tác bạn đã tạo từ Phòng thí nghiệm</p>
        </div>
        <router-link :to="LAB_ROUTES.HOME" class="btn btn-primary">
          <i class="bi bi-chat-dots me-1"></i> Tạo sản phẩm mới
        </router-link>
      </div>

      <!-- Bảng dữ liệu chuẩn DataTable -->
      <DataTable
        title="Danh sách sản phẩm tương tác"
        :headers="tableHeaders"
        :items="products"
        :totalItems="totalProducts"
        :loading="loading"
        :pageSize="pageSize"
        :currentPage="currentPage"
        placeholder="Tìm kiếm theo tên sản phẩm..."
        :showAddButton="false"
        @update:currentPage="onPageChange"
        @update:pageSize="onPageSizeChange"
        @search="onSearch"
        @view="openPlayer"
        @edit="openEditModal"
        @delete="handleDeleteProduct"
      >
        <!-- Custom Tên sản phẩm -->
        <template #item-title="{ item }">
          <div class="fw-bold text-dark">{{ item.title }}</div>
          <div class="text-muted small">Mã: <code>{{ item.publicId }}</code> (v{{ item.version }})</div>
        </template>

        <!-- Custom Khuôn mẫu -->
        <template #item-templateType="{ item }">
          <span class="badge bg-light text-dark border">
            {{ getTemplateIcon(item.templateType) }} {{ getTemplateName(item.templateType) }}
          </span>
        </template>

        <!-- Custom Quyền riêng tư -->
        <template #item-isPublic="{ item }">
          <span v-if="item.isPublic" class="badge bg-success">
            <i class="bi bi-globe me-1"></i>Công khai
          </span>
          <span v-else class="badge bg-secondary">
            <i class="bi bi-lock me-1"></i>Riêng tư
          </span>
        </template>

        <!-- Custom Cập nhật -->
        <template #item-updatedAt="{ item }">
          <span class="small text-muted">{{ formatForumDate(item.updatedAt) }}</span>
        </template>

        <!-- Nút thao tác bổ sung: Cấu hình dữ liệu & Copy Link -->
        <template #extra-actions="{ item }">
          <button
            class="action-btn config-btn"
            @click="goToDatasetConfig(item.publicId)"
            title="Cấu hình bảng dữ liệu"
          >
            📊
          </button>
          <button
            class="action-btn copy-btn"
            @click="copyProductLink(item.publicId)"
            title="Sao chép đường dẫn chia sẻ"
          >
            🔗
          </button>
        </template>
      </DataTable>
    </div>

    <!-- Modal chỉnh sửa thông tin sản phẩm -->
    <div v-if="showEditModal" class="modal-backdrop-custom" @click="closeEditModal">
      <div class="modal-card-custom" @click.stop>
        <div class="modal-header-custom">
          <h5>Chỉnh sửa sản phẩm</h5>
          <button class="btn-close-custom" @click="closeEditModal">&times;</button>
        </div>
        <div class="modal-body-custom">
          <div class="mb-3">
            <label class="form-label fw-bold">Tên sản phẩm</label>
            <input
              type="text"
              class="form-control"
              v-model="editForm.title"
              placeholder="Nhập tên sản phẩm..."
            />
          </div>
          <div class="form-check form-switch mb-3">
            <input
              class="form-check-input"
              type="checkbox"
              id="switchPublic"
              v-model="editForm.isPublic"
            />
            <label class="form-check-label fw-bold" for="switchPublic">
              {{ editForm.isPublic ? 'Chia sẻ công khai (Ai có link đều xem được)' : 'Chỉ mình tôi xem được' }}
            </label>
          </div>
        </div>
        <div class="modal-footer-custom">
          <button class="btn btn-secondary btn-sm" @click="closeEditModal">Hủy</button>
          <button class="btn btn-primary btn-sm" :disabled="saving" @click="submitEdit">
            {{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import DataTable from '@/shared/components/DataTable.vue'
import { labService } from '../services/lab.service'
import { LAB_TEMPLATE_META, LAB_ROUTES } from '@/shared/utils/constants'
import { alertConfirm, alertError, alertSuccess } from '@/shared/utils/swal'
import { formatForumDate } from '@/shared/utils/date'

export default {
  name: 'MyProductsView',
  components: { DataTable },
  data() {
    return {
      LAB_ROUTES,
      products: [],
      totalProducts: 0,
      loading: true,
      saving: false,
      currentPage: 1,
      pageSize: 10,
      keyword: '',
      showEditModal: false,
      editForm: {
        publicId: '',
        title: '',
        isPublic: true
      },
      tableHeaders: [
        { text: 'Tên sản phẩm', value: 'title', width: '320px', sortable: false },
        { text: 'Khuôn mẫu', value: 'templateType', width: '200px', sortable: false },
        { text: 'Quyền riêng tư', value: 'isPublic', width: '150px', sortable: false },
        { text: 'Cập nhật', value: 'updatedAt', width: '180px', sortable: false }
      ]
    }
  },
  async mounted() {
    await this.fetchProducts()
  },
  methods: {
    formatForumDate,
    getTemplateIcon(type) {
      return LAB_TEMPLATE_META[type]?.icon || '📦'
    },
    getTemplateName(type) {
      return LAB_TEMPLATE_META[type]?.name || type
    },
    async fetchProducts() {
      this.loading = true
      try {
        const res = await labService.getMyProducts({
          page: this.currentPage - 1,
          size: this.pageSize
        })
        const data = res.data || {}
        this.products = data.content || []
        this.totalProducts = data.totalElements || 0
      } catch (err) {
        alertError('Không thể tải danh sách sản phẩm: ' + (err.response?.data?.message || err.message))
      } finally {
        this.loading = false
      }
    },
    onPageChange(page) {
      this.currentPage = page
      this.fetchProducts()
    },
    onPageSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.fetchProducts()
    },
    onSearch(kw) {
      this.keyword = kw
      this.currentPage = 1
      this.fetchProducts()
    },
    openPlayer(item) {
      window.open(`${LAB_ROUTES.PLAYER_PREFIX}${item.publicId}`, '_blank')
    },
    goToDatasetConfig(publicId) {
      this.$router.push(`${LAB_ROUTES.MY_PRODUCTS}/${publicId}/du-lieu`)
    },
    copyProductLink(publicId) {
      const url = `${window.location.origin}${LAB_ROUTES.PLAYER_PREFIX}${publicId}`
      navigator.clipboard.writeText(url)
        .then(() => alertSuccess('Đã sao chép liên kết vào bộ nhớ tạm!'))
        .catch(() => alertError('Không thể sao chép liên kết'))
    },
    openEditModal(item) {
      this.editForm = {
        publicId: item.publicId,
        title: item.title,
        isPublic: item.isPublic
      }
      this.showEditModal = true
    },
    closeEditModal() {
      this.showEditModal = false
    },
    async submitEdit() {
      if (!this.editForm.title.trim()) {
        alertError('Tên sản phẩm không được để trống')
        return
      }
      this.saving = true
      try {
        await labService.updateProduct(this.editForm.publicId, {
          title: this.editForm.title.trim(),
          isPublic: this.editForm.isPublic
        })
        alertSuccess('Cập nhật sản phẩm thành công!')
        this.closeEditModal()
        await this.fetchProducts()
      } catch (err) {
        alertError(err.response?.data?.message || 'Lỗi khi cập nhật sản phẩm')
      } finally {
        this.saving = false
      }
    },
    async handleDeleteProduct(item) {
      const confirm = await alertConfirm(
        'Xác nhận xoá sản phẩm',
        `Bạn có chắc chắn muốn xoá sản phẩm "${item.title}"? Thao tác này sẽ xoá luôn toàn bộ dòng dữ liệu và phiên trò chuyện tương ứng!`
      )
      if (!confirm.isConfirmed) return

      try {
        await labService.deleteProduct(item.publicId)
        alertSuccess('Đã xoá sản phẩm thành công!')
        await this.fetchProducts()
      } catch (err) {
        alertError(err.response?.data?.message || 'Không thể xoá sản phẩm')
      }
    }
  }
}
</script>

<style scoped>
.my-products-page {
  background: var(--bg-body, #eceef1);
  min-height: calc(100vh - 120px);
}

.page-main-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: #1a507a;
}

.action-btn.config-btn {
  background: #f0fdf4;
  border-color: #bbf7d0;
  color: #16a34a;
}

.action-btn.copy-btn {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #2563eb;
}

/* Modal Styling */
.modal-backdrop-custom {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1rem;
}

.modal-card-custom {
  background: #ffffff;
  border-radius: 12px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.modal-header-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e2e8f0;
}

.btn-close-custom {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  color: #64748b;
  cursor: pointer;
}

.modal-body-custom {
  padding: 1.25rem;
}

.modal-footer-custom {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 1rem 1.25rem;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}
</style>
