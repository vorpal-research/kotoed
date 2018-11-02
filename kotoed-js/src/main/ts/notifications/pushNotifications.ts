import {sendAsync} from "../views/components/common";
import {Generated} from "../util/kotoed-generated";
import Address = Generated.Address;

function urlBase64ToUint8Array(base64String: String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
        .replace(/\-/g, '+')
        .replace(/_/g, '/')
    ;
    const rawData = window.atob(base64);
    return Uint8Array.from([...rawData].map((char) => char.charCodeAt(0)));
}

export async function subscribeToPushNotifications() {
    if(!("Notification" in window)) return;

    let Notification_ = (window as any)['Notification'] as Notification;

    if(Notification_.permission !== 'default') return;

    const serverKey = (await sendAsync<{}, any>(Address.Api.Notification.Web.PublicKey, {})).key;

    if(!serverKey) {
        console.log("Notification keys not set, web push is not available =(")
        return;
    }

    await Notification.requestPermission(async permission => {
        if(permission === 'granted') {
            const reg = await navigator.serviceWorker.register('/static/static/serviceWorker.js');

            const sw = reg.installing || reg.waiting || reg.active;
            if(!sw) return;

            async function pushSub() {
                const sub = await reg.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(serverKey)
                });

                const p256dh_ = sub.getKey('p256dh');
                const auth_ = sub.getKey('auth');

                const key = p256dh_ && btoa(String.fromCharCode.apply(null, new Uint8Array(p256dh_))) || '';
                const auth = auth_ && btoa(String.fromCharCode.apply(null, new Uint8Array(auth_))) || '';

                await sendAsync(Address.Api.Notification.Web.Subscribe, {
                    endpoint: sub.endpoint,
                    key: key,
                    auth: auth
                });
                console.log(sub.toJSON())
            }

            if(sw.state == "activated") {
                await pushSub();
            } else {
                sw.addEventListener("statechange", async e => {
                    const sw_ = e.target as ServiceWorker;

                    if(sw_.state == "activated") {
                        await pushSub();
                    }
                })
            }
        }
    })
}
