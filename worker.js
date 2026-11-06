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

    if (url.pathname === '/privacy-policy') {
      const request2 = new Request(new URL('/privacy-policy-app.html', url), request);
      return env.ASSETS.fetch(request2);
    }

    return env.ASSETS.fetch(request);
  }
};
