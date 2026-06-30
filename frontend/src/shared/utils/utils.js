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
