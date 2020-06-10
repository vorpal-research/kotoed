self.addEventListener('push', function(e) {
    var body;

    if (e.data) {
        body = e.data.json();
    } else {
        body = { id: -1, contents: '' };
    }

    console.log("Pushed: " + JSON.stringify(body))

    var options = {
        body: body.contents,
        vibrate: [100, 50, 100],
        data: {
            dateOfArrival: Date.now(),
            primaryKey: 1,
            linked: body
        }
    };
    e.waitUntil(
        self.registration.showNotification('Kotoed', options)
    );
});

self.addEventListener('notificationclick', function (e) {
    e.notification.close()
    const notificationData = e.notification.data
    const body = notificationData.linked

    const kotoed = self.location.origin
    const page = new URL(`/notification/${body.id}`, kotoed)

    e.waitUntil(
        clients.openWindow(page)
    )
})