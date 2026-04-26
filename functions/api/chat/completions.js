const JSON_HEADERS = {
  'Content-Type': 'application/json; charset=utf-8',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type'
};

function jsonResponse(status, payload, extraHeaders = {}) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: {
      ...JSON_HEADERS,
      ...extraHeaders
    }
  });
}

function normalizeBaseUrl(baseUrl) {
  if (typeof baseUrl !== 'string' || baseUrl.trim() === '') {
    throw new Error('Missing baseUrl');
  }

  const normalized = baseUrl.trim().replace(/\/+$/, '');
  const url = new URL(normalized);

  if (url.protocol !== 'https:' && url.hostname !== 'localhost' && url.hostname !== '127.0.0.1') {
    throw new Error('Only https endpoints are allowed');
  }

  return normalized;
}

export function onRequestOptions() {
  return new Response(null, {
    status: 204,
    headers: JSON_HEADERS
  });
}

export async function onRequestPost(context) {
  try {
    const { request } = context;
    const { baseUrl, apiKey, model, messages } = await request.json();

    if (typeof apiKey !== 'string' || apiKey.trim() === '') {
      return jsonResponse(400, { error: 'Missing apiKey' });
    }

    if (typeof model !== 'string' || model.trim() === '') {
      return jsonResponse(400, { error: 'Missing model' });
    }

    if (!Array.isArray(messages) || messages.length === 0) {
      return jsonResponse(400, { error: 'Missing messages' });
    }

    const upstreamUrl = `${normalizeBaseUrl(baseUrl)}/chat/completions`;
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
    });

    const body = await upstreamResponse.text();
    const contentType = upstreamResponse.headers.get('content-type') || 'application/json; charset=utf-8';

    return new Response(body, {
      status: upstreamResponse.status,
      headers: {
        'Content-Type': contentType,
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type'
      }
    });
  } catch (error) {
    return jsonResponse(500, {
      error: error instanceof Error ? error.message : 'Proxy request failed'
    });
  }
}

export function onRequest() {
  return jsonResponse(405, { error: 'Method Not Allowed' }, { Allow: 'POST, OPTIONS' });
}
