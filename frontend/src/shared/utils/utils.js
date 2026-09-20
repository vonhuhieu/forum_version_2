import { ROLES } from './constants';

/**
 * Kiểm tra xem người dùng hiện tại có vai trò chưa chính thức (chờ duyệt) hay không.
 * @returns {boolean}
 */
export function isNonOfficialUser() {
  const userStr = localStorage.getItem('user');
  if (!userStr) return false;
  try {
    const user = JSON.parse(userStr);
    return !!(user && user.roles && user.roles.includes(ROLES.NON_OFFICIAL));
  } catch (e) {
    return false;
  }
}

/**
 * Rút gọn chuỗi nếu độ dài vượt quá giới hạn.
 * @param {string} str - Chuỗi cần rút gọn
 * @param {number} limit - Độ dài tối đa
 * @returns {string}
 */
export function truncateString(str, limit = 8) {
  if (!str) return '';
  if (str.length <= limit) return str;
  return str.slice(0, limit) + '...';
}

/**
 * Kiểm tra xem người dùng hiện tại có vai trò Admin hoặc Super Admin hay không.
 * @returns {boolean}
 */
export function isAdminOrSuperAdmin() {
  const userStr = localStorage.getItem('user');
  if (!userStr) return false;
  try {
    const user = JSON.parse(userStr);
    return !!(user && user.roles && (user.roles.includes(ROLES.ADMIN) || user.roles.includes(ROLES.SUPER_ADMIN)));
  } catch (e) {
    return false;
  }
}

export const THREAD_SCOPES = {
  PUBLIC: 'PUBLIC',
  INTERNAL: 'INTERNAL'
};

import { getBackendBaseUrl } from '@/shared/services/api.service';

/**
 * Kiểm tra xem chuỗi đại diện avatar có phải là đường dẫn URL ảnh hay không.
 * @param {string} avatar
 * @returns {boolean}
 */
export function isAvatarUrl(avatar) {
  if (!avatar) return false;
  return avatar.startsWith('http://') || avatar.startsWith('https://') || avatar.startsWith('/');
}

/**
 * Định dạng URL của tài nguyên tải lên (ảnh, video) sang URL đầy đủ của VPS Backend nếu là đường dẫn relative /uploads/...
 * @param {string} url
 * @returns {string}
 */
export function formatUploadUrl(url) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  if (url.startsWith('/uploads/')) {
    return `${getBackendBaseUrl()}${url}`;
  }
  if (url.startsWith('uploads/')) {
    return `${getBackendBaseUrl()}/${url}`;
  }
  return url;
}

/**
 * Định dạng URL avatar nếu avatar là đường dẫn relative /uploads/... hoặc uploads/...
 * @param {string} avatar
 * @returns {string}
 */
export function formatAvatarUrl(avatar) {
  if (!avatar) return '';
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar;
  if (avatar.startsWith('/uploads/')) {
    return `${getBackendBaseUrl()}${avatar}`;
  }
  if (avatar.startsWith('uploads/')) {
    return `${getBackendBaseUrl()}/${avatar}`;
  }
  return avatar;
}

/**
 * Lấy giá trị nhập liệu tức thời từ sự kiện input, giải quyết vấn đề gạch chân Telex (IME Composition) trong Vue.
 * @param {Event} event - Sự kiện input
 * @returns {string} - Giá trị nhập liệu thuần túy của input target
 */
export function getImeValue(event) {
  if (event && event.target) {
    return event.target.value;
  }
  return '';
}

/**
 * Lấy domain/origin hiện tại của trình duyệt người dùng truy cập (VD: http://localhost:5173 hoặc https://htxslvn.com).
 * @returns {string}
 */
export function getCurrentOrigin() {
  if (typeof window !== 'undefined' && window.location) {
    return window.location.origin.replace(/^(https?:\/\/)www\./i, '$1');
  }
  return '';
}

/**
 * Trả về chuỗi HTML SVG đại diện cho Badge Tích xanh Uy tín
 * @param {Object|boolean} userOrIsVerified - Đối tượng user hoặc boolean isVerified
 * @param {string} size - Kích thước của icon SVG (mặc định '16px')
 * @returns {string} - Chuỗi HTML đại diện cho badge tích xanh hoặc chuỗi rỗng
 */
export function getVerifiedBadgeSvgHtml(userOrIsVerified, size = '16px') {
  let isVerified = false;
  if (typeof userOrIsVerified === 'boolean') {
    isVerified = userOrIsVerified;
  } else if (userOrIsVerified && typeof userOrIsVerified === 'object') {
    if (userOrIsVerified.isVerifiedBadge) {
      isVerified = true;
    } else if (Array.isArray(userOrIsVerified.roles)) {
      if (userOrIsVerified.roles.includes('ROLE_SUPER_ADMIN') || userOrIsVerified.roles.includes('ROLE_ADMIN')) {
        isVerified = true;
      }
    }
  }

  if (!isVerified) return '';

  return `<span class="verified-badge-wrapper" title="Tài khoản Uy tín / Quản trị viên" style="display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; line-height: 1; margin-left: 4px; pointer-events: none; user-select: none;"><svg class="verified-badge-icon" style="width: ${size}; height: ${size}; display: inline-block; vertical-align: middle; flex-shrink: 0; pointer-events: none;" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="10" fill="#1877F2"/><path d="M8.5 12.5L10.5 14.5L15.5 9.5" stroke="white" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg></span>`;
}

