function sendJson(res, statusCode, payload) {
  res.status(statusCode).json(payload)
}

function normalizeBaseUrl(baseUrl) {
  if (typeof baseUrl !== 'string' || baseUrl.trim() === '') {
    throw new Error('Missing baseUrl')
  }

  const normalized = baseUrl.trim().replace(/\/+$/, '')
  const url = new URL(normalized)

  if (url.protocol !== 'https:' && url.hostname !== 'localhost' && url.hostname !== '127.0.0.1') {
    throw new Error('Only https endpoints are allowed')
  }

  return normalized
}

module.exports = async function handler(req, res) {
  if (req.method === 'OPTIONS') {
    res.status(204).end()
    return
  }

  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST, OPTIONS')
    sendJson(res, 405, { error: 'Method Not Allowed' })
    return
  }

  try {
    const { baseUrl, apiKey, model, messages } = req.body ?? {}

    if (typeof apiKey !== 'string' || apiKey.trim() === '') {
      sendJson(res, 400, { error: 'Missing apiKey' })
      return
    }

    if (typeof model !== 'string' || model.trim() === '') {
      sendJson(res, 400, { error: 'Missing model' })
      return
    }

    if (!Array.isArray(messages) || messages.length === 0) {
      sendJson(res, 400, { error: 'Missing messages' })
      return
    }

    const upstreamUrl = `${normalizeBaseUrl(baseUrl)}/chat/completions`
    const upstreamResponse = await fetch(upstreamUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey.trim()}`
      },
      body: JSON.stringify({
        model: model.trim(),
        messages
      })
    })

    const responseText = await upstreamResponse.text()
    const contentType = upstreamResponse.headers.get('content-type') || 'application/json; charset=utf-8'

    res.status(upstreamResponse.status)
    res.setHeader('Content-Type', contentType)
    res.send(responseText)
  } catch (error) {
    sendJson(res, 500, {
      error: error instanceof Error ? error.message : 'Proxy request failed'
    })
  }
}
