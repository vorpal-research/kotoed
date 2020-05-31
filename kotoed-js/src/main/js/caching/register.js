/**
 * Register service worker and check if debug is enabled.
 * We can't check for item on localStorage in service worker, because
 * it doesn't have access to DOM.
 */
const worker = 'serviceWorker';
const sw = navigator.serviceWorker;


if (worker in navigator) {
    sw.register('/sw.js', { // access service worker from root. Important
        scope: '/'
    }).then(reg => {
        if (!reg.active) {
            return;
        }

        let debugEnabled = localStorage.getItem('debug');
        debugEnabled = debugEnabled !== null;
        console.log('debug mode:', debugEnabled);

        toWorker({
            debug: debugEnabled
        });
    }).catch((error) => {
        console.log(`Registration failed with ${error}.`);
    });
}

/**
 * post message to service worker handler
 */
function toWorker(message) {
    const {controller} = sw;
    if (controller) {
        controller.postMessage(message);
    }
}