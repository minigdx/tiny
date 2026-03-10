(function () {
    if (document.querySelector('.tiny-nav')) return;

    // Inject CSS
    var style = document.createElement('style');
    style.textContent = [
        '.tiny-nav {',
        '    position: fixed;',
        '    top: 0; left: 0; right: 0;',
        '    z-index: 10000;',
        '    height: 56px;',
        '    background: rgba(255, 255, 255, 0.8);',
        '    backdrop-filter: blur(12px);',
        '    -webkit-backdrop-filter: blur(12px);',
        '    border-bottom: 4px solid rgba(0, 229, 255, 0.2);',
        '    display: flex;',
        '    align-items: center;',
        '    padding: 0 24px;',
        '    font-family: "Quicksand", sans-serif;',
        '}',
        '.tiny-nav__logo {',
        '    color: #1e293b;',
        '    font-family: "Lilita One", cursive;',
        '    font-weight: 400;',
        '    font-size: 1.3em;',
        '    margin-right: auto;',
        '    white-space: nowrap;',
        '    letter-spacing: 2px;',
        '    text-decoration: none;',
        '    transition: color 0.15s ease;',
        '}',
        '.tiny-nav__logo:hover { color: #ff0080; }',
        '.tiny-nav__links {',
        '    display: flex;',
        '    align-items: center;',
        '    gap: 8px;',
        '}',
        '.tiny-nav__link {',
        '    color: #444;',
        '    text-decoration: none;',
        '    margin: 0 12px;',
        '    font-size: 0.85rem;',
        '    font-weight: 700;',
        '    text-transform: uppercase;',
        '    letter-spacing: 1px;',
        '    transition: color 0.15s ease;',
        '}',
        '.tiny-nav__link:hover { color: #ff0080; }',
        '.tiny-nav__cta {',
        '    background: #00e5ff;',
        '    color: #fff;',
        '    padding: 8px 20px;',
        '    border-radius: 16px;',
        '    border: 2px solid #1e293b;',
        '    font-family: "Lilita One", cursive;',
        '    font-weight: 400;',
        '    font-size: 0.85rem;',
        '    text-transform: uppercase;',
        '    letter-spacing: 1px;',
        '    text-decoration: none;',
        '    box-shadow: 0 6px 0 #00b8cc;',
        '    transition: transform 0.15s ease;',
        '}',
        '.tiny-nav__cta:hover {',
        '    background: #00d4ec;',
        '    color: #fff;',
        '    transform: translateY(-2px);',
        '}',
        '@media (max-width: 960px) {',
        '    .tiny-nav__links { display: none; }',
        '}'
    ].join('\n');
    document.head.appendChild(style);

    // Inject HTML
    var nav = document.createElement('nav');
    nav.className = 'tiny-nav';
    nav.innerHTML = [
        '<a href="index.html" class="tiny-nav__logo">\uD83E\uDDF8 Tiny</a>',
        '<div class="tiny-nav__links">',
        '    <a href="index.html#features" class="tiny-nav__link">Features</a>',
        '    <a href="documentation.html#_tiny_showcase" class="tiny-nav__link">Showcase</a>',
        '    <a href="tiny-api.html" class="tiny-nav__link">Docs</a>',
        '    <a href="https://github.com/minigdx/tiny" class="tiny-nav__link">Community</a>',
        '    <a href="documentation.html#_tiny_install" class="tiny-nav__cta">Get Started</a>',
        '</div>'
    ].join('\n');
    document.body.insertBefore(nav, document.body.firstChild);
})();
