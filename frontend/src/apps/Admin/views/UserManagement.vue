<template>
  <div class="page-content">
    <Loading :visible="saving" />
    <DataTable
      title="Quản lý Thành viên"
      placeholder="Tìm kiếm tên đăng nhập, tên hiển thị, email..."
      addButtonLabel="Thêm thành viên mới"
      :headers="headers"
      :items="displayUsers"
      :totalItems="totalElements"
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

      <!-- Slot cho cột Cấp bậc -->
      <template #item-displayTitle="{ item }">
        <div class="text-center">
          <span class="badge bg-secondary text-wrap" v-if="item.displayTitle">
            {{ item.displayTitle }}
          </span>
          <span class="text-muted small fst-italic" v-else>---</span>
          <div v-if="item.assignedTitleName" class="text-success font-weight-bold" style="font-size: 10px;">
            (Admin cấp)
          </div>
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

      <!-- Slot extra-actions cho Nút Cấp Title -->
      <template #extra-actions="{ item }">
        <button class="action-btn" style="background: #e67e22; color: #fff; margin-right: 4px;" title="Cấp Cấp bậc" @click="openAssignTitleModal(item)">
          🎖️
        </button>
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

    <!-- Modal Cấp Cấp bậc trực tiếp cho User -->
    <BaseModal
      v-model:show="showAssignTitleModal"
      title="CẤP CẤP BẬC TỰ DO"
    >
      <div class="admin-form" v-if="assignTargetUser">
        <p class="mb-3">
          Đang cấp Cấp bậc cho thành viên: <strong>{{ assignTargetUser.displayName || assignTargetUser.username }}</strong>
        </p>

        <div class="form-group mb-3">
          <label class="form-label font-weight-bold">Chọn Cấp bậc cấp riêng</label>
          <select class="form-select" v-model="selectedTitleId">
            <option :value="null">-- Mặc định (Tự động theo điểm / xác thực) --</option>
            <option v-for="title in allTitles" :key="title.id" :value="title.id">
              {{ title.name }} ({{ getTitleTypeName(title.type) }})
            </option>
          </select>
          <small class="text-muted mt-1 d-block">
            * Cấp bậc do Admin cấp trực tiếp có quyền lực cao nhất, ghi đè toàn bộ điểm số và trạng thái xác thực.
          </small>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="showAssignTitleModal = false">Hủy</button>
          <button class="btn-save" @click="saveAssignTitle" :disabled="assigningTitle">
            {{ assigningTitle ? 'Đang lưu...' : 'Lưu Cấp bậc' }}
          </button>
        </div>
      </div>
    </BaseModal>
  </div>
</template>

<script>
import DataTable from '@/shared/components/DataTable.vue'
import BaseModal from '@/shared/components/BaseModal.vue'
import Loading from '@/shared/components/Loading.vue'
import AdminService from '@/apps/Admin/services/admin.service'
import TitleService from '@/apps/Admin/services/title.service'
import { alertConfirm, toastSuccess, toastError } from '@/shared/utils/swal'
import { TITLE_TYPES } from '@/shared/utils/constants'

export default {
  name: 'UserManagement',
  components: {
    DataTable,
    BaseModal,
    Loading
  },
  data() {
    return {
      users: [],
      totalElements: 0,
      loading: false,
      keyword: '',
      pageSize: 10,
      currentPage: 1,
      sortField: 'createdAt',
      sortOrder: 'desc',
      headers: [
        { text: 'Tên đăng nhập', value: 'username', sortable: true, width: '15%' },
        { text: 'Tên hiển thị', value: 'displayName', sortable: true, width: '20%' },
        { text: 'Cấp bậc', value: 'displayTitle', sortable: false, width: '15%' },
        { text: 'Email', value: 'email', sortable: true, width: '20%' },
        { text: 'Vai trò', value: 'roles', sortable: false, width: '15%' },
        { text: 'Ngày tham gia', value: 'createdAt', sortable: true, width: '15%' }
      ],
      showModal: false,
      isEdit: false,
      saving: false,
      showAssignTitleModal: false,
      assignTargetUser: null,
      selectedTitleId: null,
      allTitles: [],
      assigningTitle: false,
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
    displayUsers() {
      return this.users
    }
  },
  watch: {
    selectedRoleFilter() {
      this.currentPage = 1
      this.fetchUsers()
    },
    currentPage() {
      this.fetchUsers()
    },
    pageSize() {
      this.currentPage = 1
      this.fetchUsers()
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
        const params = {
          page: this.currentPage - 1,
          size: this.pageSize,
          keyword: this.keyword,
          sortBy: this.sortField,
          sortOrder: this.sortOrder,
          role: this.selectedRoleFilter
        }
        const response = await AdminService.getAdminUsers(params)
        if (response.data && response.data.content) {
          this.users = response.data.content
          this.totalElements = response.data.totalElements
        } else {
          this.users = response.data
          this.totalElements = response.data.length
        }
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
      this.fetchUsers()
    },
    handleSort({ field, order }) {
      this.sortField = field
      this.sortOrder = order
      this.currentPage = 1
      this.fetchUsers()
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
    },
    async openAssignTitleModal(user) {
      this.assignTargetUser = user
      this.selectedTitleId = user.assignedTitleId || null
      this.showAssignTitleModal = true
      try {
        const res = await TitleService.getAllTitles()
        this.allTitles = res.data || []
      } catch (err) {
        console.error('Lỗi khi tải danh sách Cấp bậc:', err)
      }
    },
    async saveAssignTitle() {
      if (!this.assignTargetUser) return
      this.assigningTitle = true
      try {
        await TitleService.assignTitleToUser(this.assignTargetUser.id, this.selectedTitleId)
        toastSuccess('Cập nhật Cấp bậc thành công!')
        this.showAssignTitleModal = false
        this.fetchUsers()
      } catch (err) {
        toastError(err.response?.data?.message || 'Có lỗi xảy ra khi gán Cấp bậc')
      } finally {
        this.assigningTitle = false
      }
    },
    getTitleTypeName(type) {
      switch (type) {
        case TITLE_TYPES.UNVERIFIED_DEFAULT: return 'Chưa xác thực'
        case TITLE_TYPES.POINT_BASED: return 'Theo điểm'
        case TITLE_TYPES.CUSTOM_ASSIGNABLE: return 'Tự do'
        default: return type
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
