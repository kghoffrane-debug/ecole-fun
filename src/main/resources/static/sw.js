// Service worker minimal : rend l'application installable (PWA).
// Aucune donnée scolaire n'est mise en cache hors ligne (données d'enfants).
self.addEventListener('install', function () { self.skipWaiting(); });
self.addEventListener('activate', function (e) { e.waitUntil(self.clients.claim()); });
self.addEventListener('fetch', function () { /* réseau uniquement */ });
