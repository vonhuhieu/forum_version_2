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
    return window.location.origin;
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
