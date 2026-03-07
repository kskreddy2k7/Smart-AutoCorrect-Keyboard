# ⌨️ Smart AutoCorrect Keyboard

> **AI-powered autocorrect typing assistant** — runs 100% in the browser via GitHub Pages. No backend, no account, no internet required after first load.

[![Live Demo](https://img.shields.io/badge/Live%20Demo-GitHub%20Pages-6c63ff?style=for-the-badge&logo=github)](https://kskreddy2k7.github.io/Smart-AutoCorrect-Keyboard/)
[![PWA Ready](https://img.shields.io/badge/PWA-Ready-4ade80?style=for-the-badge&logo=pwa)](https://kskreddy2k7.github.io/Smart-AutoCorrect-Keyboard/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

---

## ✨ Features

| Feature | Details |
|---------|---------|
| 🔴 **Real-time highlighting** | Misspelled words are underlined in red as you type |
| 💡 **Top 5 suggestions** | Ranked by Levenshtein distance + word frequency |
| 🖱️ **Click to replace** | Tap any suggestion chip to instantly fix the word |
| 📊 **Live statistics** | Word count, correction count, accuracy percentage |
| 📱 **Mobile responsive** | Fluid layout that works on every screen size |
| 📲 **PWA / Installable** | Add to home screen on iOS & Android |
| ✈️ **Offline capable** | Service Worker caches all assets after first visit |
| 🔒 **100% private** | All processing is local — nothing leaves your browser |

---

## 🧠 Algorithm — Levenshtein Distance

The autocorrect engine uses the classic **Levenshtein edit-distance** algorithm to measure how "close" any two words are:

```
distance(a, b) = minimum number of single-character edits
                 (insertions · deletions · substitutions)
                 required to transform a into b
```

### Tolerance table

| Word length | Max allowed distance |
|-------------|----------------------|
| ≤ 3         | 0 — no correction    |
| 4 – 5       | 1                    |
| 6 – 8       | 2                    |
| ≥ 9         | 3                    |

Suggestions are ranked by a combined score:

```
score = distance − log₂(frequency) × 0.1
```

Lower score = better suggestion. This ensures that common words rank ahead of rare words at the same edit distance.

### Time complexity

| Step | Complexity |
|------|-----------|
| Single pair comparison | O(m × n) where m, n are word lengths |
| Dictionary scan | O(D × m × n) — optimised with length pre-filter |
| Length pre-filter | Skips any dict word whose length differs by > maxDist |

---

## 📁 Project Structure

```
Smart-AutoCorrect-Keyboard/
├── index.html        ← Single-page app shell
├── style.css         ← Dark-theme responsive styles
├── script.js         ← Levenshtein engine + UI logic
├── words.json        ← 500+ word dictionary
├── manifest.json     ← PWA manifest
├── sw.js             ← Service Worker (offline support)
├── assets/
│   └── icon.png      ← App icon (192 × 192)
└── README.md
```

---

## 🚀 GitHub Pages Deployment

### One-click setup

1. Fork or clone this repository
2. Go to **Settings → Pages**
3. Under *Source*, choose **Deploy from a branch**
4. Select branch `main` (or your default branch) and folder `/ (root)`
5. Click **Save**

Your app will be live at:
```
https://<your-username>.github.io/Smart-AutoCorrect-Keyboard/
```

### Local development

No build step required — just open `index.html` in a browser:

```bash
# Option 1: Python simple server (recommended — needed for fetch() + SW)
python3 -m http.server 8080
# Then open http://localhost:8080

# Option 2: Node.js
npx serve .
```

> ⚠️ **Important:** Open via `http://localhost:…` rather than `file://` so that `fetch('words.json')` and the Service Worker work correctly.

---

## 📸 Screenshots

### Desktop view
The main card with real-time highlighted preview and suggestion chips:

```
┌─────────────────────────────────────────────┐
│  ● ● ●    autocorrect.ai — typing assistant  │
├─────────────────────────────────────────────┤
│  YOUR TEXT                                   │
│  ┌───────────────────────────────────────┐  │
│  │ Teh quik brwon fox jmps over the…     │  │
│  └───────────────────────────────────────┘  │
│  HIGHLIGHTED PREVIEW                         │
│  [Teh] [quik] [brwon] fox [jmps] over the… │
│  SUGGESTIONS                                 │
│  [① the d=1] [② ten d=2] [③ tea d=2]       │
├──────┬──────┬──────┬────────────────────────┤
│  12  │  4   │  67% │  550                   │
│ Words│Fixes │Accur.│ Dict                   │
└──────┴──────┴──────┴────────────────────────┘
```

---

## 🛠️ Customising the Dictionary

`words.json` is a plain JSON array of strings. Add your own domain-specific terms:

```json
{
  "words": [
    "javascript", "typescript", "react", "your-custom-term",
    ...
  ]
}
```

The engine will deduplicate and sort the list automatically at load time.

---

## 🤝 Contributing

Pull requests are welcome! Please open an issue first to discuss what you would like to change.

---

## 📄 Licence

This project is open source and available under the [MIT Licence](LICENSE).

---

<p align="center">Made with ❤️ — runs 100% in your browser</p>
