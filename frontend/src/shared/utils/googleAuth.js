/**
 * Tiện ích hỗ trợ Google Identity Services (GIS) cho đăng nhập/đăng ký nhanh bằng Google
 */
export function initGoogleAuth(onCredentialCallback) {
  const clientId = process.env.VUE_APP_GOOGLE_CLIENT_ID

  if (window.google && window.google.accounts && window.google.accounts.id) {
    if (clientId && !clientId.startsWith('${')) {
      try {
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: onCredentialCallback,
          auto_select: false,
          cancel_on_tap_outside: true
        })
      } catch (e) {
        console.warn('Google Identity initialization error:', e)
      }
    }
  }
}

export function promptGoogleLogin(onCredentialCallback, fallbackEmail) {
  const clientId = process.env.VUE_APP_GOOGLE_CLIENT_ID

  // 1. Nếu có Client ID và thư viện Google sẵn sàng
  if (clientId && !clientId.startsWith('${') && window.google && window.google.accounts && window.google.accounts.id) {
    try {
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: onCredentialCallback,
        auto_select: false,
        cancel_on_tap_outside: true
      })

      window.google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          console.log('Google One Tap prompt was not displayed or skipped:', notification.getNotDisplayedReason?.())
        }
      })
      return
    } catch (err) {
      console.warn('Google prompt error, falling back:', err)
    }
  }

  // 2. Chế độ kiểm thử (Dev / Testing Fallback) khi chưa thiết lập Client ID trên Google Cloud
  const testEmail = fallbackEmail || window.prompt(
    'Hệ thống đang ở chế độ thử nghiệm (Chưa có VUE_APP_GOOGLE_CLIENT_ID).\n' +
    'Vui lòng nhập địa chỉ Gmail để thử nghiệm quy trình đăng nhập/đăng ký như Google thật:',
    'hoptacxavuive.member@gmail.com'
  )

  if (testEmail && testEmail.trim()) {
    onCredentialCallback({
      credential: `mock-google-token:${testEmail.trim()}`
    })
  }
}
