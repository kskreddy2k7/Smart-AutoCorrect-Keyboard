/**
 * Smart AutoCorrect Keyboard – script.js
 * AI-powered browser autocorrect using Levenshtein distance
 */

'use strict';

// ── State ──────────────────────────────────────────────────────────────────
const state = {
  dictionary: [],
  wordFrequency: {},
  lastWord: '',
  totalCorrections: 0,
  totalWords: 0,
  accuracy: 100,
  activeTarget: null,   // { word, startIndex } of word being corrected
  debounceTimer: null,
};

// ── DOM references ─────────────────────────────────────────────────────────
const inputEl        = document.getElementById('userInput');
const highlightEl    = document.getElementById('highlightedText');
const suggestionsEl  = document.getElementById('suggestionsPanel');
const statWords      = document.getElementById('statWords');
const statCorrections= document.getElementById('statCorrections');
const statAccuracy   = document.getElementById('statAccuracy');
const statDict       = document.getElementById('statDict');
const charCountEl    = document.getElementById('charCount');
const clearBtn       = document.getElementById('clearBtn');
const copyBtn        = document.getElementById('copyBtn');
const toastEl        = document.getElementById('toast');

// ── Optimal String Alignment distance (Damerau-Levenshtein variant) ───────
/**
 * Compute edit distance between two strings, counting adjacent-character
 * transpositions as a single edit (e.g. "teh" → "the" costs 1, not 2).
 * Uses the Optimal String Alignment (OSA) algorithm which requires three
 * row buffers to detect transpositions.
 * @param {string} a
 * @param {string} b
 * @returns {number}
 */
function levenshtein(a, b) {
  if (a === b) return 0;
  if (a.length === 0) return b.length;
  if (b.length === 0) return a.length;

  const aLen = a.length;
  const bLen = b.length;

  // Three rows: prev2 (i-2), prev (i-1), curr (i)
  let prev2 = new Uint16Array(bLen + 1);
  let prev  = new Uint16Array(bLen + 1);
  let curr  = new Uint16Array(bLen + 1);

  for (let j = 0; j <= bLen; j++) prev[j] = j;

  for (let i = 1; i <= aLen; i++) {
    curr[0] = i;
    for (let j = 1; j <= bLen; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      curr[j] = Math.min(
        prev[j] + 1,          // deletion
        curr[j - 1] + 1,      // insertion
        prev[j - 1] + cost    // substitution
      );
      // Transposition: a[i-2]==b[j-1] && a[i-1]==b[j-2]
      if (i > 1 && j > 1 && a[i - 1] === b[j - 2] && a[i - 2] === b[j - 1]) {
        curr[j] = Math.min(curr[j], prev2[j - 2] + cost);
      }
    }
    [prev2, prev, curr] = [prev, curr, prev2];
  }

  return prev[bLen];
}

// ── Max edit distance tolerance based on word length ──────────────────────
function maxDistance(len) {
  if (len <= 3) return 0;
  if (len <= 5) return 1;
  if (len <= 8) return 2;
  return 3;
}

// ── Get top-N suggestions for a given word ────────────────────────────────
/**
 * @param {string} word  - lowercased input token
 * @param {number} limit - max suggestions to return
 * @returns {Array<{word:string, distance:number, score:number}>}
 */
function getSuggestions(word, limit = 5) {
  if (word.length < 2) return [];

  const maxDist = maxDistance(word.length);
  if (maxDist === 0) return [];

  const candidates = [];
  const wLen = word.length;

  for (let i = 0; i < state.dictionary.length; i++) {
    const dictWord = state.dictionary[i];
    const dLen = dictWord.length;

    // Quick length filter: skip if length difference > maxDist
    if (Math.abs(dLen - wLen) > maxDist) continue;

    const dist = levenshtein(word, dictWord);
    if (dist === 0) return [];  // exact match
    if (dist <= maxDist) {
      const freq = state.wordFrequency[dictWord] || 1;
      // Score: lower distance is better; higher frequency is better
      const score = dist - Math.log2(freq) * 0.1;
      candidates.push({ word: dictWord, distance: dist, score });
    }
  }

  candidates.sort((a, b) => a.score - b.score || a.distance - b.distance);
  return candidates.slice(0, limit);
}

// ── Check if a word is in the dictionary ──────────────────────────────────
function isKnownWord(word) {
  const lower = word.toLowerCase().replace(/[^a-z]/g, '');
  if (lower.length === 0) return true;
  // Binary search (dictionary is sorted during load)
  let lo = 0, hi = state.dictionary.length - 1;
  while (lo <= hi) {
    const mid = (lo + hi) >>> 1;
    if (state.dictionary[mid] === lower) return true;
    if (state.dictionary[mid] < lower) lo = mid + 1;
    else hi = mid - 1;
  }
  return false;
}

