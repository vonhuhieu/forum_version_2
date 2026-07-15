<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside :class="['admin-sidebar', { 'collapsed': isCollapsed }]">
      <div class="sidebar-brand">
        <span class="brand-text" v-show="!isCollapsed">QUẢN TRỊ HỆ THỐNG</span>
        <button class="btn-toggle-sidebar" @click="toggleSidebar" :title="isCollapsed ? 'Mở rộng sidebar' : 'Thu hẹp sidebar'">
          <span class="toggle-icon">{{ isCollapsed ? '▶' : '◀' }}</span>
        </button>
      </div>
      
      <nav class="sidebar-nav">
        <div class="nav-group">
          <div class="nav-label">HỆ THỐNG</div>
          <router-link :to="{ name: 'AdminMenu' }" class="nav-item">
            <span class="icon">☰</span> <span class="nav-text">Cấu hình Menu</span>
          </router-link>
          <router-link :to="{ name: 'AdminSettings' }" :class="['nav-item', { 'active': $route.name === 'AdminSettings' }]">
            <span class="icon">⚙️</span> <span class="nav-text">Cấu hình Hệ thống</span>
          </router-link>
        </div>
        
        <div class="nav-group">
          <div class="nav-label">NỘI DUNG</div>
           <router-link :to="{ name: 'AdminCategoryGroup' }" :class="['nav-item', { 'active': $route.name === 'AdminCategoryGroup' || $route.name === 'AdminCategoryGroupDetail' }]">
             <span class="icon">📂</span> <span class="nav-text">Quản lý Nhóm & Chuyên mục</span>
           </router-link>
           <router-link :to="{ name: 'AdminLabels' }" :class="['nav-item', { 'active': $route.name === 'AdminLabels' }]">
             <span class="icon">🏷️</span> <span class="nav-text">Quản lý Nhãn (Label)</span>
           </router-link>
           <router-link :to="{ name: 'AdminReactionIcons' }" :class="['nav-item', { 'active': $route.name === 'AdminReactionIcons' }]">
             <span class="icon">🥰</span> <span class="nav-text">Quản lý Cảm xúc</span>
           </router-link>
           <router-link :to="{ name: 'AdminThreads' }" :class="['nav-item', { 'active': $route.name === 'AdminThreads' || $route.name === 'AdminThreadCreate' || $route.name === 'AdminThreadEdit' }]">
             <span class="icon">📝</span> <span class="nav-text">Quản lý Bài viết</span>
           </router-link>
           <router-link :to="{ name: 'AdminReports' }" :class="['nav-item', { 'active': $route.name === 'AdminReports' }]">
             <span class="icon">🚩</span> <span class="nav-text">Quản lý Báo cáo</span>
           </router-link>
        </div>

        <div class="nav-group">
          <div class="nav-label">NGƯỜI DÙNG</div>
          <router-link :to="{ name: 'AdminUsers' }" :class="['nav-item', { 'active': $route.name === 'AdminUsers' }]">
            <span class="icon">👥</span> <span class="nav-text">Thành viên</span>
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <button @click="handleLogout" class="btn-logout" title="Đăng xuất">
          <span class="icon">🚪</span> <span class="nav-text">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="admin-main">
      <AdminHeader />

      <div class="admin-breadcrumb-container">
        <Breadcrumb :items="breadcrumbItems" />
      </div>

      <div class="admin-content-wrapper">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import Breadcrumb from '@/shared/components/Breadcrumb.vue'
import AdminHeader from '@/apps/Admin/components/AdminHeader.vue'
import AdminService from '@/apps/Admin/services/admin.service'

export default {
  name: 'AdminLayout',
  components: {
    Breadcrumb,
    AdminHeader
  },
  data() {
    return {
      groups: [],
      isCollapsed: localStorage.getItem('adminSidebarCollapsed') === 'true'
    }
  },
  computed: {
    breadcrumbItems() {
      const mapping = {
        'AdminMenu': 'Cấu hình Menu',
        'AdminSettings': 'Cấu hình Hệ thống',
        'AdminCategoryGroup': 'Quản lý Nhóm',
        'AdminCategory': 'Quản lý Chuyên mục',
        'AdminCategoryGroupDetail': 'Chuyên mục thuộc Nhóm',
        'AdminLabels': 'Quản lý Nhãn',
        'AdminReactionIcons': 'Quản lý Cảm xúc',
        'AdminThreads': 'Quản lý Bài viết',
        'AdminThreadCreate': 'Thêm bài viết mới',
        'AdminThreadEdit': 'Cập nhật bài viết',
        'AdminThreadView': 'Chi tiết bài viết',
        'AdminUsers': 'Quản lý Thành viên',
        'AdminReports': 'Quản lý Báo cáo'
      }
      
      const items = [{ title: 'Quản trị', to: { path: '/admin/menu' } }]
      const currentRouteName = this.$route.name
      
      if (currentRouteName === 'AdminCategoryGroupDetail') {
        items.push({ title: 'Quản lý Nhóm', to: { name: 'AdminCategoryGroup' } })
        
        let title = mapping[currentRouteName]
        const groupId = this.$route.params.groupId
        if (groupId && this.groups.length > 0) {
          const group = this.groups.find(g => g.id == groupId)
          if (group) {
            title = `Chuyên mục thuộc Nhóm: ${group.name}`
          }
        }
        items.push({ title })
        return items
      }

      if (mapping[currentRouteName]) {
        items.push({ title: mapping[currentRouteName] })
      } else {
        items.push({ title: currentRouteName })
      }
      
      return items
    }
  },
  mounted() {
    this.fetchGroups()
  },
  methods: {
    toggleSidebar() {
      this.isCollapsed = !this.isCollapsed
      localStorage.setItem('adminSidebarCollapsed', this.isCollapsed)
    },
    async fetchGroups() {
      try {
        const response = await AdminService.getCategoryGroups()
        this.groups = response.data
      } catch (e) {
        console.error('Error fetching groups in layout', e)
      }
    },
    handleLogout() {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      this.$router.push({ name: 'Login' })
    }
  }
}
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; background-color: #f4f7f6; font-family: 'Roboto', sans-serif; }

