;(function (config) {
    if (!config.devServer) return;

    const endpoint = "/__rachel_dev_proxy";
    const { Readable } = require("node:stream");
    const blockedRequestHeaders = new Set([
        "accept-encoding", "connection", "cookie", "expect", "forwarded", "host",
        "keep-alive", "origin", "referer", "te", "trailer", "transfer-encoding", "upgrade",
    ]);
    const responseHeaders = new Set([
        "accept-ranges", "content-disposition", "content-language", "content-range",
        "content-type", "etag", "last-modified", "retry-after",
    ]);

    async function proxy(request, response, next) {
        const incoming = new URL(request.url, "http://localhost");
        if (incoming.pathname !== endpoint) return next();
        if (request.headers["sec-fetch-site"] !== "same-origin" ||
            request.headers["sec-fetch-dest"] !== "empty" ||
            (request.headers.origin && request.headers.origin !== "http://" + request.headers.host)) {
            response.writeHead(403).end();
            return;
        }

        let target;
        try {
            target = new URL(incoming.searchParams.get("url"));
            if (!["http:", "https:"].includes(target.protocol) || target.username || target.password) throw new Error();
        }
        catch {
            response.writeHead(400).end("Expected an HTTP(S) URL");
            return;
        }

        const abort = new AbortController();
        response.once("close", () => abort.abort());
        try {
            const headers = new Headers();
            for (const [name, value] of Object.entries(request.headers)) {
                if (!blockedRequestHeaders.has(name) && !/^(sec-|proxy-|x-forwarded-)/.test(name)) {
                    headers.set(name, value);
                }
            }
            const hasBody = request.method !== "GET" && request.method !== "HEAD";
            const upstream = await fetch(target, {
                method: request.method,
                headers,
                body: hasBody ? request : undefined,
                duplex: hasBody ? "half" : undefined,
                signal: abort.signal,
                redirect: hasBody ? "manual" : "follow",
            });
            for (const [name, value] of upstream.headers) {
                if (responseHeaders.has(name)) response.setHeader(name, value);
            }
            const encoding = upstream.headers.get("content-encoding");
            const length = upstream.headers.get("content-length");
            if (length && (!encoding || encoding === "identity")) response.setHeader("Content-Length", length);
            const redirect = upstream.headers.get("location");
            if (hasBody && redirect && [301, 302, 303, 307, 308].includes(upstream.status)) {
                const destination = new URL(redirect, target);
                if (destination.origin === target.origin) {
                    response.setHeader("Location", endpoint + "?url=" + encodeURIComponent(destination.href));
                }
            }
            response.setHeader("Cache-Control", "no-store");
            response.writeHead(upstream.status);
            if (upstream.body) Readable.fromWeb(upstream.body).on("error", error => response.destroy(error)).pipe(response);
            else response.end();
        }
        catch (error) {
            if (response.destroyed) return;
            if (response.headersSent) response.destroy(error);
            else response.writeHead(502).end("Unable to reach the requested HTTP(S) server");
        }
    }

    function installProxy(endpoint) {
        const originalFetch = window.fetch.bind(window);
        const originalOpen = XMLHttpRequest.prototype.open;

        function proxyUrl(input) {
            const url = new URL(input, document.baseURI);
            return url.origin !== location.origin && (url.protocol === "http:" || url.protocol === "https:")
                ? endpoint + "?url=" + encodeURIComponent(url.href)
                : null;
        }

        window.fetch = async (input, init) => {
            const url = proxyUrl(input instanceof Request ? input.url : input);
            if (!url) return originalFetch(input, init);
            const request = new Request(input, init);
            return originalFetch(url, {
                method: request.method,
                headers: request.headers,
                body: request.body ? await request.arrayBuffer() : undefined,
                credentials: "omit",
                redirect: request.redirect,
                signal: request.signal,
            });
        };

        XMLHttpRequest.prototype.open = function (method, url, ...options) {
            return originalOpen.call(this, method, proxyUrl(url) || url, ...options);
        };
    }

    config.devServer.host = "127.0.0.1";
    config.devServer.allowedHosts = ["localhost", "127.0.0.1"];
    const previousSetup = config.devServer.setupMiddlewares;
    config.devServer.setupMiddlewares = (middlewares, server) => {
        const configured = previousSetup ? previousSetup(middlewares, server) : middlewares;
        configured.unshift({ name: "rachel-web-dev-proxy", middleware: proxy });
        return configured;
    };
    const { BannerPlugin } = require("webpack");
    config.plugins.push(new BannerPlugin({
        banner: "(" + installProxy.toString() + ")(" + JSON.stringify(endpoint) + ");",
        raw: true,
        entryOnly: true,
    }));
})(config);
