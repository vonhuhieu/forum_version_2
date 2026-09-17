module.exports = async (req, res) => {
  try {
    // Dynamic Origin Detection: Tự động nhận diện domain backend từ domain frontend hiện tại
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

    const response = await fetch(`${backendUrl}/api/sitemap.xml`)
    if (!response.ok) {
      return res.status(response.status).send('Error fetching sitemap from backend')
    }

    const xml = await response.text()
    res.setHeader('Content-Type', 'application/xml; charset=utf-8')
    res.setHeader('Cache-Control', 'public, max-age=3600, s-maxage=3600, stale-while-revalidate=86400')
    return res.status(200).send(xml)
  } catch (err) {
    console.error('Sitemap proxy error:', err)
    return res.status(500).send('Error fetching sitemap')
  }
}
