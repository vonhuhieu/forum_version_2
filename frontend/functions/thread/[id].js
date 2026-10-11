export async function onRequest(context) {
  const { request, params, next } = context
  const userAgent = request.headers.get('user-agent') || ''
  const isBot = /(facebookexternalhit|Facebot|Twitterbot|Pinterest|LinkedInBot|TelegramBot|WhatsApp|vkShare|Slackbot|Discordbot)/i.test(userAgent)

  if (isBot) {
    try {
      const id = params.id
      const url = new URL(request.url)
      const postId = url.searchParams.get('postId')
      const rootDomain = url.hostname.replace(/^www\./, '')
      const backendUrl = `https://api.${rootDomain}`
      const query = postId ? `?postId=${encodeURIComponent(postId)}` : ''
      const targetUrl = `${backendUrl}/api/share/thread/${id}${query}`

      const res = await fetch(targetUrl)
      const html = await res.text()
      return new Response(html, {
        status: res.status,
        headers: {
          'Content-Type': 'text/html; charset=utf-8',
          'Cache-Control': 's-maxage=3600, stale-while-revalidate'
        }
      })
    } catch (e) {
      return next()
    }
  }

  return next()
}
