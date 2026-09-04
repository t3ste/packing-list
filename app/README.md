# Packing List — Android app (standalone)

A real, self-contained Android app built with [Capacitor](https://capacitorjs.com/):
the same HTML/CSS/JS from the web app is bundled directly inside the app and
rendered by Android's built-in system WebView — a fixed OS component, not a
separately installed browser. **No Chrome (or any other browser) needs to be
installed** for this app to work, unlike the earlier Trusted Web Activity (TWA)
approach, which launched the installed Chrome browser in a chromeless window.

`www/` holds the bundled copy of the web app — it's a snapshot, not a live link
to the deployed site at t3ste.github.io/packing-list. Pulling in new web app
features means re-copying the updated `index.html`/`manifest.json`/`icons/`
from the repo root into `www/`, then re-applying the two Capacitor-specific
edits (see **Bundled-copy differences** below) before rebuilding.

## Native file linking

The web app's "🔗 Link" feature (browser File System Access API) doesn't exist
in Android's WebView at all. This app ships its own custom native plugin,
[`FileLinkPlugin.kt`](android/app/src/main/java/com/t3ste/packinglist/FileLinkPlugin.kt),
built on Android's Storage Access Framework, to do the same job: pick or
create one file, and keep reusing it automatically — across app restarts —
until told otherwise via the menu's link button.

- **First launch**: the app asks once whether to save to a new file, open an
  existing one, or skip. Skipping isn't a dead end — data is still saved
  automatically, first to the WebView's own local storage, and mirrored into
  a private file inside the app's own storage (via the official
  [`@capacitor/filesystem`](https://github.com/ionic-team/capacitor-plugins)
  plugin) — so "no file chosen" never means "no backup", it just means the
  file isn't somewhere you can browse to yourself.
- **Later**: the same choice is available any time from the ☰ menu's link
  button, to switch to a different file.

## Bundled-copy differences from the live web app

Two things were changed in `www/index.html` relative to the deployed copy,
since neither makes sense inside a bundled native app:

1. Service worker registration removed — there's nothing to fetch, the app
   ships its own content.
2. The header's cache-timestamp display removed along with it (no
   `service-worker.js` shipped here to read a build date from).

Everything else — including the native file-link bridge — is additive, gated
behind `window.Capacitor.isNativePlatform()`, so none of it affects the
regular web app's behavior if this file were ever compared against it.

## Rebuilding

Requirements: Node.js, JDK 17 (Gradle 8.x doesn't support newer JDKs yet —
Android Studio's bundled JBR at `<Android Studio install>/jbr` works well),
the Android SDK (`build-tools`, `platform-tools`).

```
cd app
npm install
npx cap sync android
```

Then either open `app/android` in Android Studio, or build from the command
line (see `.github/workflows/android-release.yml` for the exact commands this
project's CI uses, including alignment and signing).

`android/android.keystore` and `android/local.properties` are **not**
committed (see `.gitignore`) — same reasoning and same file as documented in
the (now removed) TWA version: back the keystore up somewhere safe, it's the
app's permanent signing identity.
