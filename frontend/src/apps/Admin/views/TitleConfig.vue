<template>
  <div class="page-content">
    <!-- Thống kê & Banner tổng quan phía trên -->
    <div class="stats-grid mb-4">
      <!-- Card 1: Trạng thái Chưa xác thực -->
      <div class="stat-card stat-unverified">
        <div class="stat-icon-wrapper">
          <i class="fas fa-exclamation-triangle stat-icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-label">Danh hiệu Chưa Xác Thực</div>
          <div class="stat-value text-warning">
            {{ unverifiedTitle ? unverifiedTitle.name : 'Chưa thiết lập' }}
          </div>
          <div class="stat-desc">Tự động gán khi đệ tử mới đăng ký chưa xác thực email</div>
        </div>
        <div class="stat-action">
          <button 
            v-if="unverifiedTitle" 
            class="btn-stat-action btn-stat-edit" 
            @click="openEditModal(unverifiedTitle)"
            title="Chỉnh sửa danh hiệu chưa xác thực"
          >
            ✏️ Sửa
          </button>
          <button 
            v-else 
            class="btn-stat-action btn-stat-add" 
            @click="openAddModalWithType(TITLE_TYPES.UNVERIFIED_DEFAULT)"
          >
            + Thiết lập
          </button>
        </div>
      </div>

      <!-- Card 2: Cấp bậc Theo Mốc điểm -->
      <div class="stat-card stat-points">
        <div class="stat-icon-wrapper">
          <i class="fas fa-trophy stat-icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-label">Cấp bậc Theo Mốc Điểm</div>
          <div class="stat-value text-primary">{{ pointBasedTitles.length }} cấp bậc</div>
          <div class="stat-desc">Xếp hạng tự động dựa trên mốc điểm tích lũy (trophyPoints)</div>
        </div>
      </div>

      <!-- Card 3: Cấp bậc Tự do -->
      <div class="stat-card stat-custom">
        <div class="stat-icon-wrapper">
          <i class="fas fa-award stat-icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-label">Cấp bậc Tự Do (Admin Gán)</div>
          <div class="stat-value text-success">{{ customTitles.length }} danh hiệu</div>
          <div class="stat-desc">Danh hiệu đặc biệt Admin trực tiếp trao cho từng đệ tử</div>
        </div>
      </div>
    </div>

    <!-- DataTable Quản lý Cấp bậc -->
    <DataTable
      title="Quản lý Cấp bậc (User Titles)"
      placeholder="Tìm kiếm tên cấp bậc, mô tả..."
      addButtonLabel="Thêm cấp bậc mới"
      :headers="headers"
      :items="displayTitles"
      :totalItems="filteredTitles.length"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="loading"
      @search="handleSearch"
      @add="openAddModal"
      @edit="openEditModal"
      @delete="deleteTitle"
      @sort="handleSort"
    >
      <!-- Slot cho Bộ lọc Loại cấp bậc ở phần Header DataTable -->
      <template #extra-filters>
        <div class="filter-select-wrapper">
          <select v-model="selectedTypeFilter" class="form-select filter-select">
            <option value="ALL">-- Tất cả loại cấp bậc --</option>
            <option :value="TITLE_TYPES.POINT_BASED">Theo mốc điểm (POINT_BASED)</option>
            <option :value="TITLE_TYPES.CUSTOM_ASSIGNABLE">Cấp bậc tự do (CUSTOM_ASSIGNABLE)</option>
            <option :value="TITLE_TYPES.UNVERIFIED_DEFAULT">Chưa xác thực (UNVERIFIED_DEFAULT)</option>
          </select>
        </div>
      </template>

      <!-- Slot cho cột Tên Cấp bậc (giao diện Badge) -->
      <template #item-name="{ item }">
        <div class="title-name-cell">
          <span class="title-badge-preview" :class="getTypeBadgeClass(item.type)">
            {{ item.name }}
          </span>
        </div>
      </template>

      <!-- Slot cho cột Loại Cấp bậc -->
      <template #item-type="{ item }">
        <span class="badge" :class="getTypeBadgeClass(item.type)">
          {{ getTypeName(item.type) }}
        </span>
      </template>

      <!-- Slot cho cột Mốc điểm tối thiểu -->
      <template #item-minPoints="{ item }">
        <div v-if="item.type === TITLE_TYPES.POINT_BASED" class="points-badge-cell">
          <span class="badge-points-tag">
            ≥ {{ item.minPoints || 0 }} điểm
          </span>
        </div>
        <span v-else class="text-muted">—</span>
      </template>

      <!-- Slot cho cột Mô tả -->
      <template #item-description="{ item }">
        <span class="text-muted small">{{ item.description || '---' }}</span>
      </template>
    </DataTable>

    <!-- Modal Thêm/Sửa Cấp bậc -->
    <BaseModal 
      v-model:show="showModal" 
      :title="isEdit ? 'SỬA CẤP BẬC' : 'THÊM CẤP BẬC MỚI'"
    >
      <div class="admin-form">
        <div class="form-group">
          <label>Tên Cấp Bậc <span class="text-danger">*</span></label>
          <input 
            type="text" 
            class="form-control" 
            v-model="formData.name" 
            placeholder="VD: Tân thủ, Tập sự, Cao thủ, VIP..." 
          />
        </div>

        <div class="form-group">
          <label>Loại Cấp Bậc <span class="text-danger">*</span></label>
          <select class="form-select" v-model="formData.type">
            <option :value="TITLE_TYPES.POINT_BASED">Theo mốc điểm (Gán tự động dựa trên trophyPoints)</option>
            <option :value="TITLE_TYPES.CUSTOM_ASSIGNABLE">Tự do (Gán thủ công bởi Admin trong Quản lý người dùng)</option>
            <option :value="TITLE_TYPES.UNVERIFIED_DEFAULT">Chưa xác thực (Mặc định cho tài khoản chưa confirm email)</option>
          </select>
          <small class="form-text text-muted mt-1">
            {{ getTypeHelpText(formData.type) }}
          </small>
        </div>

        <div v-if="formData.type === TITLE_TYPES.POINT_BASED" class="form-group">
          <label>Mốc điểm tối thiểu (trophyPoints) <span class="text-danger">*</span></label>
          <input 
            type="number" 
            class="form-control" 
            v-model.number="formData.minPoints" 
            min="0" 
            placeholder="VD: 0, 100, 500..." 
          />
          <small class="form-text text-muted">Đệ tử đạt từ mốc điểm này trở lên sẽ tự động nhận danh hiệu.</small>
        </div>

        <div class="form-group">
          <label>Mô tả Cấp Bậc</label>
          <textarea 
            class="form-control" 
            v-model="formData.description" 
            rows="2" 
            placeholder="Ghi chú thêm về ý nghĩa hoặc điều kiện nhận danh hiệu..."
          ></textarea>
        </div>

        <!-- Khối Xem trước Live Preview -->
        <div class="preview-section mb-3" v-if="formData.name">
          <label>Xem trước hiển thị danh hiệu:</label>
          <div class="preview-box-active">
            <div class="simulated-badge-box">
              <span class="title-badge-preview" :class="getTypeBadgeClass(formData.type)">
                {{ formData.name }}
              </span>
              <span v-if="formData.type === TITLE_TYPES.POINT_BASED" class="preview-points-hint">
                (Điểm tối thiểu: ≥ {{ formData.minPoints || 0 }})
              </span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="showModal = false">Hủy</button>
          <button class="btn-save" @click="saveTitle" :disabled="saving">
            {{ saving ? 'Đang lưu...' : (isEdit ? 'Cập nhật' : 'Lưu lại') }}
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script>
import DataTable from '@/shared/components/DataTable.vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import TitleService from '@/apps/Admin/services/title.service'
import { TITLE_TYPES } from '@/shared/utils/constants'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'TitleConfig',
  components: {
    DataTable,
    BaseModal
  },
  data() {
    return {
      TITLE_TYPES,
      titles: [],
      loading: false,
      saving: false,
      showModal: false,
      isEdit: false,
      
      // Pagination & Filter state
      keyword: '',
      selectedTypeFilter: 'ALL',
      currentPage: 1,
      pageSize: 10,
      sortField: 'minPoints',
      sortOrder: 'asc',

      // Data Headers for DataTable
      headers: [
        { text: 'Tên Cấp Bậc', value: 'name', width: '220px', sortable: true },
        { text: 'Loại Cấp Bậc', value: 'type', width: '180px', sortable: true },
        { text: 'Mốc Điểm Tối Thiểu', value: 'minPoints', width: '180px', sortable: true },
        { text: 'Mô Tả', value: 'description', sortable: false }
      ],

      formData: {
        id: null,
        name: '',
        type: TITLE_TYPES.POINT_BASED,
        minPoints: 0,
        description: ''
      }
    }
  },
  computed: {
    unverifiedTitle() {
      return this.titles.find(t => t.type === TITLE_TYPES.UNVERIFIED_DEFAULT)
    },
    pointBasedTitles() {
      return this.titles
        .filter(t => t.type === TITLE_TYPES.POINT_BASED)
        .sort((a, b) => (a.minPoints || 0) - (b.minPoints || 0))
    },
    customTitles() {
      return this.titles.filter(t => t.type === TITLE_TYPES.CUSTOM_ASSIGNABLE)
    },
    filteredTitles() {
      let result = [...this.titles]

      // Filter by type dropdown
      if (this.selectedTypeFilter && this.selectedTypeFilter !== 'ALL') {
        result = result.filter(t => t.type === this.selectedTypeFilter)
      }

      // Search by keyword
      if (this.keyword && this.keyword.trim()) {
        const kw = this.keyword.trim().toLowerCase()
        result = result.filter(t => 
          (t.name && t.name.toLowerCase().includes(kw)) ||
          (t.description && t.description.toLowerCase().includes(kw))
        )
      }

      // Sorting
      if (this.sortField) {
        result.sort((a, b) => {
          let valA = a[this.sortField]
          let valB = b[this.sortField]

          if (valA === null || valA === undefined) valA = ''
          if (valB === null || valB === undefined) valB = ''

          if (typeof valA === 'number' && typeof valB === 'number') {
            return this.sortOrder === 'asc' ? valA - valB : valB - valA
          }

          return this.sortOrder === 'asc' 
            ? String(valA).localeCompare(String(valB)) 
            : String(valB).localeCompare(String(valA))
        })
      }

      return result
    },
    displayTitles() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredTitles.slice(start, start + this.pageSize)
    }
  },
  mounted() {
    this.fetchTitles()
  },
  methods: {
    async fetchTitles() {
      this.loading = true
      try {
        const res = await TitleService.getAllTitles()
        this.titles = res.data || []
      } catch (err) {
        console.error('Lỗi tải danh sách cấp bậc:', err)
        toastError('Khởi tạo danh sách cấp bậc thất bại')
      } finally {
        this.loading = false
      }
    },

    handleSearch(kw) {
      this.keyword = kw
      this.currentPage = 1
    },

    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
    },

    getTypeBadgeClass(type) {
      switch (type) {
        case TITLE_TYPES.POINT_BASED:
          return 'badge-type-point'
        case TITLE_TYPES.CUSTOM_ASSIGNABLE:
          return 'badge-type-custom'
        case TITLE_TYPES.UNVERIFIED_DEFAULT:
          return 'badge-type-unverified'
        default:
          return 'badge-secondary'
      }
    },

    getTypeName(type) {
      switch (type) {
        case TITLE_TYPES.POINT_BASED:
          return 'Theo mốc điểm'
        case TITLE_TYPES.CUSTOM_ASSIGNABLE:
          return 'Tự do (Admin gán)'
        case TITLE_TYPES.UNVERIFIED_DEFAULT:
          return 'Chưa xác thực'
        default:
          return type || 'Không xác định'
      }
    },

    getTypeHelpText(type) {
      switch (type) {
        case TITLE_TYPES.POINT_BASED:
          return 'Danh hiệu tự động sắp xếp và hiển thị dựa trên điểm tích lũy của đệ tử.'
        case TITLE_TYPES.CUSTOM_ASSIGNABLE:
          return 'Danh hiệu đặc biệt chỉ xuất hiện khi Admin gán trực tiếp cho đệ tử trong Quản lý Người dùng.'
        case TITLE_TYPES.UNVERIFIED_DEFAULT:
          return 'Danh hiệu tự động được gắn cho đệ tử mới đăng ký tài khoản nhưng chưa xác thực email.'
        default:
          return ''
      }
    },

    openAddModal() {
      this.isEdit = false
      this.formData = {
        id: null,
        name: '',
        type: TITLE_TYPES.POINT_BASED,
        minPoints: 0,
        description: ''
      }
      this.showModal = true
    },

    openAddModalWithType(type) {
      this.openAddModal()
      this.formData.type = type
    },

    openEditModal(title) {
      this.isEdit = true
      this.formData = {
        id: title.id,
        name: title.name,
        type: title.type,
        minPoints: title.minPoints != null ? title.minPoints : 0,
        description: title.description || ''
      }
      this.showModal = true
    },

    async saveTitle() {
      if (!this.formData.name || !this.formData.name.trim()) {
        toastError('Vui lòng nhập tên cấp bậc')
        return
      }

      this.saving = true
      try {
        if (this.isEdit) {
          await TitleService.updateTitle(this.formData.id, this.formData)
          toastSuccess('Cập nhật cấp bậc thành công!')
        } else {
          await TitleService.createTitle(this.formData)
          toastSuccess('Tạo mới cấp bậc thành công!')
        }
        this.showModal = false
        this.fetchTitles()
      } catch (err) {
        toastError(err.response?.data?.message || 'Có lỗi xảy ra khi lưu cấp bậc')
      } finally {
        this.saving = false
      }
    },

    async deleteTitle(title) {
      const titleId = title.id || title
      const titleName = title.name || `#${titleId}`

      const confirmResult = await alertConfirm(
        'Xác nhận xóa',
        `Con có chắc chắn muốn xóa Cấp bậc "${titleName}" không?`
      )
      if (!confirmResult.isConfirmed) return

      try {
        await TitleService.deleteTitle(titleId)
        toastSuccess('Xóa cấp bậc thành công!')
        this.fetchTitles()
      } catch (err) {
        toastError(err.response?.data?.message || 'Có lỗi xảy ra khi xóa cấp bậc')
      }
    }
  }
}
</script>

