export async function onRequest(context) {
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
