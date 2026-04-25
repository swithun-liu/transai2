import { createServer } from 'node:http'

const PORT = Number(process.env.TRANSAI_PROXY_PORT || 8081)

function writeJson(res, statusCode, payload) {
  const body = JSON.stringify(payload)
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type'
  })
  res.end(body)
}

function writeText(res, statusCode, contentType, text) {
  res.writeHead(statusCode, {
    'Content-Type': contentType,
    'Content-Length': Buffer.byteLength(text),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type'
  })
  res.end(text)
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

async function readJsonBody(req) {
  const chunks = []
  for await (const chunk of req) {
    chunks.push(chunk)
  }
  const raw = Buffer.concat(chunks).toString('utf-8').trim()
  if (!raw) {
    return {}
  }
  return JSON.parse(raw)
}

const server = createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type'
    })
    res.end()
    return
  }

  if (req.url !== '/api/chat/completions') {
    writeJson(res, 404, { error: 'Not Found' })
    return
  }

  if (req.method !== 'POST') {
    writeJson(res, 405, { error: 'Method Not Allowed' })
    return
  }

  try {
    const { baseUrl, apiKey, model, messages } = await readJsonBody(req)

    if (typeof apiKey !== 'string' || apiKey.trim() === '') {
      writeJson(res, 400, { error: 'Missing apiKey' })
      return
    }
    if (typeof model !== 'string' || model.trim() === '') {
      writeJson(res, 400, { error: 'Missing model' })
      return
    }
    if (!Array.isArray(messages) || messages.length === 0) {
      writeJson(res, 400, { error: 'Missing messages' })
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
    const contentType =
      upstreamResponse.headers.get('content-type') || 'application/json; charset=utf-8'
    writeText(res, upstreamResponse.status, contentType, responseText)
  } catch (error) {
    writeJson(res, 500, {
      error: error instanceof Error ? error.message : 'Proxy request failed'
    })
  }
})

server.listen(PORT, '127.0.0.1', () => {
  console.log(`TransAI local AI proxy running at http://127.0.0.1:${PORT}`)
})
