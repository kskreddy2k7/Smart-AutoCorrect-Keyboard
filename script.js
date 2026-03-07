/**
 * Smart AutoCorrect Keyboard - Web App
 * script.js
 *
 * Implements:
 *  - Fetching and loading the dictionary from dictionary.json
 *  - Damerau-Levenshtein edit-distance algorithm
 *  - Autocorrect suggestion logic (with frequency-based ranking)
 *  - Real-time suggestion on every keystroke
 */

// ─── State ────────────────────────────────────────────────────────────────────

/** @type {Object.<string, number>} word → frequency map loaded from dictionary.json */
let dictionary = {};

/** True once dictionary.json has been fetched and parsed */
let dictionaryLoaded = false;

// ─── DOM references ───────────────────────────────────────────────────────────

const wordInput       = document.getElementById("wordInput");
const inputDisplay    = document.getElementById("inputDisplay");
const suggestionBox   = document.getElementById("suggestionBox");
const loadingMessage  = document.getElementById("loadingMessage");

// ─── Dictionary loading ───────────────────────────────────────────────────────

/**
 * Fetch dictionary.json and populate the `dictionary` map.
 * Shows an error in the suggestion box if the fetch fails.
 */
async function loadDictionary() {
  try {
    const response = await fetch("dictionary.json");
    if (!response.ok) {
      throw new Error(`HTTP error – status ${response.status}`);
    }
    dictionary = await response.json();
    dictionaryLoaded = true;

    // Hide loading indicator and enable the input field
    loadingMessage.style.display = "none";
    wordInput.disabled = false;
    wordInput.focus();
  } catch (error) {
    console.error("Failed to load dictionary:", error);
    loadingMessage.textContent = "⚠️ Could not load dictionary. Please refresh.";
    loadingMessage.classList.add("error");
  }
}

// ─── Edit-distance algorithm ──────────────────────────────────────────────────

/**
 * Compute the Damerau-Levenshtein distance between two strings.
 *
 * Counts the minimum number of single-character edits (insertions, deletions,
 * substitutions, and adjacent transpositions) required to transform `a` into `b`.
 *
 * @param {string} a - Source string
 * @param {string} b - Target string
 * @returns {number} Edit distance
 */
function editDistance(a, b) {
  const lenA = a.length;
  const lenB = b.length;

  // Create a 2-D matrix filled with zeros
  // dp[i][j] = distance between a[0..i-1] and b[0..j-1]
  const dp = Array.from({ length: lenA + 1 }, () => new Array(lenB + 1).fill(0));

  // Base cases: transforming to/from empty string
  for (let i = 0; i <= lenA; i++) dp[i][0] = i;
  for (let j = 0; j <= lenB; j++) dp[0][j] = j;

  // Fill the matrix
  for (let i = 1; i <= lenA; i++) {
    for (let j = 1; j <= lenB; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1; // 0 if same character, else 1

      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,       // deletion
        dp[i][j - 1] + 1,       // insertion
        dp[i - 1][j - 1] + cost // substitution
      );

      // Transposition (Damerau extension) – always costs 1 operation
      if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) {
        dp[i][j] = Math.min(dp[i][j], dp[i - 2][j - 2] + 1);
      }
    }
  }

  return dp[lenA][lenB];
}

// ─── Autocorrect logic ────────────────────────────────────────────────────────

/**
 * Determine the maximum allowable edit distance for a word of a given length.
 * Mirrors the tolerance table used in the Android Kotlin implementation.
 *
 * @param {number} len - Length of the typed word
 * @returns {number} Maximum allowed edit distance
 */
function maxDistance(len) {
  if (len <= 3) return 0; // Very short words: no correction
  if (len <= 5) return 1; // Medium-short: allow 1 edit
  if (len <= 8) return 2; // Medium: allow 2 edits
  return 3;               // Long words: allow 3 edits
}

