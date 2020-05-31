const INSTALL = 'install';
const ACTIVATE = 'activate';
const FETCH = 'fetch';
const CUSTOM_MESSAGE = 'message';
const VERSION = new Date().getTime();

/**
 * global variable shared across function to log or not
 * @type {boolean}
 */
let debug = false;

/**
 * Main config for caching service worker.
 * Defines main rules for `fetch` event and base resources, that
 * are presented on each page
 */
const config = {
    resources: [
        '/static/js/vendor.bundle.js',
        '/static/css/vendor.css',
        '/static/static/register.js',
        '/static/sw.js',
    ],
    eventRegex: ['eventbus', 'auth'],
    cacheName: 'static-' + VERSION,
    debugAllowed: 'true'
};

let cachingStrategies = {

    cacheFirst: (cfg, request) =>
        caches.match(request)
            .then(response => {
                if (response) {
                    return response;
                }
                return fetch(request).then(response => {
                        if (!response || response.status !== 200) {
                            return response;
                        }
                        toCache(request, response, cfg.cacheName);
                        return response;
                    }
                );
            })
            .catch(() => {
                /**
                 * Maybe we should use yet another offline page like snafu dialog
                 */
                log('We are currently offline. Wait for websocket max reconnect retries')
            }),

    networkFirst: (cfg, request) =>
        fetch(request)
            .then(
                (response) => {
                    toCache(request, response, cfg.cacheName);
                    return response;
                },
                (err) =>
                    caches.open(cfg.cacheName)
                        .then((cache) => cache.match(request))
            ),

    defaultStrategy: (cfg, request) => cachingStrategies.cacheFirst(cfg, request)
};

function log(message) {
    if (debug === true) {
        console.log(message);
    }
}

/**
 * Add following @param request to cache with @param cacheName
 */
function toCache(request, response, cacheName) {
    if (response.ok) {
        let copy = response.clone(); // response is stream, we have to clone it
        caches.open(cacheName).then(cache => {
            cache.put(request, copy);
        });
        return response;
    }
}

/**
 * check if following event should be pushed forwards
 */
function validate(event) {
    let request = event.request;
    return request.method === 'GET' &&
        !config.eventRegex.filter(pattern => request.url.includes(pattern)).length;
}


self.addEventListener(CUSTOM_MESSAGE, event => {
    const {data = {}} = event;
    switch (data.debug) {
        case true:
            debug = true;
            break;
        case false:
            debug = false;
            break;
    }
});

self.addEventListener(INSTALL, event => {
    function onInstall(cfg) {
        return self.caches.open(cfg.cacheName)
            // cache the resources that are present on each page
            .then(cache => cache.addAll(cfg.resources));
    }

    event.waitUntil(onInstall(config));
});

/**
 * At this phase, we have to clear all data from the old cache, except main resources.
 */
self.addEventListener(ACTIVATE, (event) => {
    const clear = function (config) {
        self.caches.open(config.cacheName)
            .then(cache => cache.keys()
                .then(keys => Promise.all(keys.map(key => {
                    let shouldDelete = !config.resources.includes(key.url.pathname);
                    return shouldDelete ? cache.delete(event.request)
                        : Promise.resolve();
                }))))
            .then(() => log('Old cache has been cleared'));
    };

    event.waitUntil(
        clear(config)
    );
});

self.addEventListener(FETCH, function (event) {
    let cachingPipeline = (cfg, request) =>
        event.respondWith(
            cachingStrategies.networkFirst(cfg, request)
        );

    if (validate(event)) {
        let request = event.request;
        log(request);
        cachingPipeline(config, request)
    }
});