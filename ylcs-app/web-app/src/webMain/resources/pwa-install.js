(() => {
    let pendingPrompt;
    let installed = false;

    const notify = () => window.dispatchEvent(new Event("rachel-pwa-install-change"));
    const canInstall = () => !installed && (pendingPrompt != null || typeof navigator.install === "function");
    const reportFailure = error => {
        if (error.name !== "AbortError") console.warn("Unable to open the web app install prompt", error);
    };

    window.addEventListener("beforeinstallprompt", event => {
        event.preventDefault();
        pendingPrompt = event;
        notify();
    });

    window.addEventListener("appinstalled", () => {
        installed = true;
        pendingPrompt = undefined;
        notify();
    });

    window.rachelPwaInstall = {
        shouldSuggest: () => !installed && !window.matchMedia("(display-mode: standalone)").matches && !navigator.standalone,
        canInstall,
        prompt() {
            if (!canInstall()) return;
            const event = pendingPrompt;
            pendingPrompt = undefined;
            try {
                Promise.resolve(event ? event.prompt() : navigator.install()).catch(reportFailure);
            }
            catch (error) {
                reportFailure(error);
            }
            finally {
                notify();
            }
        },
    };
})();
