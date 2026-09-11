<template>
  <div class="page-content">
    <DataTable
      :title="pageTitle"
      placeholder="Tìm kiếm tên, mô tả..."
      addButtonLabel="Thêm chuyên mục"
      :headers="headers"
      :items="displayCategories"
      :totalItems="filteredCategories.length"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="loading"
      @search="handleSearch"
      @add="openAddModal"
      @edit="openEditModal"
      @delete="deleteCategory"
      @view="openEditModal"
      @sort="handleSort"
    >
      <template #extra-filters>
        <div class="filter-group-wrapper">
          <label>Nhóm:</label>
          <select v-model="filterGroupId" class="form-select-sm" :disabled="isFixedGroup">
            <option :value="null">-- Tất cả nhóm --</option>
            <option v-for="g in categoryGroups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
      </template>

      <template #divAction>
        <button class="btn-add-new" @click="openAddModal">
          <span class="icon">+</span> Thêm chuyên mục
        </button>
        <button 
          type="button" 
          class="btn-delete-all" 
          @click="deleteAllCategoriesInGroup"
          :disabled="loading || filteredCategories.length === 0 || !filterGroupId"
          title="Xóa tất cả chuyên mục của nhóm này"
        >
          <span class="icon">🗑️</span> Xóa tất cả
        </button>
      </template>

      <template #extra-actions="{ item }">
        <button class="action-btn sub-cat-btn" @click="openSubModal(item)" title="Quản lý chuyên mục con">📁</button>
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

    <!-- Modal Form Chuyên mục chính -->
    <BaseModal 
      v-model:show="showModal" 
      :title="isEditing ? 'CẬP NHẬT CHUYÊN MỤC' : 'THÊM CHUYÊN MỤC MỚI'"
    >
      <form @submit.prevent="handleSubmit" class="admin-form">
        <div class="form-group">
          <label>Tên chuyên mục:</label>
          <input v-model="form.name" required>
        </div>
        <div class="form-group">
          <label>Mô tả:</label>
          <textarea v-model="form.description" rows="3"></textarea>
        </div>
        <div class="form-group">
          <label>Thứ tự hiển thị:</label>
          <input type="number" v-model="form.positionOrder">
        </div>
        <div class="form-group">
          <label>Nhóm chuyên mục:</label>
          <select v-model="form.categoryGroupId" class="form-select" :disabled="isFixedGroup">
            <option :value="null">-- Không chọn --</option>
            <option v-for="g in categoryGroups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div class="modal-footer">
          <button type="button" @click="showModal = false" class="btn-cancel">Đóng</button>
          <button type="submit" class="btn-save">{{ isEditing ? 'Cập nhật' : 'Lưu lại' }}</button>
        </div>
      </form>
    </BaseModal>

    <!-- Modal Quản lý Chuyên mục con (Sub-categories) -->
    <TableModal 
      v-model:show="showSubModal" 
      :title="`QUẢN LÝ CHUYÊN MỤC CON: ${selectedParentCategory?.name}`"
      cardClass="sub-manager-card"
      placeholder="Tìm kiếm chuyên mục con..."
      addButtonLabel="Thêm chuyên mục con"
      :headers="subHeaders"
      :items="displaySubCategories"
      :totalItems="filteredSubCategories.length"
      v-model:pageSize="subPageSize"
      v-model:currentPage="subCurrentPage"
      :loading="loading"
      @search="handleSubSearch"
      @add="openAddSubCategory"
      @edit="openEditSubCategory"
      @delete="deleteSubCategory"
      @sort="handleSubSort"
      :showSTT="true"
    >
      <template #item-name="{ item }">
        <strong>{{ item.name }}</strong>
      </template>
      <template #item-active="{ item }">
        <span :class="['badge', item.active ? 'badge-success' : 'badge-danger']">
          {{ item.active ? 'Bật' : 'Tắt' }}
        </span>
      </template>
    </TableModal>

    <!-- Modal Form Chuyên mục con (CRUD Sub-cat) -->
    <BaseModal 
      v-model:show="showSubFormModal" 
      :title="isEditingSub ? 'SỬA CHUYÊN MỤC CON' : 'THÊM CHUYÊN MỤC CON'"
      overlayClass="sub-form-overlay"
      cardClass="sub-form-card"
    >
      <form @submit.prevent="handleSubSubmit" class="admin-form">
        <div class="form-group">
          <label>Tên chuyên mục:</label>
          <input v-model="subForm.name" required>
        </div>
        <div class="form-group">
          <label>Mô tả:</label>
          <textarea v-model="subForm.description" rows="2"></textarea>
        </div>
        <div class="form-group">
          <label>Thứ tự:</label>
          <input type="number" v-model="subForm.positionOrder">
        </div>
        <div class="form-group checkbox-group">
          <input type="checkbox" v-model="subForm.active" id="sub-active">
          <label for="sub-active">Kích hoạt</label>
        </div>
        <div class="modal-footer">
          <button type="button" @click="showSubFormModal = false" class="btn-cancel">Hủy</button>
          <button type="submit" class="btn-save">Lưu lại</button>
        </div>
      </form>
    </BaseModal>
  </div>