// ── Tokenise text into words, preserving non-word spans ───────────────────
function tokenise(text) {
  const tokens = [];
  const re = /([a-zA-Z']+)|([^a-zA-Z']+)/g;
  let m;
  while ((m = re.exec(text)) !== null) {
    tokens.push({ text: m[0], isWord: !!m[1], index: m.index });
  }
  return tokens;
}

// ── Render highlighted preview ────────────────────────────────────────────
function renderHighlight(text) {
  if (!text.trim()) {
    highlightEl.innerHTML = '';
    return;
  }

  const tokens = tokenise(text);
  const fragments = tokens.map(tok => {
    if (!tok.isWord) {
      return document.createTextNode(tok.text);
    }
    const clean = tok.text.replace(/[^a-zA-Z]/g, '').toLowerCase();
    if (clean.length >= 2 && !isKnownWord(clean)) {
      const span = document.createElement('span');
      span.className = 'misspelled';
      span.textContent = tok.text;
      span.dataset.word = clean;
      span.title = 'Click to see suggestions';
      span.addEventListener('click', () => focusWordSuggestions(clean));
      return span;
    }
    return document.createTextNode(tok.text);
  });

  highlightEl.innerHTML = '';
  fragments.forEach(f => highlightEl.appendChild(f));
}

// ── Focus suggestions for a specific word ────────────────────────────────
function focusWordSuggestions(word) {
  state.activeTarget = word;
  displaySuggestions(word);
}

// ── Render suggestion chips ────────────────────────────────────────────────
function displaySuggestions(word) {
  const lower = word.toLowerCase().replace(/[^a-z]/g, '');
  if (!lower || lower.length < 2) {
    suggestionsEl.innerHTML = '<span class="no-suggestions">Start typing to get suggestions…</span>';
    return;
  }

  if (isKnownWord(lower)) {
    suggestionsEl.innerHTML = `<span class="no-suggestions">✓ "${lower}" looks correct</span>`;
    return;
  }

  const results = getSuggestions(lower);

  if (results.length === 0) {
    suggestionsEl.innerHTML = '<span class="no-suggestions">No suggestions found</span>';
    return;
  }

  suggestionsEl.innerHTML = '';
  results.forEach((r, idx) => {
    const chip = document.createElement('button');
    chip.className = 'suggestion-chip';
    chip.innerHTML = `
      <span class="chip-rank">${idx + 1}</span>
      <span class="chip-word">${r.word}</span>
      <span class="chip-dist">d=${r.distance}</span>
    `;
    chip.setAttribute('aria-label', `Replace with ${r.word}`);
    chip.addEventListener('click', () => applySuggestion(word, r.word));
    suggestionsEl.appendChild(chip);
  });
}

// ── Apply a suggestion: replace the word in the textarea ──────────────────
function applySuggestion(originalWord, replacement) {
  const text = inputEl.value;
  const lowerText = text.toLowerCase();
  const lowerWord = originalWord.toLowerCase();

  // Find and replace the last occurrence that matches
  let idx = -1;
  let searchFrom = 0;
  while (true) {
    const found = lowerText.indexOf(lowerWord, searchFrom);
    if (found === -1) break;
    // Ensure it's a whole word
    const before = found === 0 || !/[a-zA-Z]/.test(text[found - 1]);
    const after  = found + lowerWord.length >= text.length || !/[a-zA-Z]/.test(text[found + lowerWord.length]);
    if (before && after) idx = found;
    searchFrom = found + 1;
  }

  if (idx === -1) return;

  // Preserve original capitalisation
  const original = text.slice(idx, idx + originalWord.length);
  let corrected = replacement;
  if (original[0] === original[0].toUpperCase() && original[0] !== original[0].toLowerCase()) {
    corrected = replacement[0].toUpperCase() + replacement.slice(1);
  }

  inputEl.value = text.slice(0, idx) + corrected + text.slice(idx + original.length);
  state.totalCorrections++;
  updateStats();
  processInput();
  showToast(`✓ Replaced "${original}" → "${corrected}"`);

  // Refocus input
  inputEl.focus();
  const newCursor = idx + corrected.length;
  inputEl.setSelectionRange(newCursor, newCursor);
}

// ── Main input processor ──────────────────────────────────────────────────
function processInput() {
  const text = inputEl.value;
  charCountEl.textContent = `${text.length} / 2000`;

  const tokens = tokenise(text);
  const words = tokens.filter(t => t.isWord && t.text.length >= 2);
  state.totalWords = words.length;

  // Render highlight
  renderHighlight(text);

  // Find the word at cursor for live suggestion
  const cursorPos = inputEl.selectionStart;
  const wordAtCursor = getWordAtCursor(text, cursorPos);

  if (wordAtCursor) {
    displaySuggestions(wordAtCursor);
  } else {
    // Show suggestions for the last misspelled word in the text
    const misspelled = words.filter(t => !isKnownWord(t.text.toLowerCase().replace(/[^a-z]/g, '')));
    if (misspelled.length > 0) {
      const last = misspelled[misspelled.length - 1];
      displaySuggestions(last.text.replace(/[^a-zA-Z]/g, ''));
    } else {
      suggestionsEl.innerHTML = text.trim()
        ? '<span class="no-suggestions">✓ No corrections needed</span>'
        : '<span class="no-suggestions">Start typing to get suggestions…</span>';
    }
  }

  updateStats();
}

