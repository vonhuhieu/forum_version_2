<template>
  <div class="page-content">
    <DataTable
      title="Quản lý Thành viên"
      placeholder="Tìm kiếm tên đăng nhập, tên hiển thị, email..."
      addButtonLabel="Thêm thành viên mới"
      :headers="headers"
      :items="displayUsers"
      :totalItems="filteredUsers.length"
      v-model:pageSize="pageSize"
      v-model:currentPage="currentPage"
      :loading="loading"
      @search="handleSearch"
      @add="openAddModal"
      @edit="openEditModal"
      @delete="deleteUser"
      @sort="handleSort"
    >
      <!-- Slot cho bộ lọc thêm -->
      <template #extra-filters>
        <div class="filter-item-mini">
          <select v-model="selectedRoleFilter" class="mini-select">
            <option value="">Tất cả vai trò</option>
            <option v-for="role in filterRoleOptions" :key="role.value" :value="role.value">
              {{ role.text }}
            </option>
          </select>
        </div>
      </template>

      <!-- Slot cho cột Vai trò -->
      <template #item-roles="{ item }">
        <div class="roles-badges">
          <span v-for="role in item.roles" :key="role" :class="['badge', getRoleBadgeClass(role)]">
            {{ getRoleName(role) }}
          </span>
        </div>
      </template>

      <!-- Slot cho cột Ngày tham gia -->
      <template #item-createdAt="{ item }">
        {{ formatDate(item.createdAt) }}
      </template>
    </DataTable>

    <!-- Modal Thêm/Sửa Thành viên -->
    <BaseModal 
      v-model:show="showModal" 
      :title="isEdit ? 'CẬP NHẬT THÀNH VIÊN' : 'THÊM THÀNH VIÊN MỚI'"
    >
      <div class="admin-form">
        <div class="form-group">
          <label>Tên đăng nhập (Username) <span class="text-danger">*</span></label>
          <input 
            type="text" 
            class="form-control" 
            v-model="formData.username" 
            placeholder="Tên đăng nhập dùng để login"
            :disabled="isEdit"
          />
        </div>
        
        <div class="form-group">
          <label>Tên hiển thị (Display Name)</label>
          <input 
            type="text" 
            class="form-control" 
            v-model="formData.displayName" 
            placeholder="Tên hiển thị trên diễn đàn (Không bắt buộc)"
          />
        </div>

        <div class="form-group">
          <label>Email</label>
          <input 
            type="email" 
            class="form-control" 
            v-model="formData.email" 
            placeholder="Địa chỉ email (Không bắt buộc)"
          />
        </div>

        <div class="form-group">
          <label>Mật khẩu <span v-if="!isEdit" class="text-danger">*</span></label>
          <input 
            type="password" 
            class="form-control" 
            v-model="formData.password" 
            :placeholder="isEdit ? 'Để trống nếu không muốn thay đổi' : 'Nhập mật khẩu'"
          />
        </div>

        <div class="form-group">
          <label>Vai trò <span class="text-danger">*</span></label>
          <select class="form-control select-control" v-model="formData.role">
            <option v-for="role in availableRoles" :key="role.value" :value="role.value">
              {{ role.text }}
            </option>
          </select>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="showModal = false">Hủy</button>
          <button class="btn-save" @click="saveUser" :disabled="saving">
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
import AdminService from '@/apps/Admin/services/admin.service'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'

