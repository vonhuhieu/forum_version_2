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
 * Thử giải mã nhanh bằng Native Hardware (createImageBitmap / HTMLImageElement + Canvas)
 * Thời gian xử lý: ~15-30ms
 * @param {File} file 
 * @returns {Promise<Blob|null>}
 */
async function fastNativeDecode(file) {
  try {
    let imgBitmap = null
    if (typeof createImageBitmap === 'function') {
      try {
        imgBitmap = await createImageBitmap(file)
      } catch (e) {
        // Trình duyệt không hỗ trợ decode HEIC qua createImageBitmap
      }
    }

    if (!imgBitmap) {
      const url = URL.createObjectURL(file)
      const img = new Image()
      const loaded = await new Promise((resolve) => {
        img.onload = () => resolve(true)
        img.onerror = () => resolve(false)
        img.src = url
      })
      URL.revokeObjectURL(url)

      if (!loaded) return null
      imgBitmap = img
    }

    const MAX_DIM = 2048
    let width = imgBitmap.width || imgBitmap.naturalWidth
    let height = imgBitmap.height || imgBitmap.naturalHeight
    if (!width || !height) return null

    if (width > MAX_DIM || height > MAX_DIM) {
      if (width > height) {
        height = Math.round((height * MAX_DIM) / width)
        width = MAX_DIM
      } else {
        width = Math.round((width * MAX_DIM) / height)
        height = MAX_DIM
      }
    }

    const canvas = (typeof OffscreenCanvas !== 'undefined')
      ? new OffscreenCanvas(width, height)
      : document.createElement('canvas')
    
    canvas.width = width
    canvas.height = height

    const ctx = canvas.getContext('2d')
    ctx.drawImage(imgBitmap, 0, 0, width, height)

    if (typeof imgBitmap.close === 'function') {
      imgBitmap.close()
    }

    if (canvas.convertToBlob) {
      return await canvas.convertToBlob({ type: 'image/jpeg', quality: 0.85 })
    }

    return await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.85))
  } catch (err) {
    return null
  }
}

/**
 * Chuyển đổi file HEIC sang JPEG siêu tốc (<50ms)
 * Ưu tiên giải mã phần cứng Native GPU/Canvas, fallback heic2any nếu cần.
 * @param {File} file 
 * @returns {Promise<File>}
 */
export async function convertHeicToJpegIfNeeded(file) {
  if (!isHeicFile(file)) {
    return file
  }

  // 1. Thử giải mã siêu tốc bằng Native Hardware (<30ms)
  const nativeBlob = await fastNativeDecode(file)
  if (nativeBlob) {
    const newName = file.name ? file.name.replace(/\.(heic|heif)$/i, '.jpg') : 'image.jpg'
    return new File([nativeBlob], newName, { type: 'image/jpeg' })
  }

  // 2. Fallback sang heic2any nếu trình duyệt không có bộ giải mã Native
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
    return file
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
