module.exports = async (req, res) => {
  try {
    const host = req.headers['x-forwarded-host'] || req.headers.host || ''
    const rootDomain = host.replace(/^www\./, '').split(':')[0]

    let backendUrl = process.env.VUE_APP_BACKEND_URL
    if (!backendUrl || backendUrl.startsWith('${')) {
      if (rootDomain === 'localhost' || rootDomain === '127.0.0.1') {
        backendUrl = 'http://localhost:8080'
      } else {
        backendUrl = `https://api.${rootDomain}`
      }
    }

    const path = req.query.path || ''
    return res.redirect(301, `${backendUrl}/uploads/${path}`)
  } catch (err) {
    console.error('Uploads proxy error:', err)
    return res.status(500).send('Error redirecting upload resource')
  }
}
