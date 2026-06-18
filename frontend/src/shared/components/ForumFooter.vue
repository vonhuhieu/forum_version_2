<template>
  <footer class="forum-footer">
    <div class="container footer-container">
      <div class="footer-columns">
        <!-- Cột 1: Giới thiệu & Logo -->
        <div class="footer-col brand-col">
          <div class="footer-logo" @click="$router.push({ name: 'Home' })">HTXSL</div>
          <p class="brand-description">
            Hợp tác xã Sinh Lý - Diễn đàn thảo luận và chia sẻ kiến thức, kinh nghiệm sống, sức khỏe và các mối quan hệ xã hội một cách văn minh, khách quan.
          </p>
          <div class="social-links">
            <a href="#" aria-label="Telegram" class="social-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
            </a>
            <a href="#" aria-label="Facebook" class="social-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path></svg>
            </a>
            <a href="#" aria-label="Twitter" class="social-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path></svg>
            </a>
          </div>
        </div>

        <!-- Cột 2: Điều hướng -->
        <div class="footer-col links-col">
          <h3>Điều hướng</h3>
          <ul>
            <li>
              <router-link :to="{ name: 'Home' }">
                <span class="link-arrow">›</span> Trang nhất
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'LatestThreads' }">
                <span class="link-arrow">›</span> Con sò mới
              </router-link>
            </li>
            <li>
              <router-link :to="{ name: 'ConversationList' }">
                <span class="link-arrow">›</span> Hộp thư
              </router-link>
            </li>
          </ul>
        </div>

        <!-- Cột 3: Quy định & Hướng dẫn -->
        <div class="footer-col links-col">
          <h3>Quy định & Trợ giúp</h3>
          <ul>
            <li><a href="#" @click.prevent><span class="link-arrow">›</span> Nội quy diễn đàn</a></li>
            <li><a href="#" @click.prevent><span class="link-arrow">›</span> Điều khoản dịch vụ</a></li>
            <li><a href="#" @click.prevent><span class="link-arrow">›</span> Chính sách bảo mật</a></li>
            <li><a href="#" @click.prevent><span class="link-arrow">›</span> Hỗ trợ người dùng</a></li>
          </ul>
        </div>

        <!-- Cột 4: Liên hệ & Hỗ trợ -->
        <div class="footer-col contact-col">
          <h3>Liên hệ & Hỗ trợ</h3>
          <div class="contact-info">
            <p class="contact-email">
              <svg class="contact-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path><polyline points="22,6 12,13 2,6"></polyline></svg>
              <span>Email: <a href="mailto:contact@htxsl.com">contact@htxsl.com</a></span>
            </p>
            <p class="server-status">
              <span class="status-dot green"></span>
              <span class="status-text">Máy chủ: Hoạt động bình thường</span>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Thanh Bản Quyền Phía Dưới -->
    <div class="footer-bottom">
      <div class="container footer-bottom-container">
        <p class="copyright">
          &copy; {{ currentYear }} HTXSL. Tất cả quyền được bảo lưu.
        </p>
        <p class="credits">
          Thiết kế dựa trên nền tảng Vue 3 & Spring Boot.
        </p>
      </div>
    </div>

    <!-- Nút Cuộn Lên Đầu Trang -->
    <transition name="fade">
      <button 
        v-show="showScrollTop" 
        @click="scrollToTop" 
        class="scroll-top-btn" 
        aria-label="Cuộn lên đầu trang"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"></polyline></svg>
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
    window.addEventListener('scroll', this.handleScroll)
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
    }
  }
}
</script>

<style scoped>
.forum-footer {
  background-color: var(--primary);
  color: #b4c7d7;
  font-size: 0.95rem;
  border-top: 1px solid var(--primary-dark);
  position: relative;
  z-index: 100;
  margin-top: auto; /* Push to bottom in flex containers */
}

.footer-container {
  padding: 4rem 1rem 3rem 1rem;
}

.footer-columns {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1.5fr;
  gap: 2.5rem;
}

