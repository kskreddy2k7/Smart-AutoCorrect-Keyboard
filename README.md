# Smart AutoCorrect Keyboard

An Android custom keyboard application with intelligent auto-correct functionality using the Levenshtein distance algorithm.

## Features

- **Custom QWERTY Keyboard Layout**: Full QWERTY keyboard with Space, Backspace (Delete), and Enter (Done) keys
- **Auto-Correct Functionality**: Automatically corrects misspelled words when Space is pressed
- **Levenshtein Algorithm**: Uses edit distance algorithm to find the closest match from dictionary
- **Word Buffer**: Maintains current word being typed for intelligent correction
- **Hardcoded Dictionary**: Contains 100+ common English words

## Project Structure

```
SmartAutoCorrectKeyboard/
├── app/
│   ├── src/main/
│   │   ├── java/com/smartautocorrect/keyboard/
│   │   │   ├── MyKeyboardService.java    # Main keyboard service
│   │   │   └── SpellChecker.java         # Auto-correct logic
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── keyboard_view.xml     # Keyboard view layout
│   │   │   └── xml/
│   │   │       ├── keyboard_layout.xml   # QWERTY keyboard layout
│   │   │       └── method.xml            # Input method metadata
│   │   └── AndroidManifest.xml           # App manifest
│   ├── build.gradle                      # App build configuration
│   └── proguard-rules.pro
├── build.gradle                          # Project build configuration
├── settings.gradle                       # Project settings
└── gradle.properties                     # Gradle properties
```

## Key Components

### MyKeyboardService.java
- Extends `InputMethodService` to create a custom keyboard
- Implements `KeyboardView.OnKeyboardActionListener` for key press handling
- Maintains a word buffer to track current word being typed
- Integrates with SpellChecker for auto-correction on space press

### SpellChecker.java
- Contains a hardcoded dictionary of common English words
- Implements the Levenshtein distance algorithm for spell checking
- Provides `correctWord()` method that returns the closest dictionary match
- Only suggests corrections with edit distance ≤ 2

### keyboard_layout.xml
- Defines QWERTY layout with 3 rows of letter keys
- Includes Space (50% width), Delete (25% width), and Done (25% width) keys
- Configures key sizes, gaps, and edge flags

### method.xml
- Metadata file for the Input Method Service
- Defines settings activity and icon for the keyboard

## Building the Project

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK API Level 21 or higher
- Gradle 8.0

### Build Steps

1. Clone the repository:
```bash
git clone https://github.com/kskreddy2k7/Smart-AutoCorrect-Keyboard.git
cd Smart-AutoCorrect-Keyboard
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Build the project:
```bash
./gradlew build
```

Or build from Android Studio: **Build → Make Project**

## Installation

1. Build and install the APK on your Android device
2. Go to **Settings → System → Languages & Input → Virtual Keyboard**
3. Enable "Smart AutoCorrect Keyboard"
4. Select the keyboard when typing in any app

## How It Works

1. User types characters on the keyboard
2. Each character is added to a word buffer
3. When Space is pressed:
   - The current word buffer is checked against the dictionary
   - If not found, Levenshtein algorithm finds the closest match
   - The word is automatically corrected if a match is found within edit distance of 2
   - The word buffer is cleared
4. Backspace removes characters from both input and word buffer
5. Enter key submits the input and clears the buffer

## Technical Details

- **Minimum SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Language**: Java
- **Build System**: Gradle

## Auto-Correct Algorithm

The spell checker uses the Levenshtein distance algorithm to calculate the edit distance between the typed word and dictionary words. The algorithm considers three operations:
- Insertion of a character
- Deletion of a character  
- Substitution of a character

The word with minimum edit distance (≤ 2) is selected as the correction.

## License

This project is open source and available for educational purposes.