/**
 * Find the best autocorrect suggestion for a typed word.
 *
 * Candidates are filtered by edit-distance tolerance and then ranked by a
 * combined score that weighs both similarity (lower distance is better) and
 * word frequency (higher frequency is better).
 *
 * @param {string} input - The word typed by the user (already lower-cased)
 * @returns {{ word: string, distance: number } | null} Best match, or null if none
 */
function getSuggestion(input) {
  if (!input || input.length === 0) return null;

  // If the input is already a valid dictionary word, return it as-is
  if (dictionary.hasOwnProperty(input)) {
    return { word: input, distance: 0 };
  }

  const threshold = maxDistance(input.length);
  let bestWord     = null;
  let bestScore    = -Infinity;

  for (const [word, frequency] of Object.entries(dictionary)) {
    // Skip candidates whose length difference alone exceeds the threshold
    // (a quick pre-filter to avoid computing full edit distance)
    if (Math.abs(word.length - input.length) > threshold) continue;

    const dist = editDistance(input, word);
    if (dist > threshold) continue; // Outside allowed tolerance

    // Score: higher frequency is better, lower distance is better
    // We weight frequency lightly so that very common words still win
    const score = frequency - dist * 50;
    if (score > bestScore) {
      bestScore = score;
      bestWord  = { word, distance: dist };
    }
  }

  return bestWord;
}

// ─── UI update ────────────────────────────────────────────────────────────────

/**
 * Called on every `input` event from the text box.
 * Updates the "Input Word" display and the "Suggested Correction" section.
 */
function onInput() {
  const raw   = wordInput.value;
  const input = raw.trim().toLowerCase();

  // Show what the user typed
  inputDisplay.textContent = raw.length > 0 ? raw : "—";

  if (!dictionaryLoaded) {
    showSuggestion(null, "Dictionary not loaded yet…");
    return;
  }

  if (input.length === 0) {
    showSuggestion(null, "Start typing above to see a suggestion.");
    return;
  }

  const result = getSuggestion(input);
  showSuggestion(result, null);
}

/**
 * Render the autocorrect result inside the suggestion box.
 *
 * @param {{ word: string, distance: number } | null} result - Suggestion result
 * @param {string | null} fallbackMessage - Message to show when result is null
 */
function showSuggestion(result, fallbackMessage) {
  // Clear previous content
  suggestionBox.innerHTML = "";

  if (!result) {
    // No match found or empty input
    const msg = document.createElement("p");
    msg.className = "no-suggestion";
    msg.textContent = fallbackMessage || "No suggestion found for this word.";
    suggestionBox.appendChild(msg);
    return;
  }

  if (result.distance === 0) {
    // Perfect match – word is already correct
    const row = createResultRow("✅ Correct!", result.word, "tag-correct");
    suggestionBox.appendChild(row);
  } else {
    // A correction was found
    const row = createResultRow("💡 Suggested correction:", result.word, "tag-suggestion");
    suggestionBox.appendChild(row);

    // Show how many edits were needed
    const detail = document.createElement("p");
    detail.className = "edit-detail";
    detail.textContent = `(edit distance: ${result.distance})`;
    suggestionBox.appendChild(detail);
  }
}

/**
 * Build a result row element.
 *
 * @param {string} label - Descriptive label text
 * @param {string} word  - The word to display prominently
 * @param {string} tagClass - CSS class applied to the word badge
 * @returns {HTMLDivElement}
 */
function createResultRow(label, word, tagClass) {
  const row = document.createElement("div");
  row.className = "result-row";

  const labelEl = document.createElement("span");
  labelEl.className = "result-label";
  labelEl.textContent = label;

  const wordEl = document.createElement("span");
  wordEl.className = `result-word ${tagClass}`;
  wordEl.textContent = word;

  row.appendChild(labelEl);
  row.appendChild(wordEl);
  return row;
}

// ─── Bootstrap ────────────────────────────────────────────────────────────────

// Wire up the input handler
wordInput.addEventListener("input", onInput);

// Load the dictionary when the page is ready
loadDictionary();
