/**
 * navbar.js  –  Global Shared Navbar Component
 * Safe Trip Planner
 */
(function () {
    const path = window.location.pathname;
    
    // Helper to check if current page is protected
    const isProtectedPage = path.includes('/plan-trip.html') || path.includes('/plan-trip');

    // ── 1. FETCH AUTH STATE ──────────────────────────────────────────
    fetch('/api/user', { cache: 'no-store' })
        .then(res => {
            const contentType = res.headers.get("content-type");
            if (res.ok && contentType && contentType.includes("application/json")) {
                return res.json();
            } else {
                throw new Error("Not authenticated");
            }
        })
        .then(user => {
            renderNavbar(user);
            setupStartPlanningButtons(true);
        })
        .catch(err => {
            // Not logged in
            if (isProtectedPage) {
                window.location.href = '/';
                return;
            }
            renderNavbar(null);
            setupStartPlanningButtons(false);
        });

    function setupStartPlanningButtons(isLoggedIn) {
        const buttons = document.querySelectorAll('a[href="/plan-trip.html"], a[href="/plan-trip"]');
        buttons.forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                if (isLoggedIn) {
                    window.location.href = '/plan-trip';
                } else {
                    window.location.href = '/oauth2/authorization/github';
                }
            });
        });
    }

    function renderNavbar(user) {
        // ── NAV LINKS ──────────
        const links = [
            { labelKey: "home",      label: "Home",      href: "/",    match: ["/index.html", "/"] },
            { labelKey: "about",     label: "About",     href: "/about.html",    match: ["/about.html"] },
            { labelKey: "plan_trip", label: "Plan Trip", href: "/plan-trip", match: ["/plan-trip.html", "/plan-trip"] },
            { labelKey: "my_trip",   label: "My Trip",   href: "/my-trip.html",   match: ["/my-trip.html"] },
        ];

        const linksHTML = links.map(function (link) {
            const isActive = link.match.some(function (m) { return path === m || path.endsWith(m); });
            return `<a href="${link.href}" data-i18n="${link.labelKey}"${isActive ? ' class="stp-active"' : ''}>${link.label}</a>`;
        }).join("");

        // ── AUTH STATE ────────────────────────────────────────────────
        let authHTML;
        if (user) {
            const avatarUrl = user.avatarUrl || '';
            const name = user.name || user.username || '';
            authHTML = `
                <div class="stp-user-chip">
                    <img src="${avatarUrl}" alt="${name}" class="stp-user-avatar" style="border-radius: 50%; width: 24px; height: 24px; object-fit: cover;">
                    <span>${name}</span>
                </div>
                <button class="stp-btn-logout" id="stp-logout-btn" data-i18n="logout">Logout</button>
            `;
        } else {
            authHTML = '<a href="/oauth2/authorization/github" class="stp-login-link" data-i18n="login">Login with GitHub</a>';
        }

        // ── LANGUAGE SWITCHER ────────────────────────────────────────
        const currentLang = localStorage.getItem("appLanguage") || "en";
        const langSwitcherHTML = `
            <div class="stp-lang-switcher">
                <select id="lang-switcher" class="stp-lang-select" title="Select Language">
                    <option value="en"${currentLang === 'en' ? ' selected' : ''}>English</option>
                    <option value="hi"${currentLang === 'hi' ? ' selected' : ''}>हिंदी</option>
                    <option value="zh"${currentLang === 'zh' ? ' selected' : ''}>中文</option>
                </select>
            </div>
        `;

        // ── INJECT NAVBAR HTML ────────────────────────────────────────
        const navbarHTML = `
            <header class="stp-header">
                <div class="stp-header-inner">
                    <a href="/" class="stp-logo">
                        <div class="stp-logo-mark">ST</div>
                        <div class="stp-logo-text">
                            <span class="stp-logo-main">Safe Trip Planner</span>
                            <span class="stp-logo-sub" data-i18n="logo_sub">Plan smart · travel safe</span>
                        </div>
                    </a>
                    <nav class="stp-nav" aria-label="Main navigation">
                        ${linksHTML}
                    </nav>
                    <div class="stp-nav-actions">
                        ${langSwitcherHTML}
                        <div class="stp-nav-auth">
                            ${authHTML}
                        </div>
                    </div>
                </div>
            </header>
        `;

        // Insert as the very first element in <body>
        document.body.insertAdjacentHTML("afterbegin", navbarHTML);

        // ── LANGUAGE SWITCHER HANDLER ──────────────────────────────────
        const langSelect = document.getElementById("lang-switcher");
        if (langSelect) {
            langSelect.addEventListener("change", function () {
                localStorage.setItem("appLanguage", this.value);
                window.location.reload();
            });
        }

        // ── LOGOUT HANDLER ────────────────────────────────────────────
        const logoutBtn = document.getElementById("stp-logout-btn");
        if (logoutBtn) {
            logoutBtn.addEventListener("click", function (e) {
                e.preventDefault();
                // Direct browser navigation to /logout (we will enable GET /logout in SecurityConfig)
                window.location.href = '/logout';
            });
        }
    }
})();
