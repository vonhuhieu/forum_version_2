<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <span>QUẢN TRỊ HỆ THỐNG</span>
      </div>
      
      <nav class="sidebar-nav">
        <div class="nav-group">
          <div class="nav-label">HỆ THỐNG</div>
          <router-link :to="{ name: 'AdminMenu' }" class="nav-item">
            <span class="icon">☰</span> Cấu hình Menu
          </router-link>
          <router-link :to="{ name: 'AdminSettings' }" :class="['nav-item', { 'active': $route.name === 'AdminSettings' }]">
            <span class="icon">⚙️</span> Cấu hình Hệ thống
          </router-link>
        </div>
        
        <div class="nav-group">
          <div class="nav-label">NỘI DUNG</div>
           <router-link :to="{ name: 'AdminCategoryGroup' }" :class="['nav-item', { 'active': $route.name === 'AdminCategoryGroup' || $route.name === 'AdminCategoryGroupDetail' }]">
             <span class="icon">📂</span> Quản lý Nhóm & Chuyên mục
           </router-link>
           <router-link :to="{ name: 'AdminLabels' }" :class="['nav-item', { 'active': $route.name === 'AdminLabels' }]">
             <span class="icon">🏷️</span> Quản lý Nhãn (Label)
           </router-link>
           <router-link :to="{ name: 'AdminReactionIcons' }" :class="['nav-item', { 'active': $route.name === 'AdminReactionIcons' }]">
             <span class="icon">🥰</span> Quản lý Cảm xúc
           </router-link>
           <router-link :to="{ name: 'AdminThreads' }" :class="['nav-item', { 'active': $route.name === 'AdminThreads' || $route.name === 'AdminThreadCreate' || $route.name === 'AdminThreadEdit' }]">
             <span class="icon">📝</span> Quản lý Bài viết
           </router-link>
        </div>

        <div class="nav-group">
          <div class="nav-label">NGƯỜI DÙNG</div>
          <router-link :to="{ name: 'AdminUsers' }" :class="['nav-item', { 'active': $route.name === 'AdminUsers' }]">
            <span class="icon">👥</span> Thành viên
          </router-link>
        </div>
      </nav>

      <div class="sidebar-footer">
        <button @click="handleLogout" class="btn-logout">Đăng xuất</button>
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
      groups: []
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
        'AdminUsers': 'Quản lý Thành viên'
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
.admin-sidebar { width: 260px; background-color: #1a507a; color: white; display: flex; flex-direction: column; }
.sidebar-brand { padding: 1.5rem; display: flex; align-items: center; gap: 10px; font-weight: bold; font-size: 1.1rem; background-color: rgba(0,0,0,0.1); }
.brand-icon { background: #ffd700; color: #1a507a; width: 35px; height: 35px; display: flex; align-items: center; justify-content: center; border-radius: 4px; }
.sidebar-nav { flex: 1; padding: 1rem 0; }
.nav-group { margin-bottom: 1.5rem; }
.nav-label { padding: 0 1.5rem; font-size: 0.75rem; font-weight: bold; color: rgba(255,255,255,0.5); margin-bottom: 0.5rem; text-transform: uppercase; }
.nav-item { display: flex; align-items: center; padding: 0.75rem 1.5rem; color: rgba(255,255,255,0.8); text-decoration: none; transition: all 0.3s; gap: 10px; }
.nav-item:hover, .nav-item.router-link-active, .nav-item.active { background-color: rgba(255,255,255,0.1) !important; color: #ffd700 !important; border-left: 4px solid #ffd700 !important; }
.sidebar-footer { padding: 1rem; border-top: 1px solid rgba(255,255,255,0.1); }
.btn-logout { width: 100%; padding: 0.75rem; background: rgba(231, 76, 60, 0.2); color: #ff7675; border: 1px solid #ff7675; border-radius: 4px; cursor: pointer; }
.admin-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; background-color: #f0f2f5; }
.admin-breadcrumb-container {
  padding: 1rem 1.5rem 0 1.5rem;
}
.admin-content-wrapper { flex: 1; padding: 1.5rem; overflow-y: auto; }
</style>
