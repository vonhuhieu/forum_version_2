<template>
  <footer class="forum-footer">
    <div class="container footer-container">
      <div class="footer-grid">
        <!-- Cột 1: Nhận diện Thương hiệu & Logo -->
        <div class="footer-brand-col">
          <div class="brand-header" @click="goToHome" title="Về Trang chủ">
            <img src="/favicon.png" alt="Hợp Tác Xã Vui Vẻ" class="brand-logo" />
            <div class="brand-titles">
              <h2 class="brand-name">HỢP TÁC XÃ VUI VẺ</h2>
              <span class="brand-slogan">Cộng Đồng Thanh Niên Xa Mẹ</span>
            </div>
          </div>
          <p class="brand-desc">
            Nơi sẻ chia tâm tư, kinh nghiệm sống và gắn kết những người con xa quê. Thảo luận đa chiều, văn minh và ấm áp.
          </p>
          <div class="social-links">
            <a href="https://t.me" target="_blank" rel="noopener noreferrer" aria-label="Telegram" class="social-btn" title="Kênh Telegram">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"></line>
                <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
              </svg>
            </a>
            <a href="https://facebook.com" target="_blank" rel="noopener noreferrer" aria-label="Facebook" class="social-btn" title="Cộng đồng Facebook">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path>
              </svg>
            </a>
            <a href="mailto:admin@hoptacxavuive.com" aria-label="Email" class="social-btn" title="Hộp thư Admin">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                <polyline points="22,6 12,13 2,6"></polyline>
              </svg>
            </a>
          </div>
        </div>

        <!-- Cột 2: Điều hướng nhanh -->
        <div class="footer-nav-col">
          <h3 class="col-title">Khám phá</h3>
          <ul class="footer-links">
            <li>
              <router-link :to="{ name: 'Home' }">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Trang nhất</span>
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'LatestThreads' }">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Bài viết mới nhất</span>
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'MembersView' }">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Thành viên</span>
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'ConversationList' }">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Hộp thư</span>
              </router-link>
            </li>
          </ul>
        </div>

        <!-- Cột 3: Nội quy & Quy chế -->
        <div class="footer-policy-col">
          <h3 class="col-title">Quy định & Hỗ trợ</h3>
          <ul class="footer-links">
            <li>
              <a href="#" @click.prevent="openPolicy('rules')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Nội quy diễn đàn</span>
              </a>
            </li>
            <li>
              <a href="#" @click.prevent="openPolicy('terms')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Điều khoản dịch vụ</span>
              </a>
            </li>
            <li>
              <a href="#" @click.prevent="openPolicy('privacy')">
                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="link-icon">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
                <span>Chính sách bảo mật</span>
              </a>
            </li>
          </ul>
          <div class="server-status-pill">
            <span class="status-indicator"></span>
            <span>Hệ thống: Trực tuyến 24/7</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Thanh Bản Quyền Tinh Gọn Phía Dưới -->
    <div class="footer-bottom">
      <div class="container footer-bottom-inner">
        <div class="copyright-text">
          &copy; {{ currentYear }} <strong>HỢP TÁC XÃ VUI VẺ</strong>. Tất cả quyền được bảo lưu.
        </div>
        <div class="footer-meta-tag">
          Hợp Tác Xã Vui Vẻ - Cộng Đồng Thanh Niên Xa Mẹ
        </div>
      </div>
    </div>

    <!-- Nút Cuộn Lên Đầu Trang Nổi -->
    <transition name="fade-slide">
      <button 
        v-show="showScrollTop" 
        @click="scrollToTop" 
        class="scroll-top-btn" 
        aria-label="Cuộn lên đầu trang"
        title="Lên đầu trang"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="18 15 12 9 6 15"></polyline>
        </svg>
      </button>
    </transition>
  </footer>
</template>

<script>
export default {
  name: 'ForumFooter',
  data() {
    return {
      currentYear: new Date().getFullYear(),
      showScrollTop: false
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll, { passive: true })
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    handleScroll() {
      this.showScrollTop = window.scrollY > 300
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    },
    goToHome() {
      if (this.$route.name === 'Home') {
        this.scrollToTop()
      } else {
        this.$router.push({ name: 'Home' })
      }
    },
    openPolicy(type) {
      if (this.$route.name !== 'Home') {
        this.$router.push({ name: 'Home' })
      } else {
        this.scrollToTop()
      }
    }
  }
}
</script>

