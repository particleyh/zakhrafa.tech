export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === '/api/styles.json') {
      const data = {
        version: 1,
        updated: new Date().toISOString().split('T')[0],
        totalStyles: 164,
        arabicCount: 56,
        englishCount: 49,
        styles: []
      };
      return new Response(JSON.stringify(data, null, 2), {
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
          'Access-Control-Allow-Origin': '*',
          'Cache-Control': 'public, max-age=86400'
        }
      });
    }

    if (url.pathname === '/__worker-canary') {
      return new Response('NEW-WORKER-022be9b', {
        headers: { 'Cache-Control': 'no-store' }
      });
    }

    if (url.pathname === '/privacy-policy.html') {
      const req = new Request(new URL('/privacy-policy', url), request);
      const res = await env.ASSETS.fetch(req);
      const headers = new Headers(res.headers);
      headers.set('Cache-Control', 'no-store');
      return new Response(res.body, { status: res.status, statusText: res.statusText, headers });
    }

    if (url.pathname === '/privacy-policy') {
      const request2 = new Request(new URL('/privacy-policy-app', url), request);
      const res = await env.ASSETS.fetch(request2);
      const headers = new Headers(res.headers);
      headers.set('Cache-Control', 'no-store');
      return new Response(res.body, { status: res.status, statusText: res.statusText, headers });
    }

    return env.ASSETS.fetch(request);
  }
};
