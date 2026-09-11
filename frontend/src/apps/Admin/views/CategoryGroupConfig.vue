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
      sortOrder: 'asc'
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

.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 1rem 1.5rem; border-top: 1px solid #eee; }
.btn-save { background: #27ae60; color: white; border: none; padding: 0.75rem 2rem; border-radius: 4px; cursor: pointer; font-weight: bold; }
.btn-cancel { background: #95a5a6; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 4px; cursor: pointer; }

.cat-btn:hover { color: #f39c12; }

.btn-add-new {
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
</style>