export default {
  name: 'UserManagement',
  components: {
    DataTable,
    BaseModal
  },
  data() {
    return {
      users: [],
      loading: false,
      keyword: '',
      pageSize: 10,
      currentPage: 1,
      sortField: 'createdAt',
      sortOrder: 'desc',
      headers: [
        { text: 'Tên đăng nhập', value: 'username', sortable: true, width: '20%' },
        { text: 'Tên hiển thị', value: 'displayName', sortable: true, width: '25%' },
        { text: 'Email', value: 'email', sortable: true, width: '25%' },
        { text: 'Vai trò', value: 'roles', sortable: false, width: '15%' },
        { text: 'Ngày tham gia', value: 'createdAt', sortable: true, width: '15%' }
      ],
      showModal: false,
      isEdit: false,
      saving: false,
      currentUserRoles: [],
      selectedRoleFilter: '',
      formData: {
        id: null,
        username: '',
        displayName: '',
        email: '',
        password: '',
        role: 'ROLE_USER'
      }
    }
  },
  computed: {
    isSuperAdmin() {
      return this.currentUserRoles.includes('ROLE_SUPER_ADMIN')
    },
    isAdmin() {
      return this.currentUserRoles.includes('ROLE_ADMIN')
    },
    availableRoles() {
      if (this.isSuperAdmin) {
        return [
          { value: 'ROLE_SUPER_ADMIN', text: 'Super Admin' },
          { value: 'ROLE_ADMIN', text: 'Admin' },
          { value: 'ROLE_USER', text: 'Thành viên chính thức' },
          { value: 'ROLE_NON_OFFICIAL_USER', text: 'Chưa chính thức' }
        ]
      } else {
        return [
          { value: 'ROLE_USER', text: 'Thành viên chính thức' },
          { value: 'ROLE_NON_OFFICIAL_USER', text: 'Chưa chính thức' }
        ]
      }
    },
    filterRoleOptions() {
      if (this.isSuperAdmin) {
        return [
          { value: 'ROLE_ADMIN', text: 'Admin' },
          { value: 'ROLE_USER', text: 'Thành viên chính thức' },
          { value: 'ROLE_NON_OFFICIAL_USER', text: 'Chưa chính thức' }
        ]
      } else {
        return [
          { value: 'ROLE_USER', text: 'Thành viên chính thức' },
          { value: 'ROLE_NON_OFFICIAL_USER', text: 'Chưa chính thức' }
        ]
      }
    },
    filteredUsers() {
      let result = this.users
      
      if (this.selectedRoleFilter) {
        result = result.filter(u => u.roles && u.roles.includes(this.selectedRoleFilter))
      }
      
      if (this.keyword) {
        const k = this.keyword.trim().toLowerCase()
        result = result.filter(u => 
          (u.username && u.username.toLowerCase().includes(k)) ||
          (u.displayName && u.displayName.toLowerCase().includes(k)) ||
          (u.email && u.email.toLowerCase().includes(k))
        )
      }
      
      if (this.sortField) {
        result = [...result].sort((a, b) => {
          let valA = a[this.sortField] || ''
          let valB = b[this.sortField] || ''
          
          if (typeof valA === 'string') valA = valA.toLowerCase()
          if (typeof valB === 'string') valB = valB.toLowerCase()

          if (valA < valB) return this.sortOrder === 'asc' ? -1 : 1
          if (valA > valB) return this.sortOrder === 'asc' ? 1 : -1
          return 0
        })
      }
      
      return result
    },
    displayUsers() {
      const start = (this.currentPage - 1) * this.pageSize
      const end = start + this.pageSize
      return this.filteredUsers.slice(start, end)
    }
  },
  watch: {
    selectedRoleFilter() {
      this.currentPage = 1
    }
  },
  created() {
    this.getCurrentUserRoles()
    this.fetchUsers()
  },
  methods: {
    getCurrentUserRoles() {
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const userObj = JSON.parse(userStr)
          this.currentUserRoles = userObj.roles || []
        }
      } catch (e) {
        console.error('Lỗi khi lấy thông tin vai trò người dùng hiện tại:', e)
      }
    },
    async fetchUsers() {
      this.loading = true
      try {
        const response = await AdminService.getAdminUsers()
        this.users = response.data
      } catch (error) {
        console.error('Lỗi khi tải danh sách thành viên:', error)
        toastError('Không thể tải danh sách thành viên')
      } finally {
        this.loading = false
      }
    },
    handleSearch(keyword) {
      this.keyword = keyword
      this.currentPage = 1
    },
    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
    },
    openAddModal() {
      this.isEdit = false
      this.formData = { 
        id: null, 
        username: '', 
        displayName: '', 
        email: '', 
        password: '', 
        role: this.isSuperAdmin ? 'ROLE_SUPER_ADMIN' : 'ROLE_USER' 
      }
      this.showModal = true
    },
    openEditModal(user) {
      this.isEdit = true
      let selectedRole = 'ROLE_USER'
      if (user.roles && user.roles.length > 0) {
        if (user.roles.includes('ROLE_SUPER_ADMIN')) {
          selectedRole = 'ROLE_SUPER_ADMIN'
        } else if (user.roles.includes('ROLE_ADMIN')) {
          selectedRole = 'ROLE_ADMIN'
        } else if (user.roles.includes('ROLE_NON_OFFICIAL_USER')) {
          selectedRole = 'ROLE_NON_OFFICIAL_USER'
        } else {
          selectedRole = user.roles[0]
        }
      }
      this.formData = { 
        id: user.id,
        username: user.username,
        displayName: user.displayName || '',
        email: user.email || '',
        password: '',
        role: selectedRole
      }
      this.showModal = true
    },
    async saveUser() {
      if (!this.isEdit) {
        if (!this.formData.username || !this.formData.password) {
          toastError('Vui lòng điền tên đăng nhập và mật khẩu')
          return
        }
      }
      
      this.saving = true
      try {
        const payload = {
          username: this.formData.username,
          displayName: this.formData.displayName,
          email: this.formData.email,
          password: this.formData.password,
          roles: [this.formData.role]
        }

        if (this.isEdit) {
          await AdminService.updateAdminUser(this.formData.id, payload)
          toastSuccess('Cập nhật thành viên thành công')
        } else {
          await AdminService.createAdminUser(payload)
          toastSuccess('Thêm thành viên mới thành công')
        }
        this.showModal = false
        this.fetchUsers()
      } catch (error) {
        console.error('Lỗi khi lưu thông tin thành viên:', error)
        toastError('Lỗi khi lưu thông tin thành viên')
      } finally {
        this.saving = false
      }
    },
    async deleteUser(user) {
      const result = await alertConfirm(
        'Xóa thành viên', 
        `Bạn có chắc chắn muốn xóa thành viên "${user.displayName || user.username}"?`
      )
      if (result.isConfirmed) {
        try {
          await AdminService.deleteAdminUser(user.id)
          toastSuccess('Đã xóa thành viên thành công')
          this.fetchUsers()
        } catch (error) {
          console.error('Lỗi khi xóa thành viên:', error)
          toastError('Lỗi khi xóa thành viên')
        }
      }
    },
    formatDate(dateStr) {
      if (!dateStr) return 'Chưa có thông tin'
      return new Date(dateStr).toLocaleString('vi-VN')
    },
    getRoleBadgeClass(role) {
      switch (role) {
        case 'ROLE_SUPER_ADMIN':
          return 'badge-danger'
        case 'ROLE_ADMIN':
          return 'badge-warning'
        case 'ROLE_USER':
          return 'badge-success'
        case 'ROLE_NON_OFFICIAL_USER':
          return 'badge-secondary'
        default:
          return 'badge-light'
      }
    },
    getRoleName(role) {
      switch (role) {
        case 'ROLE_SUPER_ADMIN':
          return 'Super Admin'
        case 'ROLE_ADMIN':
          return 'Admin'
        case 'ROLE_USER':
          return 'Thành viên'
        case 'ROLE_NON_OFFICIAL_USER':
          return 'Chưa chính thức'
        default:
          return role.replace('ROLE_', '')
      }
    }
  }
}
</script>

