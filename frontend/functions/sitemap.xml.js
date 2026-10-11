export async function onRequest(context) {
  try {
    const url = new URL(context.request.url)
    const rootDomain = url.hostname.replace(/^www\./, '')
    const backendUrl = `https://api.${rootDomain}`

    const res = await fetch(`${backendUrl}/api/sitemap.xml`)
    if (!res.ok) {
      return new Response('Error fetching sitemap from backend', { status: res.status })
    }

    const xml = await res.text()
    return new Response(xml, {
      status: 200,
      headers: {
        'Content-Type': 'application/xml; charset=utf-8',
        'Cache-Control': 'public, max-age=3600, s-maxage=3600, stale-while-revalidate=86400'
      }
    })
  } catch (err) {
    return new Response('Error fetching sitemap', { status: 500 })
  }
}
