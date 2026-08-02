import heic2any from 'heic2any'

/**
 * Kiểm tra xem file có thuộc định dạng HEIC / HEIF hay không
 * @param {File} file 
 * @returns {boolean}
 */
export function isHeicFile(file) {
  if (!file) return false
  const name = file.name || ''
  const type = (file.type || '').toLowerCase()
  const isHeicExt = /\.(heic|heif)$/i.test(name)
  const isHeicType = type.includes('heic') || type.includes('heif')
  return isHeicExt || isHeicType
}

/**
 * Chuyển đổi file HEIC sang định dạng JPEG nếu cần thiết
 * @param {File} file 
 * @returns {Promise<File>}
 */
export async function convertHeicToJpegIfNeeded(file) {
  if (!isHeicFile(file)) {
    return file
  }

  try {
    const result = await heic2any({
      blob: file,
      toType: 'image/jpeg',
      quality: 0.85
    })

    const blob = Array.isArray(result) ? result[0] : result
    const newName = file.name ? file.name.replace(/\.(heic|heif)$/i, '.jpg') : 'image.jpg'
    
    return new File([blob], newName, { type: 'image/jpeg' })
  } catch (error) {
    console.error('Lỗi khi chuyển đổi HEIC sang JPEG:', error)
    return file // Trả về file gốc nếu quá trình chuyển đổi bị lỗi
  }
}

/**
 * Xử lý danh sách file trước khi tải lên (tự động chuyển đổi các file HEIC song song)
 * @param {File[]} files 
 * @returns {Promise<File[]>}
 */
export async function processFilesForUpload(files) {
  if (!files || !files.length) return []
  return Promise.all(files.map(file => convertHeicToJpegIfNeeded(file)))
}
