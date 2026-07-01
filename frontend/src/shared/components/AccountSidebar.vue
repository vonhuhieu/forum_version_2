<template>
  <div class="account-sidebar-root">
    <!-- Nút toggle sidebar trên Mobile/Tablet (centered) -->
    <div class="toggle-button-container">
      <button class="btn-account-toggle" @click="isOpen = true">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="icon-toggle">
          <line x1="3" y1="12" x2="21" y2="12"></line>
          <line x1="3" y1="6" x2="21" y2="6"></line>
          <line x1="3" y1="18" x2="21" y2="18"></line>
        </svg>
        Tài khoản của bạn
      </button>
    </div>

    <!-- Cấu trúc Sidebar Drawer -->
    <div class="account-sidebar-wrapper" :class="{ 'open': isOpen }">
      <div class="sidebar-backdrop" @click="isOpen = false"></div>
      
      <div class="account-sidebar-container">
        <div class="sidebar-box card">
          <div class="sidebar-header">
            <span>Tài khoản của bạn</span>
            <button class="btn-close-sidebar" @click="isOpen = false" aria-label="Đóng menu">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>
          <div class="sidebar-menu">
            <router-link 
              :to="{ name: 'UserProfile' }" 
              class="menu-item" 
              :class="{ 'is-active': activeMenu === 'profile' }"
              @click="isOpen = false"
            >
              Trang cá nhân
            </router-link>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Thông báo
            </a>
            <router-link 
              :to="{ name: 'ReceivedReactions' }" 
              class="menu-item" 
              :class="{ 'is-active': activeMenu === 'reactions-received' }"
              @click="isOpen = false"
            >
              Điểm tương tác nhận được
            </router-link>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Dấu trang
            </a>
            <div class="menu-divider"></div>
            <div class="menu-section-header">Thiết lập</div>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Chi tiết tài khoản
            </a>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Mật khẩu và bảo mật
            </a>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Bảo mật cá nhân
            </a>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Tùy chọn
            </a>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
            >
              Đang theo dõi
            </a>
            <a 
              href="#" 
              class="menu-item" 
              @click.prevent="isOpen = false"
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
              @click.prevent="logout(); isOpen = false"
            >
              Thoát
            </a>
          </div>
        </div>
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
  data() {
    return {
      isOpen: false
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
.account-sidebar-root {
  width: 250px;
  flex-shrink: 0;
}

.account-sidebar-container {
  width: 100%;
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

.btn-close-sidebar {
  display: none;
}

.toggle-button-container {
  display: none;
}

.account-sidebar-wrapper {
  display: contents;
}

.sidebar-backdrop {
  display: none;
}

@media (max-width: 992px) {
  .account-sidebar-root {
    width: 100%;
    flex-shrink: 1;
  }

  .toggle-button-container {
    display: flex;
    justify-content: center;
    width: 100%;
    margin-bottom: 1.5rem;
  }

  .btn-account-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background-color: #ffffff;
    border: 1px solid #d8dbe0;
    border-radius: 4px;
    padding: 8px 16px;
    color: #1a507a;
    font-weight: 500;
    font-size: 0.95rem;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .btn-account-toggle:hover {
    background-color: #f8f9fa;
    color: #d13838;
    border-color: #c0c4cc;
  }

  .btn-close-sidebar {
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    color: white;
    cursor: pointer;
    padding: 5px;
    border-radius: 4px;
    transition: background-color 0.2s;
  }

  .btn-close-sidebar:hover {
    background-color: rgba(255, 255, 255, 0.15);
  }

  .sidebar-header {
    background: #1a507a !important;
    color: #ffffff !important;
    border-bottom: 1px solid #123a59 !important;
    border-radius: 0 !important;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px !important;
  }

  .sidebar-box {
    border: none !important;
    border-radius: 0 !important;
    box-shadow: none !important;
    background: transparent !important;
  }

  .logout-box {
    border-top: 1px solid #e8e8e8 !important;
    margin-top: 15px;
  }

  /* Account Sidebar Wrapper in Mobile (drawer behavior) */
  .account-sidebar-wrapper {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 10000;
    visibility: hidden;
    transition: visibility 0.3s;
  }

  .account-sidebar-wrapper.open {
    visibility: visible;
  }

  .sidebar-backdrop {
    display: block;
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.6);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .account-sidebar-wrapper.open .sidebar-backdrop {
    opacity: 1;
  }

  .account-sidebar-wrapper .account-sidebar-container {
    position: absolute;
    top: 0;
    left: 0;
    width: 280px;
    height: 100%;
    background-color: #ffffff;
    box-shadow: 2px 0 12px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 1;
    overflow-y: auto;
    gap: 0;
  }

  .account-sidebar-wrapper.open .account-sidebar-container {
    transform: translateX(0);
  }
}
</style>