// ── Get the word the cursor is currently inside ────────────────────────────
function getWordAtCursor(text, pos) {
  if (pos === 0) return null;
  let start = pos - 1;
  while (start >= 0 && /[a-zA-Z']/.test(text[start])) start--;
  start++;
  let end = pos;
  while (end < text.length && /[a-zA-Z']/.test(text[end])) end++;
  if (end <= start) return null;
  return text.slice(start, end);
}

// ── Update statistics ──────────────────────────────────────────────────────
function updateStats() {
  statWords.textContent = state.totalWords;
  statCorrections.textContent = state.totalCorrections;
  const pct = state.totalWords > 0
    ? Math.max(0, Math.round((1 - state.totalCorrections / Math.max(state.totalWords, 1)) * 100))
    : 100;
  statAccuracy.textContent = pct + '%';
}

// ── Toast helper ──────────────────────────────────────────────────────────
let toastTimeout;
function showToast(msg) {
  toastEl.textContent = msg;
  toastEl.classList.add('show');
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toastEl.classList.remove('show'), 2500);
}

// ── Clear button ──────────────────────────────────────────────────────────
clearBtn.addEventListener('click', () => {
  inputEl.value = '';
  highlightEl.innerHTML = '';
  suggestionsEl.innerHTML = '<span class="no-suggestions">Start typing to get suggestions…</span>';
  charCountEl.textContent = '0 / 2000';
  state.totalWords = 0;
  state.totalCorrections = 0;
  updateStats();
  inputEl.focus();
});

// ── Copy button ──────────────────────────────────────────────────────────
copyBtn.addEventListener('click', async () => {
  const text = inputEl.value;
  if (!text) { showToast('Nothing to copy'); return; }
  try {
    await navigator.clipboard.writeText(text);
    showToast('✓ Text copied to clipboard');
  } catch {
    // Fallback
    inputEl.select();
    document.execCommand('copy');
    showToast('✓ Text copied');
  }
});

// ── Input event with debounce ─────────────────────────────────────────────
inputEl.addEventListener('input', () => {
  clearTimeout(state.debounceTimer);
  state.debounceTimer = setTimeout(processInput, 120);
});

inputEl.addEventListener('keyup', (e) => {
  // On cursor move, refresh suggestions immediately
  if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) {
    processInput();
  }
});

// Enforce max length
inputEl.addEventListener('keydown', (e) => {
  if (inputEl.value.length >= 2000 && e.key.length === 1 && !e.ctrlKey && !e.metaKey) {
    e.preventDefault();
    showToast('Character limit reached (2000)');
  }
});

// ── Load dictionary from words.json ───────────────────────────────────────
async function loadDictionary() {
  try {
    const response = await fetch('words.json');
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();

    const words = data.words || data;
    // Deduplicate and sort for binary search
    const unique = [...new Set(words.map(w => w.toLowerCase().trim()))].filter(w => w.length > 0);
    unique.sort();
    state.dictionary = unique;

    // Simple frequency weighting: longer/less common = lower weight
    state.wordFrequency = {};
    unique.forEach(w => {
      state.wordFrequency[w] = Math.max(1, 10 - w.length);
    });

    statDict.textContent = unique.length.toLocaleString();
    document.getElementById('statusText').textContent = 'AI Ready';

    // Initial example
    if (!inputEl.value) {
      inputEl.value = 'Teh quik brwon fox jmps over the lzy dog';
      processInput();
    }
  } catch (err) {
    console.error('Failed to load dictionary:', err);
    statDict.textContent = 'N/A';
    document.getElementById('statusText').textContent = 'Offline';
    showToast('⚠ Dictionary load failed – offline mode');

    // Fallback mini-dictionary
    const fallback = ['the', 'quick', 'brown', 'fox', 'jumps', 'over', 'lazy', 'dog',
      'hello', 'world', 'typing', 'autocorrect', 'keyboard', 'smart', 'correct',
      'text', 'word', 'error', 'spell', 'check'];
    fallback.sort();
    state.dictionary = fallback;
    statDict.textContent = fallback.length;
    document.getElementById('statusText').textContent = 'Fallback';
  }
}

// ── PWA service worker registration ──────────────────────────────────────
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('sw.js').catch(() => {
      // SW registration is optional; fail silently
    });
  });
}

// ── Bootstrap ────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', loadDictionary);
