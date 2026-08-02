/**
 * Utility module xử lý file trước khi upload.
 * Chuyển giao toàn bộ việc nhận diện và convert ảnh HEIC sang phía Backend Java
 * giúp Frontend di động đạt tốc độ 0ms và không bao giờ bị treo Promise.
 */

export function isHeicFile(file) {
  if (!file) return false
  const name = file.name || ''
  const type = (file.type || '').toLowerCase()
  const isHeicExt = /\.(heic|heif)$/i.test(name)
  const isHeicType = type.includes('heic') || type.includes('heif')
  return isHeicExt || isHeicType
}

export async function convertHeicToJpegIfNeeded(file) {
  return file
}

export async function processFilesForUpload(files) {
  return files || []
}