</template>

<script>
import AdminService from '@/apps/Admin/services/admin.service'
import DataTable from '@/shared/components/DataTable.vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import TableModal from '@/shared/components/TableModal.vue'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'CategoryConfig',
  components: { DataTable, BaseModal, TableModal },
  data() {
    return {
      categories: [],
      loading: false,
      keyword: '',
      pageSize: 10,
      currentPage: 1,
      
      // Main Category Modal
      showModal: false,
      isEditing: false,
      headers: [
        { text: 'Tên chuyên mục', value: 'name', sortable: true },
        { text: 'Mô tả', value: 'description', sortable: true },
        { text: 'Thứ tự', value: 'positionOrder', sortable: true, width: '100px' },
        { text: 'Trạng thái', value: 'active', sortable: true, width: '120px' }
      ],
      form: { id: null, name: '', description: '', positionOrder: 0, active: true, categoryGroupId: null, parentCategoryId: null },
      categoryGroups: [],
      sortField: 'positionOrder',
      sortOrder: 'asc',

      // Filter
      filterGroupId: null,
      isFixedGroup: false,

      // Sub-category Modal
      showSubModal: false,
      selectedParentCategory: null,
      subKeyword: '',
      subPageSize: 10,
      subCurrentPage: 1,
      subSortField: 'positionOrder',
      subSortOrder: 'asc',
      subHeaders: [
        { text: 'Tên chuyên mục con', value: 'name', sortable: true },
        { text: 'Mô tả', value: 'description', sortable: true },
        { text: 'Thứ tự', value: 'positionOrder', sortable: true, width: '100px' },
        { text: 'Trạng thái', value: 'active', sortable: true, width: '120px' }
      ],

      // Sub-category Form
      showSubFormModal: false,
      isEditingSub: false,
      subForm: { id: null, name: '', description: '', positionOrder: 0, active: true, categoryGroupId: null, parentCategoryId: null }
    }
  },
  computed: {
    pageTitle() {
      if (this.isFixedGroup && this.selectedGroupName) {
        return `Quản lý Chuyên mục thuộc Nhóm: ${this.selectedGroupName}`
      }
      return 'Quản lý Chuyên mục'
    },
    selectedGroupName() {
      const g = this.categoryGroups.find(group => group.id === this.filterGroupId)
      return g ? g.name : ''
    },
    filteredCategories() {
      let result = this.categories
      
      // Group Filter
      if (this.filterGroupId) {
        result = result.filter(c => c.categoryGroupId === this.filterGroupId)
      }

      // Show only top level in main table if we have a parent/child structure
      // Actually, usually we show all or just top-level. 
      // User wants another icon for sub-categories, implying main table shows parent categories.
      result = result.filter(c => !c.parentCategoryId)

      if (this.keyword) {
        const k = this.keyword.trim().toLowerCase()
        result = result.filter(c => 
          (c.name && c.name.toLowerCase().includes(k)) ||
          (c.description && c.description.toLowerCase().includes(k))
        )
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
    displayCategories() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredCategories.slice(start, end)
    },

    // Sub-categories Logic
    filteredSubCategories() {
      if (!this.selectedParentCategory) return []
      let result = this.categories.filter(c => c.parentCategoryId === this.selectedParentCategory.id)
      
      if (this.subKeyword) {
        const k = this.subKeyword.trim().toLowerCase()
        result = result.filter(c => 
          (c.name && c.name.toLowerCase().includes(k)) ||
          (c.description && c.description.toLowerCase().includes(k))
        )
      }
      if (this.subSortField) {
        result = [...result].sort((a, b) => {
          let valA = a[this.subSortField]
          let valB = b[this.subSortField]
          if (typeof valA === 'string') valA = valA.toLowerCase()
          if (typeof valB === 'string') valB = valB.toLowerCase()
          if (valA < valB) return this.subSortOrder === 'asc' ? -1 : 1
          if (valA > valB) return this.subSortOrder === 'asc' ? 1 : -1
          return 0
        })
      }
      return result
    },
    displaySubCategories() {
      const start = (this.subCurrentPage - 1) * this.subPageSize
      const end = start + this.subPageSize
      return this.filteredSubCategories.slice(start, end)
    }
  },
  watch: {
    selectedGroupName: {
      immediate: true,
      handler(newVal) {
        if (newVal) this.updateBreadcrumb()
      }
    },
    '$route.params.groupId': {
      immediate: true,
      handler() {
        this.updateFilterFromRoute()
      }
    },
    '$route.query.groupId': {
      immediate: true,
      handler() {
        this.updateFilterFromRoute()
      }
    }
  },
  mounted() {
    this.fetchCategories()
  },
  methods: {
    updateFilterFromRoute() {
      const gId = this.$route.params.groupId || this.$route.query.groupId
      if (gId) {
        this.filterGroupId = parseInt(gId)
        this.isFixedGroup = true
      } else {
        this.filterGroupId = null
        this.isFixedGroup = false
      }
      this.updateBreadcrumb()
    },
    async fetchCategories() {
      this.loading = true
      try {
        const response = await AdminService.getCategories()
        this.categories = response.data
        
        // Fetch Groups and Top-level for dropdowns
        const groupRes = await AdminService.getCategoryGroups()
        this.categoryGroups = groupRes.data
        
        this.updateBreadcrumb()
      } catch (error) {
        console.error('Lỗi khi tải dữ liệu:', error)
      } finally {
        this.loading = false
      }
    },
    updateBreadcrumb() {
      if (this.$route.name === 'AdminCategoryGroupDetail' && this.selectedGroupName) {
        this.$route.meta.breadcrumbTitle = `Chuyên mục thuộc Nhóm: ${this.selectedGroupName}`
      }
    },
    handleSearch(k) {
      this.keyword = k
      this.currentPage = 1
    },
    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
    },
    
    // Main CRUD
    openAddModal() {
      this.resetForm()
      if (this.filterGroupId) {
        this.form.categoryGroupId = this.filterGroupId
      }
      this.showModal = true
    },
    openEditModal(item) {
      this.form = { ...item }
      this.isEditing = true
      this.showModal = true
    },
    async handleSubmit() {
      try {
        if (this.isEditing) {
          await AdminService.updateCategory(this.form.id, this.form)
          toastSuccess('Cập nhật chuyên mục thành công')
        } else {
          await AdminService.createCategory(this.form)
          toastSuccess('Thêm chuyên mục thành công')
        }
        this.showModal = false
        this.fetchCategories()
      } catch (error) {
        toastError('Lỗi khi lưu dữ liệu')
      }
    },
    async deleteCategory(item) {
      const result = await alertConfirm('Xóa chuyên mục', `Bạn có chắc chắn muốn xóa chuyên mục "${item.name}" và tất cả dữ liệu bên trong?`)
      if (result.isConfirmed) {
        try {
          await AdminService.deleteCategory(item.id)
          toastSuccess('Đã xóa chuyên mục')
          this.fetchCategories()
        } catch (error) {
          toastError('Lỗi khi xóa chuyên mục')
        }
      }
    },
    async deleteAllCategoriesInGroup() {
      if (!this.filterGroupId) {
        toastError('Vui lòng chọn một nhóm chuyên mục để xóa tất cả chuyên mục bên trong.')
        return
      }
      const groupName = this.selectedGroupName || 'nhóm này'
      const result = await alertConfirm(
        'CẢNH BÁO NGUY HIỂM!',
        `Hành động này sẽ XÓA TOÀN BỘ chuyên mục (kể cả chuyên mục con và bài viết) thuộc "${groupName}"! Bạn có chắc chắn muốn xóa không?`
      )
      if (result.isConfirmed) {
        this.loading = true
        try {
          await AdminService.deleteCategoriesByGroupId(this.filterGroupId)
          toastSuccess('Đã xóa tất cả chuyên mục của nhóm thành công')
          this.currentPage = 1
          await this.fetchCategories()
        } catch (error) {
          toastError(error.response?.data?.message || 'Lỗi khi xóa chuyên mục của nhóm')
        } finally {
          this.loading = false
        }
      }
    },
    resetForm() {
      this.form = { id: null, name: '', description: '', positionOrder: 0, active: true, categoryGroupId: null, parentCategoryId: null }
      this.isEditing = false
    },

    // Sub-category Actions
    openSubModal(category) {
      this.selectedParentCategory = category
      this.subKeyword = ''
      this.subCurrentPage = 1
      this.showSubModal = true
    },
    handleSubSearch(k) {
      this.subKeyword = k
      this.subCurrentPage = 1
    },
    handleSubSort({ field, order }) {
      this.subSortField = field
      this.subSortOrder = order
    },
    openAddSubCategory() {
      this.subForm = { 
        id: null, name: '', description: '', positionOrder: 0, active: true, 
        categoryGroupId: this.selectedParentCategory.categoryGroupId, 
        parentCategoryId: this.selectedParentCategory.id 
      }
      this.isEditingSub = false
      this.showSubFormModal = true
    },
    openEditSubCategory(item) {
      this.subForm = { ...item }
      this.isEditingSub = true
      this.showSubFormModal = true
    },
    async handleSubSubmit() {
      try {
        if (this.isEditingSub) {
          await AdminService.updateCategory(this.subForm.id, this.subForm)
          toastSuccess('Cập nhật chuyên mục con thành công')
        } else {
          await AdminService.createCategory(this.subForm)
          toastSuccess('Thêm chuyên mục con thành công')
        }
        this.showSubFormModal = false
        this.fetchCategories() // Refresh all to update sub-list
      } catch (error) {
        toastError('Lỗi khi lưu chuyên mục con')
      }
    },
    async deleteSubCategory(item) {
      const result = await alertConfirm('Xóa chuyên mục con', `Xóa chuyên mục con "${item.name}"?`)
      if (result.isConfirmed) {
        try {
          await AdminService.deleteCategory(item.id)
          toastSuccess('Đã xóa chuyên mục con')
          this.fetchCategories()
        } catch (error) {
          toastError('Lỗi khi xóa chuyên mục con')
        }
      }
    }
  }
}
</script>