.footer-col h3 {
  color: #ffffff;
  font-size: 1.15rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid rgba(255, 255, 255, 0.08);
  position: relative;
}

.footer-col h3::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 40px;
  height: 2px;
  background-color: var(--accent, #f2930d);
}

/* Cột Thương hiệu */
.brand-col {
  padding-right: 1.5rem;
}

.footer-logo {
  font-size: 2.2rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 1px;
  margin-bottom: 1.2rem;
  cursor: pointer;
  display: inline-block;
  transition: transform 0.3s ease;
}

.footer-logo:hover {
  transform: scale(1.03);
}

.brand-description {
  line-height: 1.6;
  margin-bottom: 1.5rem;
  color: #c9d6e0;
}

.social-links {
  display: flex;
  gap: 0.75rem;
}

.social-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.05);
  color: #b4c7d7;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.social-icon:hover {
  background-color: var(--accent, #f2930d);
  color: #ffffff;
  transform: translateY(-3px);
  box-shadow: 0 4px 10px rgba(242, 147, 13, 0.2);
}

/* Cột liên kết */
.links-col ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.links-col li {
  margin-bottom: 0.8rem;
}

.links-col a {
  color: #b4c7d7;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  transition: all 0.25s ease;
}

.links-col a .link-arrow {
  margin-right: 6px;
  font-size: 1.1rem;
  transition: transform 0.25s ease;
  color: var(--accent, #f2930d);
}

.links-col a:hover {
  color: #ffffff;
  transform: translateX(4px);
}

.links-col a:hover .link-arrow {
  transform: translateX(2px);
}

/* Cột liên hệ */
.contact-col {
  color: #a0b6c9;
}

.contact-info p {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 1rem;
}

.contact-email a {
  color: #ffffff;
  text-decoration: none;
  border-bottom: 1px dashed rgba(255, 255, 255, 0.3);
  transition: border-color 0.2s;
}

.contact-email a:hover {
  border-bottom-color: var(--accent, #f2930d);
}

.contact-icon {
  color: var(--accent, #f2930d);
}

.server-status {
  margin-top: 1.2rem;
  font-size: 0.85rem;
  background-color: rgba(255, 255, 255, 0.03);
  padding: 8px 12px;
  border-radius: 4px;
  border-left: 3px solid #2ecc71;
  display: inline-flex !important;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-dot.green {
  background-color: #2ecc71;
  box-shadow: 0 0 8px #2ecc71;
}

.status-text {
  color: #e2f0fb;
}

/* Thanh Bản Quyền Phía Dưới */
.footer-bottom {
  background-color: var(--primary-light);
  padding: 1.5rem 1rem;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.footer-bottom-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.85rem;
  color: #c9d6e0;
}

.copyright {
  margin: 0;
}

.credits {
  margin: 0;
}

.credits .heart {
  color: #e74c3c;
  display: inline-block;
  animation: heartbeat 1.5s infinite;
}

@keyframes heartbeat {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

/* Nút Cuộn Lên Đầu Trang */
.scroll-top-btn {
  position: fixed;
  bottom: 25px;
  right: 25px;
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
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.25);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 999;
}

.scroll-top-btn:hover {
  background-color: #e68200;
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 6px 20px rgba(242, 147, 13, 0.45);
}

.scroll-top-btn:active {
  transform: translateY(-1px) scale(0.98);
}

/* Hiệu ứng Fade cho Transition */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: scale(0.8) translateY(10px);
}

/* Responsive */
@media (max-width: 992px) {
  .footer-columns {
    grid-template-columns: 1.5fr 1fr 1fr;
  }
  .contact-col {
    grid-column: span 3;
  }
}

@media (max-width: 768px) {
  .footer-columns {
    grid-template-columns: 1fr;
    gap: 2rem;
  }
  .contact-col {
    grid-column: span 1;
  }
  .brand-col {
    padding-right: 0;
  }
  .footer-container {
    padding: 3rem 1rem 2rem 1rem;
  }
  .footer-bottom-container {
    flex-direction: column;
    text-align: center;
  }
}
</style>
