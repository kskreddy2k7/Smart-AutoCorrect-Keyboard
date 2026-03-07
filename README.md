# Smart AutoCorrect Keyboard

A production-ready Android custom keyboard **and GitHub Pages web app** with intelligent autocorrect powered by edit-distance algorithms.

---

## 🌐 Web App (GitHub Pages)

A fully client-side autocorrect demo that runs directly in any modern browser — no server, no build step required.

### Live Demo

> After enabling GitHub Pages (see below), your demo will be available at:
> `https://<your-username>.github.io/Smart-AutoCorrect-Keyboard/`

### Web App Features

- **Real-time autocorrect** — suggestions appear on every keystroke
- **Damerau-Levenshtein edit-distance** implemented in pure JavaScript
- **Frequency-ranked suggestions** — common words win ties
- **Runs 100% offline** — no network calls after the initial page load
- **Responsive card layout** — looks great on mobile and desktop

### Web App Files

| File | Purpose |
|---|---|
| `index.html` | Page structure — input box, result displays, accessible markup |
| `style.css` | Modern, responsive card UI |
| `script.js` | Edit-distance algorithm and autocorrect logic |
| `dictionary.json` | English word → frequency map (used by the autocorrect engine) |

### Running Locally

Simply open `index.html` in your browser:

```bash
# Option 1 – double-click index.html in your file manager

# Option 2 – serve with Python (avoids fetch() restrictions in some browsers)
python -m http.server 8080
# then visit http://localhost:8080
```

### Deploying to GitHub Pages

1. Push the repository to GitHub (all four web-app files must be in the **root** of the default branch).
2. Go to **Settings → Pages** in your GitHub repository.
3. Under **Source**, choose **Deploy from a branch**.
4. Set the branch to `main` (or `master`) and the folder to `/ (root)`.
5. Click **Save**.
6. GitHub will publish the site — the URL appears at the top of the Pages settings page.

> ℹ️ Changes pushed to the selected branch are automatically redeployed within a few minutes.

### How the Autocorrect Works

1. `dictionary.json` is fetched asynchronously on page load.
2. The typed word is lower-cased and looked up directly in the dictionary (O(1)).  
   If found → "✅ Correct!"; if not → the edit-distance search runs.
3. Every dictionary word whose length difference from the input is within the allowed threshold is scored:
   - **Edit distance** is computed with the Damerau-Levenshtein algorithm (handles swaps, inserts, deletes, substitutions).
   - **Score** = `frequency − distance × 50` so common words win ties.
4. The highest-scoring candidate within the tolerance window is shown.

Edit-distance tolerance (mirrors the Android Kotlin implementation):

| Input length | Max allowed distance |
|---|---|
| ≤ 3 | 0 (no correction) |
| 4–5 | 1 |
| 6–8 | 2 |
| ≥ 9 | 3 |

---

## 📱 Android App

A production-ready Android custom keyboard with intelligent autocorrect, word suggestions, bigram prediction, and Clean Architecture.

## Features

- **Smart AutoCorrect**: Damerau-Levenshtein edit-distance correction with frequency-based ranking
- **Word Suggestions Bar**: Real-time suggestions (up to 3) shown as you type, with debounced updates
- **Bigram Prediction**: Next-word prediction based on the previously typed word
- **Adaptive Learning**: Learns from user typing via Room database — frequently typed words rank higher
- **Multi-language Support**: English and Hindi (romanized) dictionaries; easily extensible
- **TFLite Ready**: Optional TensorFlow Lite model integration for enhanced ML predictions
- **Settings UI**: Configure autocorrect, suggestions, language, and theme via `SettingsActivity`
- **Clean Architecture**: Domain / Data / UI layers with Hilt DI, MVVM-ready
- **CI/CD**: GitHub Actions pipeline runs unit tests and builds debug APK on every push

## Architecture

