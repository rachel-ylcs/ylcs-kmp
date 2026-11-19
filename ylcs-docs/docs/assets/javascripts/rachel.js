(() => {
    "use strict";

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const scheme = () => document.body.dataset.mdColorScheme || "rachel-dark";

    function ready(fn) {
        if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", fn);
        else fn();
    }

    /* ------------------------------------------------------------------ */
    /* reading progress                                                    */
    /* ------------------------------------------------------------------ */

    function mountProgress() {
        const root = document.createElement("div");
        root.className = "rc-progress";
        root.setAttribute("aria-hidden", "true");
        const bar = document.createElement("span");
        bar.className = "rc-progress__bar";
        root.appendChild(bar);
        document.body.appendChild(root);

        let queued = false;
        const update = () => {
            queued = false;
            const doc = document.documentElement;
            const max = doc.scrollHeight - window.innerHeight;
            const ratio = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
            bar.style.transform = `scaleX(${ratio.toFixed(4)})`;
        };
        window.addEventListener("scroll", () => {
            if (!queued) {
                queued = true;
                requestAnimationFrame(update);
            }
        }, { passive: true });
        update();
    }

    /* ------------------------------------------------------------------ */
    /* reveal on scroll                                                    */
    /* ------------------------------------------------------------------ */

    function reveal() {
        const targets = document.querySelectorAll(
            ".rc-feature, .rc-link-card, .rc-path, .rc-eco, .rc-shot, .rc-metric, .rc-board, .rc-panel, .rc-section__head"
        );
        if (!targets.length) return;

        targets.forEach((el, index) => {
            el.classList.add("rc-reveal");
            el.style.setProperty("--delay", `${Math.min(index % 6, 5) * 55}ms`);
        });

        if (reduceMotion || !("IntersectionObserver" in window)) {
            targets.forEach((el) => el.classList.add("is-visible"));
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });

        targets.forEach((el) => observer.observe(el));
    }

    /* ------------------------------------------------------------------ */
    /* pointer spotlight                                                   */
    /* ------------------------------------------------------------------ */

    function spotlight() {
        if (reduceMotion) return;
        const selector = ".rc-feature, .rc-link-card, .rc-eco, .rc-shot, .rc-path";
        document.querySelectorAll(selector).forEach((el) => {
            el.addEventListener("pointermove", (event) => {
                const rect = el.getBoundingClientRect();
                el.style.setProperty("--mx", `${((event.clientX - rect.left) / rect.width) * 100}%`);
                el.style.setProperty("--my", `${((event.clientY - rect.top) / rect.height) * 100}%`);
            }, { passive: true });
        });
    }

    /* ------------------------------------------------------------------ */
    /* back to top                                                         */
    /* ------------------------------------------------------------------ */

    function mountBackToTop() {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "rc-top";
        button.setAttribute("aria-label", "回到顶部");
        button.innerHTML = '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7"/></svg>';
        button.addEventListener("click", () => {
            window.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
        });
        document.body.appendChild(button);

        const toggle = () => button.classList.toggle("is-visible", window.scrollY > 620);
        window.addEventListener("scroll", toggle, { passive: true });
        toggle();
    }

    /* ------------------------------------------------------------------ */
    /* keyboard shortcuts                                                  */
    /* ------------------------------------------------------------------ */

    function shortcuts() {
        document.addEventListener("keydown", (event) => {
            const target = event.target;
            const typing = target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable);
            if ((event.key === "k" || event.key === "K") && (event.metaKey || event.ctrlKey)) {
                const label = document.querySelector('label[for="__search"]');
                if (label) {
                    event.preventDefault();
                    label.click();
                    window.setTimeout(() => {
                        const input = document.querySelector(".md-search__input");
                        if (input) input.focus();
                    }, 60);
                }
            } else if (event.key === "/" && !typing) {
                const label = document.querySelector('label[for="__search"]');
                if (label) {
                    event.preventDefault();
                    label.click();
                    window.setTimeout(() => {
                        const input = document.querySelector(".md-search__input");
                        if (input) input.focus();
                    }, 60);
                }
            }
        });
    }

    /* ------------------------------------------------------------------ */
    /* mermaid                                                             */
    /* ------------------------------------------------------------------ */

    let mermaidApi = null;
    let mermaidLoading = null;

    async function loadMermaid() {
        if (mermaidApi) return mermaidApi;
        if (!mermaidLoading) {
            mermaidLoading = import("https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs")
                .then((mod) => (mermaidApi = mod.default))
                .catch(() => null);
        }
        return mermaidLoading;
    }

    function mermaidVariables() {
        const dark = scheme() === "rachel-dark";
        return dark
            ? {
                background: "#0d101a",
                primaryColor: "#171b2b",
                primaryTextColor: "#e9ebf4",
                primaryBorderColor: "#8b7bff",
                lineColor: "#7d8aa6",
                secondaryColor: "#10222e",
                secondaryTextColor: "#d9f4ff",
                secondaryBorderColor: "#56d4ff",
                tertiaryColor: "#231b33",
                tertiaryTextColor: "#f0e7ff",
                tertiaryBorderColor: "#b49cff",
                noteBkgColor: "#1b1e2e",
                noteTextColor: "#e9ebf4",
                noteBorderColor: "#e9c88a",
                fontFamily: "'Noto Sans SC', 'Manrope', sans-serif",
                fontSize: "13px"
            }
            : {
                background: "#ffffff",
                primaryColor: "#f0edff",
                primaryTextColor: "#171923",
                primaryBorderColor: "#5b48d9",
                lineColor: "#8a8fa0",
                secondaryColor: "#e8f7fb",
                secondaryTextColor: "#0b3d4a",
                secondaryBorderColor: "#0e97b8",
                tertiaryColor: "#fbf3e3",
                tertiaryTextColor: "#493309",
                tertiaryBorderColor: "#b07a1e",
                noteBkgColor: "#fbfaf7",
                noteTextColor: "#171923",
                noteBorderColor: "#b07a1e",
                fontFamily: "'Noto Sans SC', 'Manrope', sans-serif",
                fontSize: "13px"
            };
    }

    let mermaidRendering = false;
    let mermaidQueued = false;

    async function renderMermaid() {
        if (mermaidRendering) {
            mermaidQueued = true;
            return;
        }

        const blocks = Array.from(document.querySelectorAll(".rc-mermaid"));
        if (!blocks.length) return;

        mermaidRendering = true;
        try {
            blocks.forEach((block) => {
                if (!block.dataset.rcSource) block.dataset.rcSource = block.textContent;
            });

            const api = await loadMermaid();
            if (!api) return;

            api.initialize({
                startOnLoad: false,
                securityLevel: "loose",
                theme: "base",
                themeVariables: mermaidVariables(),
                flowchart: { curve: "basis", htmlLabels: true, useMaxWidth: true },
                sequence: { useMaxWidth: true },
                er: { useMaxWidth: true }
            });

            for (const block of blocks) {
                block.removeAttribute("data-processed");
                block.textContent = block.dataset.rcSource;
                try {
                    await api.run({ nodes: [block] });
                    block.dataset.rcRendered = "true";
                } catch (error) {
                    block.dataset.rcRendered = "error";
                }
            }
        } finally {
            mermaidRendering = false;
            if (mermaidQueued) {
                mermaidQueued = false;
                window.setTimeout(renderMermaid, 80);
            }
        }
    }

    function watchScheme() {
        let lastScheme = scheme();
        const observer = new MutationObserver(() => {
            const next = scheme();
            if (next === lastScheme) return;
            lastScheme = next;
            if (document.querySelector(".rc-mermaid[data-rc-rendered]")) {
                window.clearTimeout(watchScheme.timer);
                watchScheme.timer = window.setTimeout(renderMermaid, 160);
            }
        });
        observer.observe(document.body, { attributes: true, attributeFilter: ["data-md-color-scheme"] });
    }

    /* ------------------------------------------------------------------ */
    /* GitHub repository widget                                            */
    /* ------------------------------------------------------------------ */

    function formatCount(value) {
        if (value < 1000) return String(value);
        const compact = value / 1000;
        return `${compact.toFixed(compact >= 10 ? 0 : 1).replace(/\.0$/, "")}k`;
    }

    function mountGithub() {
        const host = document.querySelector(".md-header__source");
        if (!host) return;

        host.innerHTML = `
            <a class="rc-gh" href="https://github.com/rachel-ylcs/ylcs-kmp" target="_blank" rel="noopener" title="GitHub · rachel-ylcs/ylcs-kmp" aria-label="GitHub 仓库 rachel-ylcs/ylcs-kmp">
                <span class="rc-gh__mark" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 .5A11.5 11.5 0 0 0 .5 12c0 5.08 3.29 9.39 7.86 10.91.58.11.79-.25.79-.56v-2.17c-3.2.7-3.88-1.54-3.88-1.54-.52-1.33-1.28-1.68-1.28-1.68-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.74-1.55-2.55-.29-5.24-1.28-5.24-5.7 0-1.26.45-2.29 1.19-3.1-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.18 1.18a11 11 0 0 1 5.78 0c2.21-1.49 3.18-1.18 3.18-1.18.63 1.59.23 2.76.11 3.05.74.81 1.19 1.84 1.19 3.1 0 4.43-2.7 5.4-5.27 5.69.41.36.78 1.06.78 2.14v3.17c0 .31.21.68.8.56A11.5 11.5 0 0 0 23.5 12 11.5 11.5 0 0 0 12 .5Z"/></svg>
                </span>
                <span class="rc-gh__repo">ylcs-kmp</span>
                <span class="rc-gh__stat" data-gh="stars" data-loaded="true" title="Stars">
                    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="m12 2.6 2.9 5.9 6.5.9-4.7 4.6 1.1 6.5-5.8-3-5.8 3 1.1-6.5L2.6 9.4l6.5-.9L12 2.6Z"/></svg>
                    <span class="rc-gh__count">31</span>
                </span>
                <span class="rc-gh__stat" data-gh="forks" data-loaded="true" title="Forks">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="6" cy="5" r="2.2"/><circle cx="18" cy="5" r="2.2"/><circle cx="12" cy="19" r="2.2"/><path d="M6 7.2v1.5c0 2 1.6 3.6 3.6 3.6h4.8c2 0 3.6-1.6 3.6-3.6V7.2"/><path d="M12 12.3V16.8"/></svg>
                    <span class="rc-gh__count">6</span>
                </span>
            </a>`;

        const stars = host.querySelector('[data-gh="stars"]');
        const forks = host.querySelector('[data-gh="forks"]');
        const starCount = stars.querySelector(".rc-gh__count");
        const forkCount = forks.querySelector(".rc-gh__count");

        fetch("https://api.github.com/repos/rachel-ylcs/ylcs-kmp", {
            headers: { Accept: "application/vnd.github+json" }
        })
            .then((response) => (response.ok ? response.json() : Promise.reject(new Error("GitHub API"))))
            .then((data) => {
                if (typeof data.stargazers_count === "number") {
                    starCount.textContent = formatCount(data.stargazers_count);
                    stars.dataset.loaded = "true";
                }
                if (typeof data.forks_count === "number") {
                    forkCount.textContent = formatCount(data.forks_count);
                    forks.dataset.loaded = "true";
                }
            })
            .catch(() => {
                // 未认证 API 可能被限流；保留初始快照，避免计数区域消失。
            });
    }

    /* ------------------------------------------------------------------ */
    /* home page flag                                                      */
    /* ------------------------------------------------------------------ */

    function markHome() {
        if (document.querySelector(".rc-hero")) document.body.classList.add("rc-home");
    }

    ready(() => {
        markHome();
        mountGithub();
        mountProgress();
        mountBackToTop();
        reveal();
        spotlight();
        shortcuts();
        renderMermaid();
        watchScheme();
    });
})();
