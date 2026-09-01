# Packing List — PWA (English, empty variant)

This variant starts **completely empty** — no categories, no locations, no items, no packing lists. Use it as a blank slate for your own inventory, or fill it in one go with the included starter file.

- `manifest.json` — app name, icons, startup behavior
- `service-worker.js` — offline cache
- `icons/` — app icons in all required sizes
- `starter-inventory-import.json` — an optional, ready-to-import starter inventory (see below)
- a few extra `<meta>` tags for iOS (Safari only partially supports the manifest)

## Important: doesn't work via double-click

Service workers (and therefore "install"/offline capability) only run over **`http://localhost` or `https://`** for security reasons, not over `file://`. Double-clicking `index.html` still opens the page, but without installability/offline cache.

### Test locally (on your computer)

Start a simple local server inside this folder, e.g.:

```
npx serve .
```

or

```
python -m http.server 8080
```

Then open `http://localhost:8080` (or whichever port is shown) in your browser. Chrome/Edge will then show an install icon in the address bar.

### Real installation on a smartphone

Your phone needs to load the file via a **reachable address** — either:

1. **On the same Wi-Fi**: start the local server (see above), find your computer's local IP (e.g. `192.168.x.x`), and open `http://192.168.x.x:8080` on your phone.
2. **Permanent, recommended**: host this folder for free, e.g. on GitHub Pages, Netlify (drag & drop the folder), or Vercel — required for "Add to Home Screen" on iOS and the install banner on Android.

## Filling the empty inventory

The app opens with nothing in it. You have two options:

1. **Build it up yourself**: Inventory tab → "+ New item", or "Bulk Add" for several items at once.
2. **Load the starter file**: footer → "⬆ Import (JSON)" → select `starter-inventory-import.json`. This fills the inventory with about 39 common, everyday packing items (camping furniture, sleeping gear, kitchen basics, tools, safety equipment, clothing, food, documents, etc.) spread across 15 categories and 7 example locations — no reference to a specific vehicle or person. Import **replaces** the current (empty) inventory, so it's safe to use right away.

You can edit, rename, or delete anything afterwards — the starter file is just a convenient starting point, not a fixed structure.

## When you change index.html in the future

The service worker caches `index.html` persistently. After content changes, bump the `CACHE_NAME` version in `service-worker.js` (e.g. `v1` → `v2`), otherwise already-installed users keep loading the old, cached version.