```
com.smartautocorrect.keyboard/
├── SmartKeyboardApplication.kt       # @HiltAndroidApp entry point
├── domain/
│   ├── model/
│   │   ├── WordSuggestion.kt         # Suggestion data class
│   │   └── UserWord.kt               # Domain model for learned words
│   ├── repository/
│   │   └── DictionaryRepository.kt   # Repository interface
│   └── usecase/
│       ├── GetSuggestionsUseCase.kt  # Retrieve autocorrect/prediction suggestions
│       └── LearnWordUseCase.kt       # Record typed words for adaptive learning
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt            # Room database
│   │   ├── UserWordDao.kt            # DAO for user word frequency
│   │   └── UserWordEntity.kt         # Room entity
│   ├── dictionary/
│   │   └── DictionaryRepositoryImpl.kt  # JSON dict + Levenshtein + bigram impl
│   └── di/
│       └── AppModule.kt              # Hilt module (singleton bindings)
├── ml/
│   ├── PredictionEngine.kt           # In-memory bigram prediction
│   └── TFLiteHelper.kt               # Optional TFLite model wrapper
├── utils/
│   ├── LevenshteinUtils.kt           # Damerau-Levenshtein distance + similarity
│   └── ThemeManager.kt               # SharedPreferences-backed theme/settings
└── ui/
    ├── keyboard/
    │   ├── KeyboardService.kt        # @AndroidEntryPoint InputMethodService
    │   └── SuggestionAdapter.kt      # RecyclerView adapter for suggestion bar
    └── settings/
        └── SettingsActivity.kt       # Preference-based settings screen
```

## Project Structure

```
Smart-AutoCorrect-Keyboard/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   ├── dictionary_en.json        # English word frequency dict
│   │   │   │   └── dictionary_hi.json        # Hindi romanized dict
│   │   │   ├── java/com/smartautocorrect/keyboard/   # Kotlin sources (see above)
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── keyboard_view.xml     # Keyboard + suggestion bar
│   │   │   │   │   ├── item_suggestion.xml   # Individual suggestion chip
│   │   │   │   │   └── activity_settings.xml # Settings host layout
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── arrays.xml            # Language/theme option arrays
│   │   │   │   └── xml/
│   │   │   │       ├── keyboard_layout.xml   # QWERTY key definitions
│   │   │   │       ├── keyboard_preferences.xml  # Preference screen XML
│   │   │   │       └── method.xml            # IME metadata
│   │   │   └── AndroidManifest.xml
│   │   └── test/                             # JUnit unit tests
│   ├── build.gradle
│   └── proguard-rules.pro
├── scripts/
│   └── train_bigram_model.py         # Offline bigram training script
├── .github/workflows/android.yml     # CI pipeline
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Building the Project

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API Level 24+

### Build Steps

```bash
git clone https://github.com/kskreddy2k7/Smart-AutoCorrect-Keyboard.git
cd Smart-AutoCorrect-Keyboard
./gradlew assembleDebug
```

### Run Unit Tests

```bash
./gradlew test
```

## Installation

1. Build and install the APK on your Android device
2. Go to **Settings → System → Languages & Input → Virtual Keyboard**
3. Enable **Smart AutoCorrect Keyboard**
4. Select it when typing in any app
5. Access keyboard settings via the settings gear icon

## Bigram Model Training

Generate a bigram model from your own text corpus:

```bash
# Demo mode (uses built-in sample corpus)
python scripts/train_bigram_model.py --demo

# Custom corpus
python scripts/train_bigram_model.py --input my_corpus.txt --output bigrams.json --min-count 3
```

Place the resulting `bigrams.json` in `app/src/main/assets/` and load it via `PredictionEngine.loadBigrams()`.

## TFLite Integration

To enable ML-based next-word prediction:

1. Train or obtain a TFLite next-word prediction model
2. Place the model file at `app/src/main/assets/keyboard_model.tflite`
3. Implement inference logic in `TFLiteHelper.predict()`

`TFLiteHelper` automatically detects the model at startup. If absent, the app gracefully falls back to the bigram rule-based model.

## Technical Details

| Property | Value |
|---|---|
| Language | Kotlin |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |
| Architecture | Clean Architecture (Domain/Data/UI) |
| DI | Hilt 2.50 |
| Database | Room 2.6.1 |
| Coroutines | 1.7.3 |
| Build System | Gradle 8.2.2 |

## Autocorrect Algorithm

`LevenshteinUtils` computes Damerau-Levenshtein distance (insertions, deletions, substitutions, and adjacent transpositions). Edit-distance tolerance scales with word length:

| Word Length | Max Allowed Distance |
|---|---|
| ≤ 3 | 0 (no correction) |
| 4–5 | 1 |
| 6–8 | 2 |
| ≥ 9 | 3 |

Suggestions are ranked by a combined score of edit similarity and word frequency.

## License

This project is open source and available for educational purposes.