<style scoped>
.page-content {
  padding: 1.5rem;
}

/* Stats Summary Cards */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.25rem;
}

.stat-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  border: 1px solid #eef2f5;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.stat-icon-wrapper {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-unverified .stat-icon-wrapper {
  background: #fff8e6;
  color: #f39c12;
}

.stat-points .stat-icon-wrapper {
  background: #e8f4ff;
  color: #3498db;
}

.stat-custom .stat-icon-wrapper {
  background: #eafaf1;
  color: #2ecc71;
}

.stat-icon {
  font-size: 1.5rem;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 0.85rem;
  color: #7f8c8d;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  margin: 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-desc {
  font-size: 0.78rem;
  color: #95a5a6;
  line-height: 1.3;
}

.stat-action {
  flex-shrink: 0;
}

.btn-stat-action {
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-stat-edit {
  background: #fff3cd;
  color: #856404;
}

.btn-stat-edit:hover {
  background: #ffe8a1;
}

.btn-stat-add {
  background: #d1ecf1;
  color: #0c5460;
}

.btn-stat-add:hover {
  background: #bee5eb;
}

/* Extra Filter Dropdown */
.filter-select-wrapper {
  min-width: 220px;
}

.filter-select {
  border-radius: 6px;
  border: 1px solid #ced4da;
  font-size: 0.9rem;
  padding: 0.4rem 0.75rem;
}

/* Title Badge Styling */
.title-name-cell {
  display: flex;
  align-items: center;
}

.title-badge-preview {
  display: inline-block;
  padding: 0.35em 0.8em;
  font-size: 0.85rem;
  font-weight: 700;
  border-radius: 6px;
  letter-spacing: 0.3px;
}

.badge-type-point {
  background-color: #e8f4ff;
  color: #1b6ec2;
  border: 1px solid #bce0fd;
}

.badge-type-custom {
  background-color: #eafaf1;
  color: #1e7e34;
  border: 1px solid #c3e6cb;
}

.badge-type-unverified {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeeba;
}

/* Points Badge Cell */
.points-badge-cell {
  display: flex;
  align-items: center;
}

.badge-points-tag {
  background: #f8f9fa;
  color: #2c3e50;
  border: 1px solid #dcdfe6;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  font-size: 0.82rem;
  font-weight: 600;
}

/* Modal Form Custom Styling */
.admin-form {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.admin-form .form-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.admin-form label {
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

.admin-form .form-control,
.admin-form .form-select {
  border-radius: 6px;
  border: 1px solid #ccc;
  padding: 0.55rem 0.75rem;
  font-size: 0.92rem;
}

.admin-form .form-control:focus,
.admin-form .form-select:focus {
  border-color: #3498db;
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.15);
  outline: none;
}

/* Preview Section in Modal */
.preview-section {
  padding: 1rem 1.25rem;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px dashed #ced4da;
  margin-top: 0.5rem;
}

.preview-box-active {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
}

.simulated-badge-box {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  padding: 0.75rem 1.25rem;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.preview-points-hint {
  font-size: 0.82rem;
  color: #6c757d;
  font-style: italic;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 1.25rem;
  border-top: 1px solid #eee;
  margin-top: 0.5rem;
}

.btn-save {
  background: #27ae60;
  color: white;
  border: none;
  padding: 0.65rem 1.75rem;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 700;
  font-size: 0.92rem;
  transition: background 0.15s;
}

.btn-save:hover {
  background: #219150;
}

.btn-save:disabled {
  background: #a5d6a7;
  cursor: not-allowed;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
  border: none;
  padding: 0.65rem 1.25rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.92rem;
  transition: background 0.15s;
}

.btn-cancel:hover {
  background: #7f8c8d;
}

code {
  background: #f1f1f1;
  padding: 2px 6px;
  border-radius: 3px;
  color: #e83e8c;
  font-size: 0.9em;
}
</style>
