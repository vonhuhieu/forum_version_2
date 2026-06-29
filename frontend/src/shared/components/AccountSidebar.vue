<template>
  <div class="account-sidebar-container">
    <div class="sidebar-box card">
      <div class="sidebar-header">
        Tài khoản của bạn
      </div>
      <div class="sidebar-menu">
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Trang cá nhân
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Thông báo
        </a>
        <router-link 
          :to="{ name: 'ReceivedReactions' }" 
          class="menu-item" 
          :class="{ 'is-active': activeMenu === 'reactions-received' }"
        >
          Điểm tương tác nhận được
        </router-link>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Dấu trang
        </a>
        <div class="menu-divider"></div>
        <div class="menu-section-header">Thiết lập</div>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Chi tiết tài khoản
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Mật khẩu và bảo mật
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Bảo mật cá nhân
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Tùy chọn
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Đang theo dõi
        </a>
        <a 
          href="#" 
          class="menu-item" 
          @click.prevent
        >
          Phớt lờ
        </a>
      </div>
    </div>
    
    <div class="sidebar-box card logout-box">
      <div class="sidebar-menu">
        <a 
          href="#" 
          class="menu-item logout-btn" 
          @click.prevent="logout"
        >
          Thoát
        </a>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AccountSidebar',
  props: {
    activeMenu: {
      type: String,
      default: ''
    }
  },
  methods: {
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/'
    }
  }
}
</script>

<style scoped>
.account-sidebar-container {
  width: 250px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.sidebar-box {
  background: #fff;
  border: 1px solid #d8dbe0;
  border-radius: 4px;
}

.sidebar-header {
  background: #f8f9fa;
  padding: 12px 15px;
  font-weight: bold;
  font-size: 1rem;
  color: #1a507a;
  border-bottom: 1px solid #d8dbe0;
  border-top-left-radius: 4px;
  border-top-right-radius: 4px;
}

.sidebar-menu {
  display: flex;
  flex-direction: column;
}

.menu-item {
  padding: 10px 15px;
  color: #1a507a;
  text-decoration: none;
  font-size: 0.95rem;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.menu-item:hover {
  background-color: #f8f9fa;
  color: #d13838;
}

.menu-item.is-active {
  background-color: #f0f4f8;
  color: #1a507a;
  font-weight: bold;
  border-left-color: #1a507a;
}

.menu-divider {
  height: 1px;
  background-color: #e8e8e8;
  margin: 5px 0;
}

.menu-section-header {
  padding: 10px 15px 5px;
  font-weight: bold;
  font-size: 0.85rem;
  color: #8c8c8c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.logout-box {
  border-color: #dfdfdf;
}

.logout-btn {
  color: #d13838;
}

.logout-btn:hover {
  background-color: #fff1f1;
}
</style>
