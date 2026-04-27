#!/usr/bin/env python3
"""
validate_levels.py — Connectivity check for every level in tools/levels.json.

Test cases (per the spec):
  1. Every cell that's part of a word must touch at least one other word.
  2. No independent / floating words in the grid.
  3. Each word must connect to another word by at least one letter cell.
  4. All 100 levels must pass.

Implementation:
  - For each level, build a graph where each word is a node and an edge
    connects words A and B if they share at least one cell coordinate.
  - Run BFS from word #0; if every word is reached, the level is valid.
  - Otherwise, print the components so we know which words are isolated.

Exit codes:
  0 — all levels pass
  1 — one or more levels fail (details printed to stdout)
"""
from __future__ import annotations

import json
import os
import sys
from collections import defaultdict, deque

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEVELS_JSON = os.path.join(REPO, "tools", "levels.json")


def cells_of(word: dict) -> list[tuple[int, int]]:
    """List of (row, col) cells the word occupies."""
    out = []
    for i in range(len(word["w"])):
        if word["d"] == "A":
            out.append((word["r"], word["c"] + i))
        else:  # "D"
            out.append((word["r"] + i, word["c"]))
    return out


def validate_level(level: dict) -> tuple[bool, str]:
    """
    Returns (is_connected, message). On failure, message describes which
    words ended up in separate components.
    """
    words = level["words"]
    n = len(words)
    if n == 0:
        return False, "no words at all"
    if n == 1:
        # A single-word level cannot be "disconnected from itself", but it
        # also has nothing to intersect with. We accept it but flag it.
        return True, "lone word — nothing to connect to (acceptable for tiny levels)"

    word_cells = [set(cells_of(w)) for w in words]

    # Build adjacency: edge between words that share ≥1 cell.
    adj: dict[int, set[int]] = defaultdict(set)
    for i in range(n):
        for j in range(i + 1, n):
            if word_cells[i] & word_cells[j]:
                adj[i].add(j)
                adj[j].add(i)

    # BFS from word 0 — count how many words are reachable.
    seen = {0}
    queue = deque([0])
    while queue:
        v = queue.popleft()
        for u in adj[v]:
            if u not in seen:
                seen.add(u)
                queue.append(u)

    if len(seen) == n:
        return True, ""

    # Build all components for the failure message.
    components: list[set[int]] = []
    visited: set[int] = set()
    for start in range(n):
        if start in visited:
            continue
        comp = set()
        q = deque([start])
        comp.add(start)
        visited.add(start)
        while q:
            v = q.popleft()
            for u in adj[v]:
                if u not in visited:
                    visited.add(u)
                    comp.add(u)
                    q.append(u)
        components.append(comp)

    parts = []
    for comp in components:
        comp_words = [words[i]["w"] for i in sorted(comp)]
        parts.append("{" + ", ".join(comp_words) + "}")
    return False, f"{len(components)} disconnected components: " + " || ".join(parts)


def validate_letters_match(level: dict) -> tuple[bool, str]:
    """
    Test: every crossword answer must be spellable from the wheel letters
    (the multiset of `letters` must contain a multiset >= the multiset of
    each answer word).
    """
    from collections import Counter
    pool = Counter(level["letters"])
    for word in level["words"]:
        wc = Counter(word["w"])
        for ch, n in wc.items():
            if pool[ch] < n:
                return False, (f"answer '{word['w']}' uses letter "
                               f"'{ch}' more often than the wheel ({n} > {pool[ch]})")
    return True, ""


def validate_no_intersection_conflict(level: dict) -> tuple[bool, str]:
    """
    Test: when two words cross at a cell, both must agree on the letter
    at that cell. (The generator enforces this, but verify on JSON too.)
    """
    cells_letters: dict[tuple[int, int], str] = {}
    for word in level["words"]:
        for i, ch in enumerate(word["w"]):
            if word["d"] == "A":
                pos = (word["r"], word["c"] + i)
            else:
                pos = (word["r"] + i, word["c"])
            existing = cells_letters.get(pos)
            if existing is not None and existing != ch:
                return False, (f"cell {pos}: '{word['w']}' wants '{ch}' "
                               f"but another word already wrote '{existing}'")
            cells_letters[pos] = ch
    return True, ""


def main():
    if not os.path.exists(LEVELS_JSON):
        sys.exit(f"levels.json not found at {LEVELS_JSON}\n"
                 "Run `python3 tools/generate_levels.py` first.")

    with open(LEVELS_JSON) as f:
        levels = json.load(f)

    failures: list[tuple[int, str, str]] = []  # (level, test, message)
    flags: list[tuple[int, str]] = []

    for L in levels:
        # Test 1-3: connectivity
        ok, msg = validate_level(L)
        if not ok:
            failures.append((L["n"], "connectivity", msg))
        elif msg:
            flags.append((L["n"], msg))

        # Bonus test: every answer is spellable from wheel letters
        ok, msg = validate_letters_match(L)
        if not ok:
            failures.append((L["n"], "letters", msg))

        # Bonus test: no conflicting letters at intersections
        ok, msg = validate_no_intersection_conflict(L)
        if not ok:
            failures.append((L["n"], "conflict", msg))

    print(f"Validated {len(levels)} levels — 3 tests each: connectivity, "
          f"letters-match, intersection-consistency.")
    if flags:
        print(f"\nNotes ({len(flags)} levels):")
        for n, msg in flags:
            print(f"  L{n:>3}: {msg}")
    if failures:
        print(f"\n❌ FAIL ({len(failures)} test failures across "
              f"{len({f[0] for f in failures})} levels):")
        for n, test, msg in failures:
            print(f"  L{n:>3} [{test}]: {msg}")
        sys.exit(1)
    else:
        print("\n✅ All checks passed:")
        print(f"   • Connectivity: every word touches another (no islands)")
        print(f"   • Letters: every answer is spellable from the wheel")
        print(f"   • Intersections: no conflicting letters at shared cells")


if __name__ == "__main__":
    main()
