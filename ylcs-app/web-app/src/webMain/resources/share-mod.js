(() => {
    if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("/service-worker.js").catch(error => {
            console.warn("Unable to register the MOD share target", error);
        });
    }

    window.rachelReceiveSharedMod = async onFile => {
        const page = new URL(location.href);
        const token = page.searchParams.get("sharedMod");
        if (!token) return;

        page.searchParams.delete("sharedMod");
        history.replaceState(history.state, "", page);

        try {
            const response = await fetch("/share-mod?token=" + encodeURIComponent(token));
            const filename = response.headers.get("X-Rachel-Filename");
            if (!response.ok || !filename) throw new Error("The shared MOD is no longer available");
            const blob = await response.blob();
            onFile(new File([blob], decodeURIComponent(filename), { type: blob.type }));
        }
        catch (error) {
            console.warn("Unable to open the shared MOD", error);
        }
    };
})();
