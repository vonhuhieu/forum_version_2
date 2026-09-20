/**
 * Tiện ích hỗ trợ Google OAuth2 và Identity Services
 */

export function getGoogleClientId() {
  const clientId = process.env.VUE_APP_GOOGLE_CLIENT_ID
  if (clientId && !clientId.startsWith('${')) {
    return clientId.trim()
  }
  return ''
}

export function getGoogleRedirectUri() {
  return `${window.location.origin}/auth/google/callback`
}

/**
 * Điều hướng người dùng sang trang chọn tài khoản Google theo chuẩn OAuth2 Redirect Flow
 * @param {string} source - Nguồn yêu cầu ('login' | 'register')
 */
export function redirectToGoogleOAuth(source = 'login', fallbackEmail = null) {
  const clientId = getGoogleClientId()
  const redirectUri = getGoogleRedirectUri()

  sessionStorage.setItem('google_oauth_source', source)

  // 1. Chế độ kiểm thử (Dev / Testing Fallback) khi chưa có Client ID
  if (!clientId) {
    const testEmail = fallbackEmail || window.prompt(
      'Hệ thống đang ở chế độ thử nghiệm (Chưa cấu hình VUE_APP_GOOGLE_CLIENT_ID).\n' +
      'Vui lòng nhập địa chỉ Gmail để thử nghiệm quy trình xác thực:',
      'hoptacxavuive.member@gmail.com'
    )
    if (testEmail && testEmail.trim()) {
      window.location.href = `${redirectUri}?code=mock-google-code:${encodeURIComponent(testEmail.trim())}&state=${source}`
    }
    return
  }

  // 2. Chuyển hướng sang Google OAuth2 Authorization Endpoint
  const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth')
  authUrl.searchParams.set('client_id', clientId)
  authUrl.searchParams.set('redirect_uri', redirectUri)
  authUrl.searchParams.set('response_type', 'code')
  authUrl.searchParams.set('scope', 'openid email profile')
  authUrl.searchParams.set('prompt', 'select_account')
  authUrl.searchParams.set('state', source)

  window.location.href = authUrl.toString()
}

export function initGoogleAuth(onCredentialCallback) {
  const clientId = getGoogleClientId()

  if (window.google && window.google.accounts && window.google.accounts.id) {
    if (clientId) {
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
  const clientId = getGoogleClientId()

  if (clientId && window.google && window.google.accounts && window.google.accounts.id) {
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