import { reactive } from 'vue';

export const publicSettingsState = reactive({
  loaded: false,
  settings: {}
});

/**
 * Tải cài đặt hệ thống public và lưu vào reactive state
 */
export async function loadPublicSettings() {
  if (publicSettingsState.loaded) return publicSettingsState.settings;
  try {
    const settingServiceModule = await import('@/shared/services/setting.service');
    const res = await settingServiceModule.default.getPublicSettings();
    if (res && res.data) {
      Object.assign(publicSettingsState.settings, res.data);
    }
    publicSettingsState.loaded = true;
    return publicSettingsState.settings;
  } catch (e) {
    console.error('Failed to load public settings:', e);
    return {};
  }
}

/**
 * Kiểm tra xem có được phép hiển thị nút Đăng bài trên màn hình chỉ định hay không
 * @param {'home'|'latest'|'pinned'|'category'} screenName - Tên định danh màn hình
 * @returns {boolean}
 */
export function canShowPostButtonOnScreen(screenName) {
  const userStr = localStorage.getItem('user');
  if (!userStr) return false;
  if (isNonOfficialUser()) return false;

  const isAdmin = isAdminOrSuperAdmin();
  const settings = publicSettingsState.settings;
  const settingKey = `post_button_${screenName}`;
  const settingVal = settings[settingKey];

  // Nếu là màn hình chú ý/ghim (pinned): mặc định chỉ Quản trị viên
  if (screenName === 'pinned') {
    const val = settingVal || 'ADMIN_ONLY';
    if (val === 'DISABLED') return false;
    if (val === 'ADMIN_ONLY') return isAdmin;
    return true;
  }

  // Các màn hình khác: mặc định ALL
  const val = settingVal || 'ALL';
  if (val === 'DISABLED') return false;
  if (val === 'ADMIN_ONLY') return isAdmin;
  return true;
}

/**
 * Render chuỗi HTML cho thẻ xem trước liên kết Gofile (Rich Link Preview Card phong cách Xamvn)
 * @param {string} url - Đường dẫn Gofile (ví dụ https://gofile.io/d/kWoURoaM)
 * @returns {string} - Chuỗi HTML của Card
 */
export function renderGofileCardHtml(url) {
  const cleanUrl = (url || '').trim();
  return `
    <div class="gofile-unfurl-wrapper">
      <a href="${cleanUrl}" target="_blank" rel="noopener noreferrer" class="gofile-unfurl-card" title="Mở video/tệp tin trên Gofile trong tab mới">
        <div class="gofile-unfurl-figure">
          <svg class="gofile-icon-svg" viewBox="0 0 64 48" width="56" height="42" xmlns="http://www.w3.org/2000/svg">
            <path d="M4 14h12M2 24h18M6 34h14" stroke="#cbd5e1" stroke-width="2.5" stroke-linecap="round"/>
            <path d="M12 18h10M14 30h8" stroke="#e2e8f0" stroke-width="2" stroke-linecap="round"/>
            <path d="M22 10h12l4 4h18a3 3 0 0 1 3 3v20a3 3 0 0 1-3 3H22a3 3 0 0 1-3-3V13a3 3 0 0 1 3-3z" fill="#f59e0b"/>
            <path d="M19 20h38v17a3 3 0 0 1-3 3H22a3 3 0 0 1-3-3V20z" fill="#fbbf24"/>
            <path d="M25 12h24v8H25z" fill="#ffffff" opacity="0.9" rx="1"/>
          </svg>
        </div>
        <div class="gofile-unfurl-main">
          <div class="gofile-unfurl-title">Gofile — Cloud Storage Made Simple</div>
          <div class="gofile-unfurl-desc">Secure, fast and free cloud storage solution. Upload and share files instantly.</div>
          <div class="gofile-unfurl-minor">
            <span class="gofile-favicon-dot"></span>
            <span class="gofile-domain">gofile.io</span>
          </div>
        </div>
      </a>
    </div>
  `.trim();
}

/**
 * Quét và chuyển đổi tất cả các liên kết Gofile trong chuỗi HTML bài viết thành Card Gofile Preview
 * @param {string} html - Chuỗi HTML bài viết
 * @returns {string} - Chuỗi HTML đã được chuyển đổi
 */
export function processGofileLinks(html) {
  if (!html || typeof html !== 'string' || !html.includes('gofile.io')) return html || '';
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const gofileLinks = doc.querySelectorAll('a[href*="gofile.io"]');
    if (gofileLinks.length === 0) return html;

    gofileLinks.forEach(link => {
      // Nếu link này đã nằm trong gofile-unfurl-card thì bỏ qua không lồng thêm
      if (link.closest('.gofile-unfurl-wrapper') || link.classList.contains('gofile-unfurl-card')) {
        return;
      }

      const href = link.getAttribute('href');
      if (!href) return;

      const cardHtml = renderGofileCardHtml(href);
      const temp = doc.createElement('div');
      temp.innerHTML = cardHtml;
      const cardElement = temp.firstElementChild;

      const parent = link.parentElement;
      if (parent && parent.tagName === 'P' && parent.textContent.trim() === link.textContent.trim()) {
        parent.replaceWith(cardElement);
      } else {
        link.replaceWith(cardElement);
      }
    });

    return doc.body.innerHTML;
  } catch (err) {
    console.error('Lỗi khi chuyển đổi liên kết Gofile sang Card:', err);
    return html;
  }
}