.admin-sidebar { 
  width: 260px; 
  background-color: #1a507a; 
  color: white; 
  display: flex; 
  flex-direction: column; 
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-x: hidden;
}

.admin-sidebar.collapsed {
  width: 70px;
}

.sidebar-brand { 
  padding: 1.5rem; 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  font-weight: bold; 
  font-size: 1.1rem; 
  background-color: rgba(0,0,0,0.1); 
  transition: all 0.3s ease;
}

.admin-sidebar.collapsed .sidebar-brand {
  padding: 1.5rem 0.5rem;
  justify-content: center;
}

.btn-toggle-sidebar {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 1rem;
  padding: 5px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, transform 0.2s;
}

.btn-toggle-sidebar:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.sidebar-nav { flex: 1; padding: 1rem 0; }

.nav-group { margin-bottom: 1.5rem; }

.nav-label { 
  padding: 0 1.5rem; 
  font-size: 0.75rem; 
  font-weight: bold; 
  color: rgba(255,255,255,0.5); 
  margin-bottom: 0.5rem; 
  text-transform: uppercase;
  transition: opacity 0.3s ease, height 0.3s ease, margin 0.3s ease, padding 0.3s ease;
  white-space: nowrap;
  overflow: hidden;
}

.admin-sidebar.collapsed .nav-label {
  opacity: 0;
  height: 0;
  margin: 0;
  padding: 0;
}

.nav-item { 
  display: flex; 
  align-items: center; 
  padding: 0.75rem 1.5rem; 
  color: rgba(255,255,255,0.8); 
  text-decoration: none; 
  transition: all 0.3s ease; 
  gap: 10px; 
  white-space: nowrap;
}

.admin-sidebar.collapsed .nav-item {
  padding: 0.75rem 0;
  justify-content: center;
  gap: 0;
}

.nav-item .icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  font-size: 1.1rem;
  transition: font-size 0.3s ease;
}

.admin-sidebar.collapsed .nav-item .icon {
  font-size: 1.3rem;
  width: 100%;
}

.nav-item .nav-text {
  transition: opacity 0.3s ease;
}

.admin-sidebar.collapsed .nav-item .nav-text {
  display: none;
}

.nav-item:hover, .nav-item.router-link-active, .nav-item.active { background-color: rgba(255,255,255,0.1) !important; color: #ffd700 !important; border-left: 4px solid #ffd700 !important; }

.admin-sidebar.collapsed .nav-item:hover, 
.admin-sidebar.collapsed .nav-item.router-link-active, 
.admin-sidebar.collapsed .nav-item.active {
  border-left: 4px solid #ffd700 !important;
}

.sidebar-footer { 
  padding: 1rem; 
  border-top: 1px solid rgba(255,255,255,0.1); 
  transition: all 0.3s ease;
}

.admin-sidebar.collapsed .sidebar-footer {
  padding: 1rem 0.5rem;
  display: flex;
  justify-content: center;
}

.btn-logout { 
  width: 100%; 
  padding: 0.75rem; 
  background: rgba(231, 76, 60, 0.2); 
  color: #ff7675; 
  border: 1px solid #ff7675; 
  border-radius: 4px; 
  cursor: pointer; 
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.admin-sidebar.collapsed .btn-logout {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 50%;
  gap: 0;
}

.admin-sidebar.collapsed .btn-logout .nav-text {
  display: none;
}

.admin-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; background-color: #f0f2f5; }
.admin-breadcrumb-container {
  padding: 1rem 1.5rem 0 1.5rem;
}
.admin-content-wrapper { flex: 1; padding: 1.5rem; overflow-y: auto; }
</style>
