const MOD_SHARE_PATH = "/share-mod";
const MOD_SHARE_LIFETIME = 30 * 60 * 1000;

self.addEventListener("install", event => event.waitUntil(self.skipWaiting()));
self.addEventListener("activate", event => event.waitUntil(self.clients.claim()));

self.addEventListener("fetch", event => {
    const url = new URL(event.request.url);
    if (url.origin !== self.location.origin || url.pathname !== MOD_SHARE_PATH) return;
    if (event.request.method === "POST") {
        event.respondWith(acceptSharedMod(event.request, url));
    }
    else if (event.request.method === "GET") {
        event.respondWith(deliverSharedMod(url.searchParams.get("token")));
    }
});

function openShares() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open("rachel-shared-mods", 1);
        request.onupgradeneeded = () => request.result.createObjectStore("mods").createIndex("expires", "expires");
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}

async function updateShares(change) {
    const database = await openShares();
    try {
        return await new Promise((resolve, reject) => {
            const transaction = database.transaction("mods", "readwrite");
            const store = transaction.objectStore("mods");
            let result;
            transaction.oncomplete = () => resolve(result);
            transaction.onabort = () => reject(transaction.error);

            const expired = store.index("expires").openCursor(IDBKeyRange.upperBound(Date.now()));
            expired.onsuccess = () => {
                const cursor = expired.result;
                if (cursor) {
                    cursor.delete();
                    cursor.continue();
                }
            };
            change(store, value => { result = value; });
        });
    }
    finally {
        database.close();
    }
}

async function acceptSharedMod(request, url) {
    try {
        const form = await request.formData();
        const file = form.getAll("mods").find(part => part instanceof File && /\.rachel$/i.test(part.name));
        if (!file) return new Response("分享内容中没有 MOD 文件。", { status: 400 });
        const token = crypto.randomUUID();
        await updateShares(store => store.add({ file, expires: Date.now() + MOD_SHARE_LIFETIME }, token));
        const destination = new URL("/", url);
        destination.searchParams.set("sharedMod", token);
        return Response.redirect(destination, 303);
    }
    catch (error) {
        console.warn("Unable to save the shared MOD", error);
        return new Response("无法接收分享的 MOD 文件", { status: 500 });
    }
}

async function deliverSharedMod(token) {
    if (!token) return new Response(null, { status: 404 });
    try {
        const file = await updateShares((store, setResult) => {
            const request = store.get(token);
            request.onsuccess = () => {
                const entry = request.result;
                if (entry) {
                    store.delete(token);
                    if (entry.expires > Date.now()) setResult(entry.file);
                }
            };
        });
        if (!file) return new Response(null, { status: 404 });
        return new Response(file, {
            headers: {
                "Content-Type": "application/octet-stream",
                "X-Rachel-Filename": encodeURIComponent(file.name),
                "Cache-Control": "no-store",
            },
        });
    }
    catch (error) {
        console.warn("Unable to read the shared MOD", error);
        return new Response(null, { status: 500 });
    }
}
