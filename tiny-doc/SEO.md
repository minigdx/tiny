# SEO Analysis for Tiny Documentation Website

> Analysis of https://minigdx.github.io/tiny/ — the Asciidoctor-generated documentation site for the Tiny game engine.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Audit](#current-state-audit)
3. [Critical Issues](#critical-issues)
4. [Meta Tags & Head Elements](#meta-tags--head-elements)
5. [Content & Keyword Strategy](#content--keyword-strategy)
6. [Site Architecture & URL Structure](#site-architecture--url-structure)
7. [Social Sharing & Open Graph](#social-sharing--open-graph)
8. [Structured Data (JSON-LD)](#structured-data-json-ld)
9. [Image Optimization](#image-optimization)
10. [Performance & Core Web Vitals](#performance--core-web-vitals)
11. [Crawlability & Indexing](#crawlability--indexing)
12. [Content Gaps & Opportunities](#content-gaps--opportunities)
13. [Competitive Positioning](#competitive-positioning)
14. [Implementation Roadmap](#implementation-roadmap)

---

## Executive Summary

The Tiny documentation website is a static Asciidoctor-generated site hosted on GitHub Pages. While the content is solid and the interactive examples are a strong differentiator, the site has **almost no SEO infrastructure in place**. There are no meta descriptions, no Open Graph tags, no sitemap, no robots.txt, no structured data, and no canonical URLs. These are foundational SEO elements that, once implemented, should significantly improve discoverability for queries related to Lua game engines, fantasy consoles, and lightweight game development tools.

### Severity Overview

| Category | Severity | Impact |
|----------|----------|--------|
| Missing meta description | Critical | Search engines show auto-generated snippets |
| Missing Open Graph / Twitter Card tags | High | Poor social media preview when shared |
| No sitemap.xml | High | Search engines may not discover all pages |
| No robots.txt | Medium | No crawl guidance for search engines |
| No structured data | Medium | Missing rich snippet opportunities |
| No canonical URLs | Medium | Potential duplicate content issues |
| Missing image alt text | Medium | Lost image search opportunities |
| Single-page architecture | High | All content competes on one URL |
| Keyword targeting gaps | High | Missing ranking opportunities |
| No `.nojekyll` file | Low | GitHub Pages may skip files starting with `_` |
| Large GIF assets (~13MB total) | Medium | Slow page load times |

---

## Current State Audit

### What Exists

| Element | Status | Details |
|---------|--------|---------|
| Favicon | Exists | `./sample/favicon.png` (118x106 PNG) |
| Document title | Exists | `Tiny` (emoji stripped by Asciidoctor or rendered as `Tiny 🧸`) |
| Custom stylesheet | Exists | `adoc-riak.css` (Riak-derived theme) |
| Custom docinfo header | Exists | Google Fonts import + custom CSS for editor |
| Custom docinfo footer | Exists | Loads `tiny-web-editor.js` |
| Syntax highlighting | Exists | Rouge highlighter |
| TOC navigation | Exists | Left-side, 5 levels deep |
| Interactive examples | Exists | `<tiny-editor>` custom elements |

### What Is Missing

| Element | Status |
|---------|--------|
| `<meta name="description">` | Missing |
| `<meta name="keywords">` | Missing |
| Open Graph meta tags (`og:*`) | Missing |
| Twitter Card meta tags (`twitter:*`) | Missing |
| `<link rel="canonical">` | Missing |
| `robots.txt` | Missing |
| `sitemap.xml` | Missing |
| `.nojekyll` file | Missing |
| Structured data (JSON-LD) | Missing |
| Image `alt` attributes | Mostly missing |
| `<meta name="robots">` | Missing |
| Google Search Console verification | Unknown |

---

## Critical Issues

### 1. No Meta Description

**File:** `tiny-doc/src/docs/asciidoc/index.adoc`

The `:description:` Asciidoctor attribute is not set. This means search engines auto-generate a snippet from the page content, which often results in poor, irrelevant previews in search results.

**Fix:** Add the `:description:` attribute to `index.adoc`:

```asciidoc
:description: Tiny is a lightweight, open-source game engine and virtual console for building retro-style games with Lua scripting. Features hot reload, 256-color palette, and web export.
```

Similarly, add `:description:` to `playground.adoc`:

```asciidoc
:description: Try the Tiny game engine directly in your browser. Interactive playground with code examples for Lua game development - no installation required.
```

### 2. No Keywords

**Fix:** Add `:keywords:` to document headers:

```asciidoc
:keywords: game engine, lua game engine, fantasy console, virtual console, retro game, pixel art, indie game development, kotlin multiplatform, web game, game jam
```

### 3. Single-Page Architecture Problem

Currently, almost all documentation lives in a single `index.html` page (via Asciidoctor `include::` directives). This means:

- All content targets a single URL, diluting keyword focus
- Search engines see one extremely long page instead of topically-focused pages
- Individual sections (tutorial, API, showcase) cannot rank independently
- Users cannot share links to specific documentation sections with clean URLs

**Recommendation:** Consider splitting into separate pages:

| Page | Target Keywords |
|------|----------------|
| `index.html` | tiny game engine, lua virtual console |
| `install.html` | install tiny game engine, tiny cli setup |
| `tutorial.html` | lua game tutorial, pong game lua, tiny game tutorial |
| `api.html` | tiny api reference, lua game api |
| `showcase.html` | tiny game showcase, games made with tiny |
| `playground.html` (already separate) | lua game playground, try lua game engine online |
| `sfx-editor.html` | retro sound effects editor, chiptune sfx tool |
| `cli.html` | tiny cli commands, game engine cli |

This is achievable by removing `include::` directives from `index.adoc` and making each `.adoc` file a standalone document with its own header attributes. Each file would need its own `:docinfo:`, `:favicon:`, `:stylesheet:`, etc.

---

## Meta Tags & Head Elements

### Required Additions to `docinfo-header.html`

Add the following meta tags to `tiny-doc/src/docs/asciidoc/docinfo-header.html`:

```html
<!-- SEO Meta Tags -->
<meta name="author" content="Tiny Game Engine Contributors">
<meta name="robots" content="index, follow">
<meta name="theme-color" content="#007bff">

<!-- Open Graph -->
<meta property="og:type" content="website">
<meta property="og:title" content="Tiny - Lightweight Lua Game Engine & Virtual Console">
<meta property="og:description" content="Build retro-style games with Lua scripting. Features hot reload, 256-color palette, desktop and web export. Open source and free.">
<meta property="og:url" content="https://minigdx.github.io/tiny/">
<meta property="og:image" content="https://minigdx.github.io/tiny/sample/favicon.png">
<meta property="og:site_name" content="Tiny Game Engine">
<meta property="og:locale" content="en_US">

<!-- Twitter Card -->
<meta name="twitter:card" content="summary_large_image">
<meta name="twitter:title" content="Tiny - Lightweight Lua Game Engine & Virtual Console">
<meta name="twitter:description" content="Build retro-style games with Lua scripting. Features hot reload, 256-color palette, desktop and web export.">
<meta name="twitter:image" content="https://minigdx.github.io/tiny/sample/favicon.png">

<!-- Canonical URL -->
<link rel="canonical" href="https://minigdx.github.io/tiny/">
```

### Asciidoctor Document Attributes to Add

In `index.adoc` header:

```asciidoc
:description: Tiny is a lightweight Lua game engine and virtual console for building retro-style games. Open source, cross-platform, with hot reload and web export.
:keywords: game engine, lua, virtual console, fantasy console, retro games, pixel art, indie game development, kotlin multiplatform, game jam, hot reload
:author: Tiny Game Engine Contributors
```

### OG Image Recommendation

The current favicon (118x106px) is too small for Open Graph previews. Create a dedicated OG image:

- **Recommended size:** 1200x630 pixels
- **Content:** Tiny logo, tagline, and a screenshot of a game or the editor
- **File:** `sample/og-image.png`
- **Format:** PNG or JPG, under 1MB

---

## Content & Keyword Strategy

### Primary Target Keywords

Based on the engine's positioning as a Lua-based virtual console, these keyword clusters should be targeted:

| Keyword Cluster | Search Intent | Monthly Volume (est.) | Current Coverage |
|----------------|--------------|----------------------|-----------------|
| `lua game engine` | Informational | Medium | Weak |
| `fantasy console` | Informational | Medium | Missing |
| `virtual console game development` | Informational | Low | Weak |
| `lightweight game engine` | Informational | Medium | Weak |
| `retro game engine` | Informational | Medium | Missing |
| `lua game tutorial` | Tutorial | Medium | Moderate |
| `make games with lua` | Tutorial | Medium | Weak |
| `pong game tutorial lua` | Tutorial | Low | Strong |
| `browser game engine` | Informational | Medium | Missing |
| `game jam game engine` | Informational | Low | Missing |
| `pixel art game engine` | Informational | Medium | Missing |
| `kotlin multiplatform game engine` | Informational | Low | Missing |

### Content Recommendations

#### Homepage (`index.adoc`)

Current opening text is generic. Improve with keyword-rich, descriptive content:

**Current:**
> Welcome to the documentation for `Tiny`, a virtual console that makes building games and applications simple and fun!

**Suggested:**
> Tiny is a free, open-source **fantasy console** and **lightweight game engine** for creating retro-style games using **Lua scripting**. Build pixel art games with a 256-color palette, test instantly with hot reload, and export to **desktop and web** platforms. Ideal for **game jams**, rapid prototyping, and learning game development.

#### Tutorial Page

The Pong tutorial is good but could benefit from:

- A brief introduction explaining why Pong is a good first game
- Links to next steps (more complex games, API deep dives)
- A "What you will learn" summary at the top
- Adding structured data for a `HowTo` schema

#### Missing Content Pages

Consider creating content for high-value search queries:

1. **"Getting Started with Tiny"** - A dedicated onboarding page (not just install instructions)
2. **"Tiny vs PICO-8 vs TIC-80"** - Comparison page targeting people searching for fantasy consoles
3. **"Game Jam Guide with Tiny"** - Targeting the game jam community
4. **"Tiny FAQ / Troubleshooting"** - Targeting long-tail questions
5. **"Tiny Changelog / What's New"** - Fresh content signals for search engines

---

## Site Architecture & URL Structure

### Current Structure

```
https://minigdx.github.io/tiny/
├── index.html          (everything: install, tutorial, API, CLI, showcase, SFX, licenses)
├── playground.html     (interactive sandbox)
├── sample/             (GIFs, game examples, favicon)
└── resources/          (sprite images)
```

### Problems

1. **Single mega-page:** `index.html` contains all documentation in one page. Search engines prefer topically-focused pages.
2. **No breadcrumb navigation:** Users and search engines cannot understand page hierarchy.
3. **Anchor links are not indexable separately:** Sections like `#_tiny_tutorial` cannot rank independently in search results.
4. **GitHub Pages URL:** `minigdx.github.io/tiny/` is a subdirectory of `github.io`, which limits domain authority. A custom domain would help.

### Recommendations

1. **Split content into separate pages** (see Critical Issues section above).
2. **Consider a custom domain:** Even something like `tiny-engine.dev` or `tinygameengine.com` would significantly improve SEO authority vs. being a path on `github.io`.
3. **Add navigation links** between pages if split into multiple pages.
4. **Clean URL slugs:** Use descriptive filenames (`getting-started.html`, `lua-api-reference.html` rather than `tiny-install.html`).

---

## Social Sharing & Open Graph

### Current State

When someone shares the Tiny documentation URL on Twitter, Discord, Reddit, or any social platform, they see:

- **No preview image** (or a generic GitHub Pages icon)
- **No custom title** (just "Tiny" or the raw URL)
- **No description**

This is a major missed opportunity given that the indie game dev and game jam communities are highly active on social platforms.

### Required Open Graph Tags

See the [Meta Tags section](#meta-tags--head-elements) above for the full implementation.

### Additional Recommendations

1. **Create a compelling OG image** (1200x630px) showing:
   - The Tiny logo/name
   - A game screenshot or the editor in action
   - A tagline like "Build retro games with Lua"
2. **Test with validators:**
   - [Facebook Sharing Debugger](https://developers.facebook.com/tools/debug/)
   - [Twitter Card Validator](https://cards-dev.twitter.com/validator)
   - [LinkedIn Post Inspector](https://www.linkedin.com/post-inspector/)
3. **Per-page OG tags** if the site is split into multiple pages — each page should have its own `og:title`, `og:description`, and ideally `og:image`.

---

## Structured Data (JSON-LD)

Adding structured data helps search engines understand the content and can enable rich snippets in search results.

### Recommended Schema Types

#### 1. SoftwareApplication Schema (Homepage)

Add to `docinfo-header.html`:

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "Tiny Game Engine",
  "description": "A lightweight, open-source game engine and virtual console for building retro-style games with Lua scripting.",
  "url": "https://minigdx.github.io/tiny/",
  "applicationCategory": "DeveloperApplication",
  "operatingSystem": "Windows, macOS, Linux, Web",
  "programmingLanguage": "Lua",
  "license": "https://github.com/minigdx/tiny/blob/main/LICENSE",
  "author": {
    "@type": "Organization",
    "name": "minigdx",
    "url": "https://github.com/minigdx"
  },
  "offers": {
    "@type": "Offer",
    "price": "0",
    "priceCurrency": "USD"
  },
  "downloadUrl": "https://github.com/minigdx/tiny/releases",
  "softwareVersion": "latest",
  "screenshot": "https://minigdx.github.io/tiny/sample/camping.gif"
}
</script>
```

#### 2. HowTo Schema (Tutorial Page)

If the tutorial is split to its own page:

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "HowTo",
  "name": "How to Build a Pong Game with Tiny Game Engine",
  "description": "Step-by-step tutorial for creating a Pong game using Lua and the Tiny game engine",
  "step": [
    {
      "@type": "HowToStep",
      "name": "Initialize the Game State",
      "text": "Define the position and size of paddles, ball, and initial velocity using the _init() function."
    },
    {
      "@type": "HowToStep",
      "name": "Update the Game State",
      "text": "Move paddles and ball, check for collisions using the _update() function."
    },
    {
      "@type": "HowToStep",
      "name": "Draw the Game",
      "text": "Draw paddles and ball using shape.rectf() and shape.circlef() in the _draw() function."
    }
  ]
}
</script>
```

#### 3. BreadcrumbList Schema

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BreadcrumbList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "name": "Tiny Game Engine",
      "item": "https://minigdx.github.io/tiny/"
    }
  ]
}
</script>
```

---

## Image Optimization

### Current Problems

1. **Missing `alt` attributes:** Most images in the showcase section have no descriptive `alt` text. Asciidoctor image macros use the `alt` text from brackets.

   **Current:**
   ```asciidoc
   image:sample/camping.gif[link=https://dwursteisen.itch.io/trijam-camping]
   ```

   **Fixed:**
   ```asciidoc
   image:sample/camping.gif[Camping - a cozy survival game made with Tiny game engine, link=https://dwursteisen.itch.io/trijam-camping]
   ```

2. **GIF file sizes are very large (~13MB total):**
   - `gravity-balls.gif` — 2.3MB
   - `meiro_de_maigo2.gif` — 1.6MB
   - `reflections.gif` — 1.4MB
   - `connect_me.gif` — 1.3MB
   - `memory.gif` — 1.1MB

3. **No lazy loading:** All GIFs load immediately, blocking page rendering.

### Recommendations

1. **Add descriptive alt text** to all images — both for SEO and accessibility.
2. **Convert GIFs to WebM/MP4 video** for significant size reduction (typically 80-90% smaller). Use `<video autoplay loop muted playsinline>` via a passthrough block.
3. **Add `loading="lazy"`** to images below the fold. This requires a custom Asciidoctor extension or passthrough HTML.
4. **Create an OG image** (1200x630px) for social sharing previews.
5. **Optimize the favicon:** Current is 118x106px. Standard favicons should be:
   - 32x32 for browsers
   - 180x180 for Apple Touch Icon
   - 192x192 and 512x512 for PWA manifests

---

## Performance & Core Web Vitals

Google uses Core Web Vitals as ranking signals. The following issues may affect the Tiny website:

### Potential Issues

1. **Large page weight:** The single `index.html` page loads all content including:
   - The full Asciidoctor Riak CSS (~36KB)
   - Custom CSS in docinfo-header
   - Google Fonts (Titillium Web, Noticia Text, Source Code Pro) — 3 separate font requests
   - Font Awesome 3.2.0 CSS from CDN
   - `tiny-web-editor.js` module (potentially large, bundles Kotlin stdlib + Lua engine)
   - Multiple GIF animations (~13MB)
   - Two iframes (game example, SFX editor)

2. **Render-blocking resources:**
   - 3 Google Fonts CSS imports (2 in `adoc-riak.css` + 1 in `docinfo-header.html`)
   - Font Awesome CSS from CDN
   - Asciidoctor stylesheet

3. **No resource preloading** for critical fonts or scripts.

### Recommendations

1. **Preload critical fonts:** Add to `docinfo-header.html`:
   ```html
   <link rel="preconnect" href="https://fonts.googleapis.com">
   <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
   ```

2. **Consider self-hosting fonts** instead of loading from Google Fonts CDN to reduce DNS lookups.

3. **Add `loading="lazy"` to iframes:**
   The embedded game example and SFX editor iframes should be lazily loaded:
   ```html
   <iframe loading="lazy" title="Breakout example" ...></iframe>
   ```

4. **Defer non-critical JavaScript:**
   The `tiny-web-editor.js` script in `docinfo-footer.html` already uses `type="module"` which is deferred by default. This is correct.

5. **Consider splitting the page** (see Site Architecture section) to reduce individual page weight.

---

## Crawlability & Indexing

### robots.txt

Create a `robots.txt` file to be deployed at the root of the site:

**File:** `tiny-doc/src/docs/asciidoc/robots.txt`

```
User-agent: *
Allow: /

Sitemap: https://minigdx.github.io/tiny/sitemap.xml
```

### sitemap.xml

Create a sitemap listing all documentation pages:

**File:** `tiny-doc/src/docs/asciidoc/sitemap.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>https://minigdx.github.io/tiny/index.html</loc>
    <changefreq>monthly</changefreq>
    <priority>1.0</priority>
  </url>
  <url>
    <loc>https://minigdx.github.io/tiny/playground.html</loc>
    <changefreq>monthly</changefreq>
    <priority>0.8</priority>
  </url>
</urlset>
```

If the site is split into multiple pages, add each page to the sitemap.

### .nojekyll

Create a `.nojekyll` file in the deployed root to prevent GitHub Pages from processing files through Jekyll (which can skip files starting with `_`):

**File:** `tiny-doc/src/docs/asciidoc/.nojekyll` (empty file)

### Google Search Console

1. Register the site with [Google Search Console](https://search.google.com/search-console/)
2. Submit the sitemap
3. Monitor indexing status and any crawl errors
4. Check for mobile usability issues

### Build Pipeline Update

The `robots.txt`, `sitemap.xml`, and `.nojekyll` files need to be included in the deploy output. Add a copy task in `build.gradle.kts` or ensure they are in the Asciidoctor source directory so they get copied to the build output.

---

## Content Gaps & Opportunities

### Blog / Changelog

Fresh content is a signal to search engines that a site is active. Consider adding:

1. **A changelog page** listing new features per release
2. **Dev blog posts** or links to blog posts about Tiny (the Links section at the bottom of index.adoc already has two, but they are buried)

### FAQ Page

Create a FAQ targeting long-tail search queries:

- "What programming language does Tiny use?"
- "Can I export Tiny games to the web?"
- "Is Tiny free?"
- "How does Tiny compare to PICO-8?"
- "Can I use Tiny for game jams?"

### Comparison Content

People searching for game engines often compare options. A comparison page targeting terms like "PICO-8 alternatives" or "TIC-80 vs" could drive significant traffic. Key differentiators for Tiny:

- Free and open source (vs PICO-8 which is paid)
- Kotlin Multiplatform architecture
- Web export included
- Hot reload development
- CLI-based workflow

### Backlink Opportunities

1. **itch.io game pages:** Ensure all games made with Tiny link back to the Tiny website
2. **Game jam listings:** When using Tiny in jams, mention the engine with links
3. **Awesome lists:** Submit to "awesome-lua", "awesome-gamedev", "awesome-fantasy-consoles" GitHub lists
4. **Game dev forums/communities:** Share on Reddit r/gamedev, r/lua, r/indiegaming, Hacker News
5. **Conference talks:** The DroidKaigi 2024 talk is great — consider more conference submissions
6. **Developer blogs:** The existing Substack links are good — encourage more community content

---

## Competitive Positioning

### How Tiny Compares to Competitors in SEO

| Engine | Custom Domain | Meta Tags | Structured Data | Sitemap | Multi-page Docs | Social Previews |
|--------|--------------|-----------|----------------|---------|----------------|-----------------|
| PICO-8 | lexaloffle.com | Yes | No | No | Yes | Yes |
| TIC-80 | tic80.com | Yes | No | No | Yes | Basic |
| Pixel Vision 8 | Custom | Yes | No | No | Yes | Yes |
| **Tiny** | **github.io** | **No** | **No** | **No** | **No** | **No** |

### Key Takeaways

1. Tiny is the only one still using a `github.io` subdomain — all competitors have custom domains
2. Tiny is the only one missing basic meta tags
3. The lack of multi-page documentation puts Tiny at a structural disadvantage for search ranking

---

## Implementation Roadmap

### Phase 1: Quick Wins (Low Effort, High Impact)

These changes require minimal effort but provide immediate SEO benefits:

- [ ] Add `:description:` attribute to `index.adoc` and `playground.adoc`
- [ ] Add `:keywords:` attribute to `index.adoc` and `playground.adoc`
- [ ] Add Open Graph meta tags to `docinfo-header.html`
- [ ] Add Twitter Card meta tags to `docinfo-header.html`
- [ ] Add descriptive `alt` text to all images in `tiny-showcase.adoc`
- [ ] Create and deploy `robots.txt`
- [ ] Create and deploy `sitemap.xml`
- [ ] Create and deploy `.nojekyll`
- [ ] Add `SoftwareApplication` JSON-LD structured data to `docinfo-header.html`
- [ ] Add `<link rel="preconnect">` for Google Fonts
- [ ] Register with Google Search Console

### Phase 2: Content Improvements (Medium Effort, High Impact)

- [ ] Rewrite the homepage introduction with target keywords
- [ ] Create an OG image (1200x630px) for social sharing
- [ ] Improve the document title (currently `Tiny 🧸` — consider `Tiny Game Engine - Build Retro Games with Lua`)
- [ ] Add a "What is Tiny?" section with keyword-rich content
- [ ] Create a FAQ section or page
- [ ] Surface the external blog links more prominently

### Phase 3: Structural Changes (High Effort, High Impact)

- [ ] Split documentation into separate HTML pages (tutorial, API, CLI, showcase)
- [ ] Add inter-page navigation / breadcrumbs
- [ ] Consider a custom domain (e.g., `tiny-engine.dev`)
- [ ] Implement proper canonical URLs per page
- [ ] Create per-page Open Graph tags

### Phase 4: Performance & Advanced (Medium Effort, Medium Impact)

- [ ] Convert showcase GIFs to WebM/MP4 video elements
- [ ] Add lazy loading to images and iframes below the fold
- [ ] Self-host fonts to eliminate CDN dependencies
- [ ] Optimize favicon with multiple sizes (32x32, 180x180, 192x192, 512x512)
- [ ] Add a web app manifest for PWA support
- [ ] Set up automated Lighthouse CI testing in GitHub Actions

### Phase 5: Growth & Outreach (Ongoing)

- [ ] Submit to "awesome" lists on GitHub (awesome-gamedev, awesome-lua, awesome-fantasy-consoles)
- [ ] Create comparison content (Tiny vs PICO-8 vs TIC-80)
- [ ] Publish a game jam guide using Tiny
- [ ] Ensure all itch.io game pages link back to the Tiny website
- [ ] Present at additional conferences and meetups
- [ ] Encourage community blog posts and tutorials

---

## References

- [Asciidoctor Document Metadata](https://docs.asciidoctor.org/asciidoc/latest/document/metadata/)
- [Asciidoctor Docinfo Files](https://docs.asciidoctor.org/asciidoc/latest/docinfo/)
- [Asciidoctor SEO Tags Discussion (Issue #3882)](https://github.com/asciidoctor/asciidoctor/issues/3882)
- [Open Graph Protocol](https://ogp.me/)
- [Google Search Central Documentation](https://developers.google.com/search/docs)
- [Schema.org SoftwareApplication](https://schema.org/SoftwareApplication)
- [GitHub Pages SEO Guide](https://free-git-hosting.github.io/seo-for-github-hosted-sites/)
- [15 Essential SEO Tags in 2026](https://www.link-assistant.com/news/html-tags-for-seo.html)
