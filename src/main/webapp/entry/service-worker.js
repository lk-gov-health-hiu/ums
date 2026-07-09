// App-shell cache only — API calls (/ums/api/*) always go to the network;
// offline queuing for the actual submission happens in app.js (localStorage),
// not here. Keeps the daily-entry page loadable with no connectivity.
const CACHE_NAME = 'ums-entry-v1';
const SHELL = [
    '/ums/entry/',
    '/ums/entry/index.html',
    '/ums/entry/app.js',
    '/ums/entry/manifest.json'
];

self.addEventListener('install', (event) => {
    event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(SHELL)));
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
        )
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);
    if (url.pathname.startsWith('/ums/api/')) {
        return; // never cache API calls
    }
    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request))
    );
});
