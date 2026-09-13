module.exports = async (req, res) => {
  try {
    const { id, postId } = req.query
    if (!id) {
      return res.status(400).send('Missing thread id')
    }

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

    const query = postId ? `?postId=${encodeURIComponent(postId)}` : ''
    const targetUrl = `${backendUrl}/api/share/thread/${id}${query}`

    const response = await fetch(targetUrl)
    const html = await response.text()

    res.setHeader('Content-Type', 'text/html; charset=utf-8')
    res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate')
    return res.status(response.status).send(html)
  } catch (err) {
    console.error('Share handler error:', err)
    return res.status(500).send('Error fetching share metadata')
  }
}