<style scoped>
.page-content {
  padding: 1rem 0;
}

.admin-form {
  padding: 1.5rem;
  max-height: calc(80vh - 120px);
  overflow-y: auto;
}

.form-group {
  margin-bottom: 0.75rem;
}

.text-danger {
  color: #e74c3c;
  margin-left: 2px;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.9rem;
}

.form-control {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.95rem;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #3498db;
  outline: none;
}

.select-control {
  background-color: white;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 1.25rem;
  border-top: 1px solid #eee;
  margin-top: 1.5rem;
}

.btn-save {
  background: #27ae60;
  color: white;
  border: none;
  padding: 0.75rem 2rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: opacity 0.2s;
}

.btn-save:hover {
  opacity: 0.9;
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-cancel {
  background: #95a5a6;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: opacity 0.2s;
}

.btn-cancel:hover {
  opacity: 0.9;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: bold;
}

.badge-secondary {
  background: #e2e8f0;
  color: #4a5568;
}

.badge-warning {
  background: #feebc8;
  color: #c05621;
}

.badge-success {
  background: #c6f6d5;
  color: #22543d;
}

.badge-danger {
  background: #fed7d7;
  color: #9b2c2c;
}

.badge-light {
  background: #edf2f7;
  color: #2d3748;
}

.roles-badges {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.filter-item-mini {
  display: flex;
  align-items: center;
}

.mini-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  color: #333;
  font-size: 14px;
  min-width: 200px;
  outline: none;
  cursor: pointer;
}

.mini-select:focus {
  border-color: #3498db;
}
</style>
