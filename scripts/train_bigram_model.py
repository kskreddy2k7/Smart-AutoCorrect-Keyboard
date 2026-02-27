#!/usr/bin/env python3
"""
Bigram model training script for Smart AutoCorrect Keyboard.

Reads a text corpus, generates bigram frequency counts,
and outputs a JSON file suitable for loading into the Android app.

Usage:
    python train_bigram_model.py --input corpus.txt --output bigrams.json
    python train_bigram_model.py --demo  # Generate from built-in sample corpus
"""

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path


def tokenize(text: str) -> list[str]:
    """Tokenize text into lowercase words, removing punctuation."""
    text = text.lower()
    words = re.findall(r'\b[a-z]+\b', text)
    return words


def build_bigrams(tokens: list[str]) -> dict[str, dict[str, int]]:
    """
    Build bigram frequency counts from a token list.

    Returns:
        A dict mapping each word to a dict of {next_word: count}.
    """
    bigrams: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for i in range(len(tokens) - 1):
        prev = tokens[i]
        nxt = tokens[i + 1]
        bigrams[prev][nxt] += 1
    return {k: dict(v) for k, v in bigrams.items()}


def prune_bigrams(
    bigrams: dict[str, dict[str, int]],
    min_count: int = 2,
    top_n: int = 10
) -> dict[str, dict[str, int]]:
    """
    Prune bigrams to reduce model size:
    - Remove pairs with fewer than min_count occurrences.
    - Keep only top_n predictions per word.
    """
    pruned = {}
    for prev, nexts in bigrams.items():
        filtered = {w: c for w, c in nexts.items() if c >= min_count}
        if filtered:
            top = sorted(filtered.items(), key=lambda x: -x[1])[:top_n]
            pruned[prev] = dict(top)
    return pruned


SAMPLE_CORPUS = """
the quick brown fox jumps over the lazy dog
the cat sat on the mat and the dog ran away
i love to type on my phone every day
hello world this is a test of the keyboard
the android keyboard helps you type faster
please send me a message when you get home
good morning how are you doing today
i need to buy some food from the store
the weather today is very nice and sunny
thank you for your help with this problem
"""


def main():
    parser = argparse.ArgumentParser(description="Train bigram model for keyboard prediction")
    parser.add_argument("--input", type=str, help="Path to input text corpus file")
    parser.add_argument("--output", type=str, default="bigrams.json", help="Output JSON file path")
    parser.add_argument("--min-count", type=int, default=2, help="Minimum bigram count to include")
    parser.add_argument("--top-n", type=int, default=10, help="Max predictions per word")
    parser.add_argument("--demo", action="store_true", help="Use built-in sample corpus")
    args = parser.parse_args()

    # Load corpus
    if args.demo or args.input is None:
        print("Using sample corpus (demo mode)...")
        text = SAMPLE_CORPUS
    else:
        input_path = Path(args.input)
        if not input_path.exists():
            print(f"Error: Input file '{args.input}' not found.")
            return
        text = input_path.read_text(encoding="utf-8")

    print("Tokenizing...")
    tokens = tokenize(text)
    print(f"Total tokens: {len(tokens)}")

    print("Building bigrams...")
    bigrams = build_bigrams(tokens)
    print(f"Unique bigram pairs before pruning: {sum(len(v) for v in bigrams.values())}")

    print(f"Pruning (min_count={args.min_count}, top_n={args.top_n})...")
    pruned = prune_bigrams(bigrams, min_count=args.min_count, top_n=args.top_n)
    print(f"Unique bigram pairs after pruning: {sum(len(v) for v in pruned.values())}")

    output_path = Path(args.output)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(pruned, f, indent=2, ensure_ascii=False)
    print(f"Saved bigrams to: {output_path}")

    # Show sample predictions
    print("\nSample predictions:")
    for word in ["the", "i", "hello", "good"]:
        if word in pruned:
            top3 = list(pruned[word].items())[:3]
            print(f"  '{word}' -> {top3}")


if __name__ == "__main__":
    main()