<style scoped>
.badge { padding: 4px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: bold; }
.badge-success { background: #d4edda; color: #155724; }
.badge-danger { background: #f8d7da; color: #721c24; }

.sub-form-overlay { z-index: 1001 !important; background: rgba(0,0,0,0.3) !important; }

/* Các style modal-card và card-header đã được chuyển sang BaseModal. 
   Ở đây chỉ định nghĩa các class truyền vào cardClass nếu cần */
:deep(.sub-manager-card) { width: 1000px; max-width: 95vw; max-height: 90vh; }
:deep(.sub-form-card) { width: 450px; }

.admin-form { padding: 1.5rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; font-weight: bold; }
.form-group input, .form-group textarea, .form-select { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; background: white; }
.checkbox-group { display: flex; align-items: center; gap: 10px; }
.checkbox-group input { width: auto; }

.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 1rem 1.5rem; border-top: 1px solid #eee; }
.btn-save { background: #27ae60; color: white; border: none; padding: 0.75rem 2rem; border-radius: 4px; cursor: pointer; font-weight: bold; }
.btn-cancel { background: #95a5a6; color: white; border: none; padding: 0.75rem 1.5rem; border-radius: 4px; cursor: pointer; }

.filter-group-wrapper { display: flex; align-items: center; gap: 8px; background: #f0f4f8; padding: 4px 12px; border-radius: 4px; border: 1px solid #d1d9e6; }
.filter-group-wrapper label { font-weight: bold; margin-bottom: 0; color: #1a507a; }
.form-select-sm { padding: 4px 8px; border-radius: 4px; border: 1px solid #ddd; }

.sub-manager-content { padding: 0 1.5rem; overflow-y: auto; max-height: 60vh; }
.sub-cat-btn:hover { color: #f39c12; }

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