<style scoped>
.forum-footer {
  background-color: var(--primary, #1a507a);
  color: #c9d6e0;
  font-size: 0.95rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 90;
  margin-top: auto;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.footer-container {
  padding: 2.5rem 1rem 2rem 1rem;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2.2fr 1fr 1.2fr;
  gap: 2.5rem;
  align-items: start;
}

/* Cột Thương hiệu */
.footer-brand-col {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.brand-header {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  user-select: none;
  width: fit-content;
}

.brand-logo {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
  transition: transform 0.25s ease, border-color 0.25s ease;
}

.brand-header:hover .brand-logo {
  transform: scale(1.05);
  border-color: var(--accent, #f2930d);
}

.brand-titles {
  display: flex;
  flex-direction: column;
}

.brand-name {
  color: #ffffff;
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  margin: 0;
  line-height: 1.3;
}

.brand-slogan {
  color: #e2ebf3;
  font-size: 0.85rem;
  font-weight: 400;
  opacity: 0.9;
  letter-spacing: 0.2px;
}

.brand-desc {
  color: #b5c7d6;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0;
  max-width: 480px;
}

.social-links {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.social-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.08);
  color: #d1e0ec;
  border-radius: 50%;
  transition: all 0.25s ease;
}

.social-btn:hover {
  background-color: var(--accent, #f2930d);
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(242, 147, 13, 0.35);
}

/* Tiêu đề Cột */
.col-title {
  color: #ffffff;
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  padding-bottom: 0.4rem;
  border-bottom: 2px solid rgba(255, 255, 255, 0.1);
  position: relative;
}

.col-title::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 30px;
  height: 2px;
  background-color: var(--accent, #f2930d);
}

/* Danh sách liên kết */
.footer-links {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.footer-links a {
  color: #b5c7d6;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
  transition: all 0.2s ease;
  min-height: 32px;
}

.link-icon {
  color: var(--accent, #f2930d);
  opacity: 0.8;
  transition: transform 0.2s ease;
}

.footer-links a:hover {
  color: #ffffff;
  transform: translateX(4px);
}

.footer-links a:hover .link-icon {
  opacity: 1;
  transform: translateX(2px);
}

/* Badge Trạng thái Máy chủ */
.server-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background-color: rgba(255, 255, 255, 0.06);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.82rem;
  color: #c9d6e0;
  margin-top: 1.2rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.status-indicator {
  width: 8px;
  height: 8px;
  background-color: #2ecc71;
  border-radius: 50%;
  box-shadow: 0 0 8px #2ecc71;
}

/* Thanh Bản Quyền */
.footer-bottom {
  background-color: rgba(0, 0, 0, 0.2);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding: 1rem 0;
  font-size: 0.85rem;
}

.footer-bottom-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.copyright-text {
  color: #a8bccd;
}

.copyright-text strong {
  color: #ffffff;
}

.footer-meta-tag {
  color: #8da4b8;
  font-size: 0.82rem;
}

/* Nút Cuộn Lên Đầu Trang Nổi */
.scroll-top-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 44px;
  height: 44px;
  background-color: var(--accent, #f2930d);
  color: #ffffff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.3);
  transition: all 0.25s ease;
  z-index: 999;
}

.scroll-top-btn:hover {
  background-color: #e68200;
  transform: translateY(-3px) scale(1.05);
  box-shadow: 0 6px 18px rgba(242, 147, 13, 0.45);
}

.scroll-top-btn:active {
  transform: translateY(0) scale(0.96);
}

/* Hiệu ứng Fade Slide cho Nút Scroll */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.85);
}

/* Responsive */
@media (max-width: 992px) {
  .footer-grid {
    grid-template-columns: 1.5fr 1fr 1fr;
    gap: 2rem;
  }
}

@media (max-width: 768px) {
  .footer-container {
    padding: 2rem 1rem 1.5rem 1rem;
  }

  .footer-grid {
    grid-template-columns: 1fr;
    gap: 1.8rem;
  }

  .brand-header {
    margin: 0 auto;
    text-align: center;
    flex-direction: column;
    gap: 8px;
  }

  .brand-desc {
    text-align: center;
    margin: 0 auto;
  }

  .social-links {
    justify-content: center;
  }

  .col-title {
    text-align: center;
  }

  .col-title::after {
    left: 50%;
    transform: translateX(-50%);
  }

  .footer-links {
    align-items: center;
  }

  .footer-links a:hover {
    transform: none;
  }

  .server-status-pill {
    margin: 1rem auto 0 auto;
    display: flex;
    width: fit-content;
  }

  .footer-bottom-inner {
    flex-direction: column;
    text-align: center;
    gap: 0.5rem;
  }

  .scroll-top-btn {
    bottom: 18px;
    right: 18px;
    width: 40px;
    height: 40px;
  }
}
</style>
