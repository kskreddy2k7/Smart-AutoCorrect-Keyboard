# Smart AutoCorrect Keyboard

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
