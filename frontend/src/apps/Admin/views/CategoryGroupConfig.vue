<template>
  <div class="page-content">
    <Loading :visible="isSubmitting" />
    <DataTable
      title="Quản lý Nhóm chuyên mục"
      placeholder="Tìm kiếm tên nhóm..."
      addButtonLabel="Thêm nhóm mới"
      :headers="headers"
      :items="displayGroups"
      :totalItems="filteredGroups.length"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="isTableLoading"
      @search="handleSearch"
      @add="openAddModal"
      @edit="openEditModal"
      @delete="deleteGroup"
      @view="openEditModal"
      @sort="handleSort"
    >
      <template #divAction>
        <button class="btn-add-new" @click="openAddModal">
          <span class="icon">+</span> Thêm nhóm mới
        </button>
        <button 
          type="button" 
          class="btn-seed-data" 
          @click="openSeedModal"
          :disabled="isTableLoading"
          title="Nạp bộ nhóm chuyên mục và chuyên mục chuẩn cho diễn đàn"
        >
          <span class="icon">🌱</span> Nạp danh mục mẫu
        </button>
        <button 
          type="button" 
          class="btn-delete-all" 
          @click="deleteAllGroups" 
          :disabled="isTableLoading || filteredGroups.length === 0"
          title="Xóa tất cả nhóm chuyên mục"
        >
          <span class="icon">🗑️</span> Xóa tất cả
        </button>
      </template>

      <template #extra-actions="{ item }">
        <button class="action-btn cat-btn" @click="openCategoryModal(item)" title="Quản lý chuyên mục">📁</button>
      </template>

      <template #item-name="{ item }">
        <strong>{{ item.name }}</strong>
      </template>

      <template #item-active="{ item }">
        <span :class="['badge', item.active ? 'badge-success' : 'badge-danger']">
          {{ item.active ? 'Hoạt động' : 'Tắt' }}
        </span>
      </template>
    </DataTable>

    <!-- Modal Form Nhóm -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-card">
        <div class="card-header">{{ isEditing ? 'CẬP NHẬT NHÓM' : 'THÊM NHÓM MỚI' }}</div>
        <form @submit.prevent="handleSubmit" class="admin-form">
          <div class="form-group">
            <label>Tên nhóm:</label>
            <input v-model="form.name" required>
          </div>
          <div class="form-group">
            <label>Thứ tự hiển thị:</label>
            <input type="number" v-model="form.positionOrder">
          </div>
          <div class="form-group checkbox-group">
            <input type="checkbox" v-model="form.active" id="g-active">
            <label for="g-active">Kích hoạt</label>
          </div>
          <div class="modal-footer">
            <button type="button" @click="showModal = false" class="btn-cancel">Đóng</button>
            <button type="submit" class="btn-save">{{ isEditing ? 'Cập nhật' : 'Lưu lại' }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal Nạp Danh Mục Mẫu từ SQL -->
    <div v-if="showSeedModal" class="modal-overlay">
      <div class="modal-card seed-modal-card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <span>🌱 NẠP BỘ DANH MỤC & NHÃN TỪ FILE SQL</span>
          <button type="button" class="close-x-btn" @click="showSeedModal = false">✕</button>
        </div>
        <div class="modal-body p-4">
          <div class="seed-intro mb-3">
            <p class="mb-1"><strong>Cơ chế tự động hóa:</strong> Hệ thống sẽ đọc file SQL và nạp <strong>5 Đại nhóm, 14 Chuyên mục</strong> (kèm icon tương ứng) và <strong>10 Nhãn bài viết</strong>.</p>
            <p class="text-muted small mb-0">Cú pháp được tối ưu an toàn (không làm trùng lặp, không xóa bài viết cũ của diễn đàn).</p>
          </div>

          <div class="seed-option-card mb-3" :class="{ active: seedMode === 'default' }" @click="seedMode = 'default'">
            <div class="d-flex align-items-center gap-3">
              <input type="radio" v-model="seedMode" value="default" id="opt-default">
              <div>
                <label for="opt-default" class="fw-bold mb-0 cursor-pointer">Sử dụng bộ danh mục chuẩn mặc định của hệ thống</label>
                <div class="small text-muted">Tự động nạp file <code>seed_default_categories.sql</code> có sẵn trên máy chủ.</div>
              </div>
            </div>
          </div>

          <div class="seed-option-card mb-3" :class="{ active: seedMode === 'custom' }" @click="seedMode = 'custom'">
            <div class="d-flex align-items-start gap-3">
              <input type="radio" v-model="seedMode" value="custom" id="opt-custom" class="mt-1">
              <div class="flex-grow-1">
                <label for="opt-custom" class="fw-bold mb-0 cursor-pointer">Tải lên file .sql tùy chỉnh từ máy tính</label>
                <div class="small text-muted mb-2">Cho phép con tự sửa đổi file SQL và tải lên nạp lại theo ý muốn.</div>
                
                <div v-if="seedMode === 'custom'" class="upload-box mt-2" @click.stop>
                  <input type="file" ref="fileInput" accept=".sql" @change="onFileSelected" class="form-control-file">
                  <div v-if="selectedFile" class="selected-file-info mt-2">
                    📄 Đã chọn: <strong>{{ selectedFile.name }}</strong> ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="template-download-bar p-3 rounded mb-3 d-flex justify-content-between align-items-center">
            <div>
              <span class="text-secondary small">💡 Cần file mẫu để chỉnh sửa?</span>
            </div>
            <button type="button" class="btn-download-tpl" @click="downloadTemplate" :disabled="isSubmitting">
              📥 Tải file .sql mẫu về máy
            </button>
          </div>

          <div class="modal-footer px-0 pb-0">
            <button type="button" @click="showSeedModal = false" class="btn-cancel" :disabled="isSubmitting">Đóng</button>
            <button type="button" class="btn-save btn-primary-action" @click="handleExecuteSeed" :disabled="isSubmitting || (seedMode === 'custom' && !selectedFile)">
              {{ isSubmitting ? 'Đang nạp...' : 'Bắt đầu nạp dữ liệu' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AdminService from '@/apps/Admin/services/admin.service'
import DataTable from '@/shared/components/DataTable.vue'
import Loading from '@/shared/components/Loading.vue'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'CategoryGroupConfig',
  components: { DataTable, Loading },
  data() {
    return {
      groups: [],
      loading: false,
      isSubmitting: false,
      keyword: '',
      pageSize: 10,
      currentPage: 1,
      
      // Group Modal
      showModal: false,
      isEditing: false,
      headers: [
        { text: 'Tên nhóm', value: 'name', sortable: true },
        { text: 'Thứ tự', value: 'positionOrder', sortable: true, width: '100px' },
        { text: 'Trạng thái', value: 'active', sortable: true, width: '120px' }
      ],
      form: { id: null, name: '', positionOrder: 0, active: true },
      
      sortField: 'positionOrder',
      sortOrder: 'asc',

      // Seed Modal
      showSeedModal: false,
      seedMode: 'default',
      selectedFile: null
    }
  },
  computed: {
    isTableLoading() {
      return this.loading || this.isSubmitting
    },
    // Groups Logic
    filteredGroups() {
      let result = this.groups
      if (this.keyword) {
        const k = this.keyword.trim().toLowerCase()
        result = result.filter(g => g.name && g.name.toLowerCase().includes(k))
      }
      if (this.sortField) {
        result = [...result].sort((a, b) => {
          let valA = a[this.sortField]
          let valB = b[this.sortField]
          if (typeof valA === 'string') valA = valA.toLowerCase()
          if (typeof valB === 'string') valB = valB.toLowerCase()
          if (valA < valB) return this.sortOrder === 'asc' ? -1 : 1
          if (valA > valB) return this.sortOrder === 'asc' ? 1 : -1
          return 0
        })
      }
      return result
    },
    displayGroups() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredGroups.slice(start, end)
    }
  },
  mounted() {
    this.fetchGroups()
  },
  methods: {
    async fetchGroups() {
      this.loading = true
      try {
        const response = await AdminService.getCategoryGroups()
        this.groups = response.data
      } catch (error) {
        console.error('Lỗi khi tải nhóm chuyên mục:', error)
      } finally {
        this.loading = false
      }
    },
    handleSearch(k) {
      this.loading = true
      this.keyword = k
      this.currentPage = 1
      this.$nextTick(() => {
        this.loading = false
      })
    },
    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
    },
    
    // Group Actions
    openAddModal() {
      this.resetForm()
      this.showModal = true
    },
    openEditModal(item) {
      this.form = { ...item }
      this.isEditing = true
      this.showModal = true
    },
    async handleSubmit() {
      this.isSubmitting = true
      try {
        if (this.isEditing) {
          await AdminService.updateCategoryGroup(this.form.id, this.form)
          toastSuccess('Cập nhật nhóm thành công')
        } else {
          await AdminService.createCategoryGroup(this.form)
          toastSuccess('Thêm nhóm thành công')
        }
        this.showModal = false
        this.fetchGroups()
      } catch (error) {
        toastError('Lỗi khi lưu dữ liệu')
      } finally {
        this.isSubmitting = false
      }
    },
    async deleteGroup(item) {
      const result = await alertConfirm('Xóa nhóm', `Bạn có chắc chắn muốn xóa nhóm "${item.name}"?`)
      if (result.isConfirmed) {
        this.isSubmitting = true
        try {
          await AdminService.deleteCategoryGroup(item.id)
          toastSuccess('Đã xóa nhóm')
          this.fetchGroups()
        } catch (error) {
          toastError('Lỗi khi xóa nhóm')
        } finally {
          this.isSubmitting = false
        }
      }
    },
    async deleteAllGroups() {
      const result = await alertConfirm(
        'CẢNH BÁO NGUY HIỂM!',
        'Hành động này sẽ XÓA TOÀN BỘ nhóm chuyên mục (kể cả toàn bộ chuyên mục và bài viết bên trong)! Bạn có chắc chắn muốn xóa không?'
      )
      if (result.isConfirmed) {
        this.isSubmitting = true
        try {
          await AdminService.deleteAllCategoryGroups()
          toastSuccess('Đã xóa tất cả nhóm chuyên mục thành công')
          this.currentPage = 1
          this.fetchGroups()
        } catch (error) {
          toastError(error.response?.data?.message || 'Lỗi khi xóa tất cả nhóm chuyên mục')
        } finally {
          this.isSubmitting = false
        }
      }
    },
    resetForm() {
      this.form = { id: null, name: '', positionOrder: 0, active: true }
      this.isEditing = false
    },

    // Category Navigation
    openCategoryModal(group) {
      this.$router.push({ name: 'AdminCategoryGroupDetail', params: { groupId: group.id } })
    },

    // Seed Data Actions
    openSeedModal() {
      this.seedMode = 'default'
      this.selectedFile = null
      this.showSeedModal = true
    },
    onFileSelected(e) {
      const file = e.target.files?.[0]
      if (file) {
        if (!file.name.endsWith('.sql')) {
          toastError('Vui lòng chọn file có định dạng .sql')
          e.target.value = ''
          this.selectedFile = null
          return
        }
        this.selectedFile = file
      }
    },
    async downloadTemplate() {
      try {
        const response = await AdminService.downloadCategoryTemplateSql()
        const blob = new Blob([response.data], { type: 'application/sql' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'seed_default_categories.sql')
        document.body.appendChild(link)
        link.click()
        link.remove()
        window.URL.revokeObjectURL(url)
        toastSuccess('Tải file .sql mẫu thành công')
      } catch (error) {
        toastError('Lỗi khi tải file mẫu')
      }
    },
    async handleExecuteSeed() {
      const isDefault = this.seedMode === 'default'
      const title = isDefault ? 'Nạp danh mục mẫu chuẩn' : 'Nạp dữ liệu từ file SQL'
      const msg = isDefault 
        ? 'Hệ thống sẽ nạp 5 Đại nhóm, 14 Chuyên mục và 10 Nhãn bài viết chuẩn. Dữ liệu bài viết cũ vẫn được bảo toàn an toàn. Bạn có muốn tiếp tục?'
        : `Hệ thống sẽ thực thi các câu lệnh trong file "${this.selectedFile?.name}". Bạn có muốn tiếp tục?`
      
      const confirm = await alertConfirm(title, msg)
      if (!confirm.isConfirmed) return

      this.isSubmitting = true
      try {
        const formData = new FormData()
        if (isDefault) {
          formData.append('useDefault', 'true')
        } else {
          formData.append('file', this.selectedFile)
          formData.append('useDefault', 'false')
        }

        await AdminService.importCategoriesSql(formData)
        toastSuccess('Nạp dữ liệu từ file SQL thành công!')
        this.showSeedModal = false
        this.currentPage = 1
        await this.fetchGroups()
      } catch (error) {
        toastError(error.response?.data?.message || 'Lỗi khi thực thi file SQL')
      } finally {
        this.isSubmitting = false
      }
    }
  }
}
</script>

<style scoped>
.badge { padding: 4px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: bold; }
.badge-success { background: #d4edda; color: #155724; }
.badge-danger { background: #f8d7da; color: #721c24; }

.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: white; width: 450px; border-radius: 8px; overflow: hidden; box-shadow: 0 5px 25px rgba(0,0,0,0.2); }
.card-header { background: #1a507a; color: white; padding: 1rem; font-weight: bold; text-align: center; }
.admin-form { padding: 1.5rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; font-weight: bold; }
.form-group input, .form-select { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; }
.checkbox-group { display: flex; align-items: center; gap: 10px; }
.checkbox-group input { width: auto; }

.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 1.5rem; padding: 1rem 1.5rem; border-top: 1px solid #eee; }
.btn-save { background: #3498db; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 4px; cursor: pointer; font-weight: bold; }
.btn-save:hover { background: #2980b9; }
.btn-cancel { background: #95a5a6; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 4px; cursor: pointer; }
.btn-cancel:hover { background: #7f8c8d; }

.action-btn { background: none; border: none; cursor: pointer; font-size: 1.1rem; padding: 4px; border-radius: 4px; }
.action-btn:hover { background: #f0f0f0; }
.cat-btn { color: #f39c12; }
.cat-btn:hover { color: #f39c12; }

.btn-add-new {
  background-color: #3498db;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn-seed-data {
  background-color: #27ae60;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background-color 0.2s, transform 0.1s;
}

.btn-seed-data:hover:not(:disabled) {
  background-color: #219653;
}

.btn-delete-all {
  background-color: #e74c3c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: background-color 0.2s, transform 0.1s;
}

.btn-delete-all:hover:not(:disabled) {
  background-color: #c0392b;
}

.btn-delete-all:disabled {
  background-color: #e57373;
  opacity: 0.6;
  cursor: not-allowed;
}

/* Seed Modal Styles */
.seed-modal-card {
  max-width: 620px;
  width: 90%;
  border-radius: 10px;
}

.close-x-btn {
  background: none;
  border: none;
  color: white;
  font-size: 1.2rem;
  cursor: pointer;
  line-height: 1;
}

.seed-intro {
  background-color: #f8fafc;
  border-left: 4px solid #3b82f6;
  padding: 10px 14px;
  border-radius: 0 6px 6px 0;
}

.seed-option-card {
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #ffffff;
}

.seed-option-card:hover {
  border-color: #94a3b8;
}

.seed-option-card.active {
  border-color: #27ae60;
  background-color: #f0fdf4;
  box-shadow: 0 0 0 1px #27ae60;
}

.upload-box {
  background-color: #f8fafc;
  border: 1px dashed #cbd5e1;
  padding: 10px;
  border-radius: 6px;
}

.selected-file-info {
  font-size: 0.85rem;
  color: #2c3e50;
}

.template-download-bar {
  background-color: #f1f5f9;
  border: 1px dashed #cbd5e1;
}

.btn-download-tpl {
  background-color: #ffffff;
  border: 1px solid #3b82f6;
  color: #3b82f6;
  border-radius: 4px;
  padding: 5px 12px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-download-tpl:hover:not(:disabled) {
  background-color: #3b82f6;
  color: #ffffff;
}

.btn-primary-action {
  background-color: #27ae60 !important;
  font-weight: 600;
}

.btn-primary-action:hover:not(:disabled) {
  background-color: #219653 !important;
}

.cursor-pointer {
  cursor: pointer;
}
</style>
