#!/usr/bin/env python3
"""
generate_levels.py — Procedurally generate 100 word puzzle levels.

Approach: spine + branches. Each level picks a root word of progressive
length (4 → 9 letters). The root letters become the wheel's tiles. A
subset of valid sub-words form the crossword answers, placed via a
"spine + branches" greedy algorithm:

  1. Pick the longest sub-word as the across "spine"
  2. For each remaining candidate, try every (spine_letter, word_letter)
     intersection and keep the first non-conflicting placement
  3. Bonus words = remaining valid sub-words not placed in the grid

Existing levels 1–10 are preserved exactly. Levels 11–100 are generated.

Outputs:
  - android-native/.../Level.kt  (regenerated)
  - ios-native/.../Models/Level.swift  (regenerated)
  - tools/levels.json  (intermediate, for inspection)

Usage:
  python3 tools/generate_levels.py
"""
from __future__ import annotations

import json
import os
import random
import sys
from collections import Counter
from dataclasses import dataclass, field
from typing import Iterable, Optional

# Deterministic generation so the same input produces the same levels.
SEED = 4242
random.seed(SEED)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORDLIST_PATH = "/usr/share/dict/words"
# Common-English words (~10k) from google-10000-english. Filtering the BSD
# dictionary down to this set kills the long tail of obscure terms (NIOG,
# MORIN, REUNFOLD…) that the linguist's dictionary includes but no casual
# player would ever guess. We still need /usr/share/dict/words on top to
# get plurals/conjugations the common list omits.
COMMON_WORDS_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                 "common-10k.txt")

# Levels 1-10 are kept verbatim (already polished + tested).
# Format: (rows, cols, letters, [(word, row, col, dir), ...])
HAND_CRAFTED: dict[int, dict] = {
    1: dict(rows=5, cols=5, letters="CATS",
            words=[("CAST", 1, 0, "A"), ("CAT", 1, 0, "D"), ("SAT", 1, 2, "D")]),
    2: dict(rows=6, cols=6, letters="SPINE",
            words=[("SPINE", 1, 0, "A"), ("SIN", 1, 0, "D"),
                   ("NIP", 1, 3, "D"), ("PEN", 3, 3, "A")]),
    3: dict(rows=7, cols=7, letters="HASTE",
            words=[("HASTE", 1, 0, "A"), ("HATE", 1, 0, "D"),
                   ("SEAT", 1, 2, "D"), ("EAT", 4, 0, "A")]),
    4: dict(rows=7, cols=7, letters="WARMS",
            words=[("SWARM", 1, 0, "A"), ("SAW", 1, 0, "D"),
                   ("RAM", 1, 3, "D"), ("WARM", 3, 0, "A")]),
    5: dict(rows=7, cols=7, letters="CARES",
            words=[("CARES", 1, 0, "A"), ("CARE", 1, 0, "D"),
                   ("SEAR", 1, 4, "D"), ("ERA", 4, 0, "A")]),
    6: dict(rows=7, cols=7, letters="GRINDS",
            words=[("GRINS", 1, 0, "A"), ("GIN", 1, 0, "D"),
                   ("SIR", 1, 4, "D"), ("RID", 3, 4, "A"), ("DIG", 3, 6, "D")]),
    7: dict(rows=8, cols=8, letters="PLANETS",
            words=[("PLANETS", 1, 0, "A"), ("PETAL", 1, 0, "D"),
                   ("NETS", 1, 3, "D"), ("SLANT", 1, 6, "D"),
                   ("ANTS", 4, 0, "A")]),
    8: dict(rows=8, cols=8, letters="CRANESD",
            words=[("CRANES", 1, 0, "A"), ("CARED", 1, 0, "D"),
                   ("NEARS", 1, 3, "D"), ("SANE", 1, 5, "D"),
                   ("DENS", 5, 0, "A"), ("END", 5, 1, "D")]),
    9: dict(rows=8, cols=8, letters="STORED",
            words=[("STORED", 1, 0, "A"), ("SORT", 1, 0, "D"),
                   ("REST", 1, 3, "D"), ("DOSE", 1, 5, "D"),
                   ("ROES", 3, 0, "A")]),
    10: dict(rows=9, cols=9, letters="TRAINSE",
             words=[("TRAINS", 1, 0, "A"), ("TEARS", 1, 0, "D"),
                    ("INSET", 1, 3, "D"), ("SIREN", 1, 5, "D"),
                    ("ANTS", 3, 0, "A"), ("SENT", 5, 0, "A")]),
}

# Pre-existing bonus words for hand-crafted levels — keep verbatim.
HAND_CRAFTED_BONUS: dict[int, list[str]] = {
    1: ["ACT", "ACTS", "SCAT", "CATS"],
    2: ["PIN", "PIE", "SIP", "SNIP", "PIES", "PENS", "SINE", "PINE"],
    3: ["HEAT", "SET", "HAT", "TEA", "ATE", "ETA", "EAST", "EATS"],
    4: ["WARMS", "WARS", "ARMS", "MARS", "ARM", "MAR", "RAW", "WAR"],
    5: ["RACE", "CARS", "EARS", "ACE", "ARC", "SCAR", "ARCS", "ACRE", "ACES"],
    6: ["GRINDS", "GRID", "RIND", "DING", "RING", "GRIN", "RINGS"],
    7: ["PLAN", "PLANT", "LEAN", "PLATE", "STEAL", "LANE", "TALE", "PELT"],
    8: ["DANCE", "SCARE", "RACED", "CEDAR", "CANE", "ACRE", "RAN"],
    9: ["RODE", "DOER", "STORE", "DOTES", "TROD", "ODES", "SORE", "TORE"],
    10: ["STARE", "RETAIN", "SATIRE", "INSERT", "STAIN", "RAIN", "RISE", "TIRE"],
}

# Words to skip even if they're in the common wordlist — proper nouns,
# foreign-language tokens, and obscure abbreviations that the dictionary
# lists as words but no English-speaking player would solve.
PROPER_NOUNS_DENYLIST: set[str] = {
    # Common English first names
    "ALAN", "ERIC", "PETER", "BRAD", "RUTH", "RICK", "PAUL", "MARK", "JOHN",
    "ANN", "AMY", "SETH", "NATE", "MARY", "JANE", "ROSS", "MIKE", "TOM",
    "JIM", "JOE", "TED", "DAN", "RON", "BOB", "SAM", "MEG", "MOE", "COLIN",
    "COLE",
    # Place names
    "TROY", "BALI", "NOVA", "RIO", "ASIA", "OHIO", "PARIS", "INDIA", "JUNE",
    "EVEREST",
    # Foreign words / cultural references
    "MING", "SRI", "SEN", "SIE", "YEN", "MEM", "KAT", "RAS",
    # Abbreviations / initialisms
    "ATIS", "IST", "NIN", "GED", "GEN", "ONS", "TRI", "ING", "ENS",
    "ELS", "IDE", "AES", "SAO", "PAC", "REG", "REV", "AVE", "GIED",
    # Other obscurities
    "ARED", "ROED", "NOWED", "FLUED", "ATES", "HURTED", "CARK", "GARCE",
    "LAKING", "WALING", "LAWING", "GARE", "AGRE", "GIE", "MORG", "MONG",
    "INRO", "NIOG", "MORIN", "NORI", "JAW", "GIE",
}

# Hand-picked root words for levels 11-100. Common English words, ordered
# by length so difficulty grows. Each must contain enough vowels to make
# sub-word generation viable. Curated to feel "puzzle-worthy".
ROOTS_11_100: list[str] = [
    # Length 5 (levels 11-30) — 20 words
    "GRACE", "DREAM", "BREAD", "FLAME", "SOLID", "STONE", "WATER", "OCEAN",
    "PEACE", "SPACE", "HEART", "PLATE", "TRACK", "SCALE", "PRINT", "CRAFT",
    "SMILE", "FROST", "BRAVE", "QUIET",
    # Length 6 (levels 31-55) — 25 words
    "PLATES", "STREAM", "BRIGHT", "FOREST", "GARDEN", "CASTLE", "ISLAND",
    "BRIDGE", "DESERT", "PLANET", "WINTER", "SPRING", "SUMMER", "MOTHER",
    "FATHER", "TEACHER", "DOCTOR", "WONDER", "MARKET", "BUTTER",
    "COTTAGE", "TENDER", "MASTER", "POSTER", "TRAVEL",
    # Length 7 (levels 56-80) — 25 words
    "MORNING", "EVENING", "PICTURE", "WEATHER", "JOURNEY", "MEETING",
    "READING", "WALKING", "FOREVER", "FREEDOM", "STATION", "VICTORY",
    "MYSTERY", "HISTORY", "FACTORY", "GALLERY", "STUDENT", "TEACHER",
    "MANAGER", "ARTISTS", "PARTNER", "MONSTER", "PALACES", "TROPHY",
    "STORIES",
    # Length 8 (levels 81-95) — 15 words
    "MOUNTAIN", "OBSERVER", "BIRTHDAY", "ATTITUDE", "PARENTAL",
    "STRENGTH", "TREASURE", "STARTERS", "ELECTION", "MAGAZINE",
    "INTERNET", "EVERYDAY", "DAUGHTER", "SOUTHERN", "DESIGNER",
    # Length 9 (levels 96-100) — 5 words
    "ADVENTURE", "STATEMENT", "BRIGHTEST", "EVERGREEN", "WONDERFUL",
]
# Sanity: must be exactly 90 (levels 11-100)
assert len(ROOTS_11_100) == 90, f"need 90 root words, got {len(ROOTS_11_100)}"


# ─── Wordlist ──────────────────────────────────────────────────────────

def _load_bsd() -> set[str]:
    """All-lowercase, alphabetic, length 3-10 entries from /usr/share/dict/words."""
    bsd: set[str] = set()
    with open(WORDLIST_PATH) as f:
        for raw in f:
            w = raw.strip()
            if not w or not w.isalpha() or not w.isascii():
                continue
            if not (3 <= len(w) <= 10):
                continue
            # Skip Capital-first entries (proper nouns). Keep ALLCAPS too
            # (acronyms — filtered later by denylist).
            if w[0].isupper() and not w.isupper():
                continue
            bsd.add(w.upper())
    return bsd


def _load_common_with_morphology() -> set[str]:
    """google-10000-english + simple plural / -ed / -ing variants."""
    if not os.path.exists(COMMON_WORDS_PATH):
        sys.exit(f"Common-words list not found at {COMMON_WORDS_PATH}\n"
                 "Run: curl -sS -L https://raw.githubusercontent.com/first20hours/"
                 "google-10000-english/master/google-10000-english-no-swears.txt "
                 f"-o {COMMON_WORDS_PATH}")
    common_raw: set[str] = set()
    with open(COMMON_WORDS_PATH) as f:
        for raw in f:
            w = raw.strip().upper()
            if w and w.isalpha() and 2 <= len(w) <= 10:
                common_raw.add(w)

    common: set[str] = set(common_raw)
    for w in list(common_raw):
        if 2 <= len(w) <= 9:
            common.add(w + "S")
            if w.endswith("E"):
                common.add(w + "D")
                common.add(w[:-1] + "ING")
            elif w.endswith(("Y",)):
                common.add(w[:-1] + "IES")
                common.add(w[:-1] + "IED")
            else:
                common.add(w + "ED")
                common.add(w + "ING")
    return common


def load_wordlist() -> tuple[set[str], set[str]]:
    """
    Returns (puzzle_words, bonus_words):

    - puzzle_words: strict — used for crossword ANSWERS placed in the
      grid. BSD ∩ common-10k (with morphology), minus the denylist.
    - bonus_words: permissive — used to seed the BONUS list shown to
      the player. BSD itself, minus only the proper-noun denylist.
      Players rarely "win" by submitting bonus words, so we cast a
      wider net here so common English words like LEAP, NAPE, NEAP,
      ETAS, all count.
    """
    if not os.path.exists(WORDLIST_PATH):
        sys.exit(f"Wordlist not found at {WORDLIST_PATH}")

    bsd = _load_bsd()
    common = _load_common_with_morphology()

    puzzle = (bsd & common) - PROPER_NOUNS_DENYLIST
    bonus = bsd - PROPER_NOUNS_DENYLIST
    return puzzle, bonus


# ─── Sub-word search ────────────────────────────────────────────────────

def subwords(root: str, dictionary: set[str], min_len: int = 3) -> list[str]:
    """All dictionary words whose multiset of letters is a subset of root's."""
    root_counter = Counter(root)
    out = []
    for w in dictionary:
        if len(w) < min_len or len(w) > len(root):
            continue
        wc = Counter(w)
        if all(wc[ch] <= root_counter[ch] for ch in wc):
            out.append(w)
    return out


# ─── Crossword placement ────────────────────────────────────────────────

@dataclass
class PlacedWord:
    word: str
    row: int
    col: int
    direction: str  # "A" or "D"

    def cells(self) -> list[tuple[int, int, str]]:
        """List of (row, col, letter) cells the word occupies."""
        out = []
        for i, ch in enumerate(self.word):
            r = self.row + (i if self.direction == "D" else 0)
            c = self.col + (i if self.direction == "A" else 0)
            out.append((r, c, ch))
        return out


def can_place(pw: PlacedWord, occupied: dict[tuple[int, int], str],
              max_size: int) -> bool:
    """A placement is OK if:
       - cells are inside [0, max_size)
       - any pre-occupied cell on the path matches the new word's letter
       - the perpendicular line before/after the word is empty
       - we don't accidentally form a longer word adjacent to the placement
    """
    cells = pw.cells()

    # Bounds
    for r, c, _ in cells:
        if not (0 <= r < max_size and 0 <= c < max_size):
            return False

    intersected = False
    for r, c, ch in cells:
        if (r, c) in occupied:
            if occupied[(r, c)] != ch:
                return False
            intersected = True

    # Must intersect at least one already-placed letter (after the spine).
    if occupied and not intersected:
        return False

    # No cell adjacent (perpendicular) to a non-intersection cell may be
    # filled — that would create stray letter-pairs the player would expect
    # to be valid words too.
    for r, c, ch in cells:
        if (r, c) in occupied:
            continue  # this cell is an intersection, neighbours expected
        if pw.direction == "A":
            for dr in (-1, 1):
                if (r + dr, c) in occupied:
                    return False
        else:  # D
            for dc in (-1, 1):
                if (r, c + dc) in occupied:
                    return False

    # The cell immediately before and after the word in its own direction
    # must also be empty — otherwise we extend an existing word.
    if pw.direction == "A":
        before = (pw.row, pw.col - 1)
        after = (pw.row, pw.col + len(pw.word))
    else:
        before = (pw.row - 1, pw.col)
        after = (pw.row + len(pw.word), pw.col)
    if before in occupied or after in occupied:
        return False

    return True


def build_level(root: str, puzzle_words: set[str], bonus_dict: set[str],
                max_words: int, rng: random.Random) -> Optional[dict]:
    """
    Build one level for the given root word.

    [puzzle_words] is the strict common wordlist used to choose crossword
    answers. [bonus_dict] is the permissive English wordlist used to
    seed the bonus list — every valid sub-word that isn't already a
    crossword answer counts as a bonus word the player can earn coins
    for, even if it's not displayed.

    Returns dict with rows, cols, letters, words[], bonus[]. Returns
    None if too few crossword words could be placed.
    """
    candidates = sorted(
        subwords(root, puzzle_words, min_len=3),
        key=lambda w: (-len(w), w),
    )
    # Long candidates make better spines; among same length, shuffle.
    by_len: dict[int, list[str]] = {}
    for w in candidates:
        by_len.setdefault(len(w), []).append(w)
    for ws in by_len.values():
        rng.shuffle(ws)
    candidates = [w for L in sorted(by_len, reverse=True) for w in by_len[L]]

    if not candidates:
        return None

    # Spine: first long candidate (avoid using the root itself as the
    # answer — too easy. But for shorter levels, allow it as fallback).
    spine_candidates = [w for w in candidates if w != root and len(w) >= max(4, len(root) - 2)]
    if not spine_candidates:
        spine_candidates = candidates
    spine_word = spine_candidates[0]

    # Use grid 1.5× spine length so we have room to grow downwards.
    grid_size = max(len(spine_word) + 2, 6)

    spine = PlacedWord(spine_word, row=1, col=0, direction="A")
    occupied: dict[tuple[int, int], str] = {(r, c): ch for r, c, ch in spine.cells()}

    placed: list[PlacedWord] = [spine]
    used_words = {spine_word}

    # Greedy branch placement.
    for cand in candidates:
        if len(placed) >= max_words:
            break
        if cand in used_words:
            continue

        placed_this = False
        # Try every (spine letter, word letter) intersection.
        for spine_idx, spine_ch in enumerate(spine.word):
            if placed_this:
                break
            for word_idx, word_ch in enumerate(cand):
                if word_ch != spine_ch:
                    continue
                # Spine is across, branches are down.
                row = spine.row - word_idx
                col = spine.col + spine_idx
                trial = PlacedWord(cand, row=row, col=col, direction="D")
                if can_place(trial, occupied, grid_size):
                    placed.append(trial)
                    used_words.add(cand)
                    for r, c, ch in trial.cells():
                        occupied[(r, c)] = ch
                    placed_this = True
                    break

        # Try intersections with existing branches too (longer levels)
        if not placed_this and len(placed) > 1:
            for parent in placed[1:]:
                if placed_this:
                    break
                for parent_idx, parent_ch in enumerate(parent.word):
                    if placed_this:
                        break
                    for word_idx, word_ch in enumerate(cand):
                        if word_ch != parent_ch:
                            continue
                        # Place perpendicular to the branch (so across).
                        if parent.direction == "D":
                            row = parent.row + parent_idx
                            col = parent.col - word_idx
                            trial = PlacedWord(cand, row=row, col=col, direction="A")
                        else:
                            continue
                        if can_place(trial, occupied, grid_size):
                            placed.append(trial)
                            used_words.add(cand)
                            for r, c, ch in trial.cells():
                                occupied[(r, c)] = ch
                            placed_this = True
                            break

    if len(placed) < 2:
        return None

    # Normalise: shift everything so the bounding box starts at row=1, col=0.
    min_r = min(r for r, _ in occupied.keys())
    min_c = min(c for _, c in occupied.keys())
    dr = 1 - min_r
    dc = 0 - min_c
    for pw in placed:
        pw.row += dr
        pw.col += dc

    max_r = max(r for r, _ in occupied.keys()) + dr
    max_c = max(c for _, c in occupied.keys()) + dc
    rows = max_r + 2
    cols = max_c + 1

    # Bonus words: every valid sub-word from the permissive English list
    # that isn't already a crossword answer.
    #
    # We deliberately uncap this — players still see a short hint list in
    # the UI but the game now ACCEPTS any of these as a valid bonus
    # submission. Was previously [:8] which made the game reject obviously
    # valid words like LEAP / NAPE / PEAL on level 7 (PLANETS).
    bonus_subs = subwords(root, bonus_dict, min_len=3)
    # Drop the root word itself + any crossword answer + the spine.
    bonus = sorted(
        (w for w in bonus_subs
         if w != root and w not in used_words),
        key=lambda w: (-len(w), w),
    )

    return dict(
        rows=rows,
        cols=cols,
        letters=root,
        words=[(p.word, p.row, p.col, p.direction) for p in placed],
        bonus=bonus,
    )


# ─── Generation pipeline ────────────────────────────────────────────────

def difficulty_max_words(level_num: int) -> int:
    """How many crossword words to aim for, growing with level number."""
    if level_num <= 20:
        return 3
    if level_num <= 40:
        return 4
    if level_num <= 60:
        return 5
    if level_num <= 80:
        return 6
    return 7  # 81-100


def generate_all() -> list[dict]:
    """Build all 100 level dicts. Hand-crafted 1-10, generated 11-100."""
    print("Loading wordlists…")
    puzzle_words, bonus_dict = load_wordlist()
    print(f"  puzzle wordlist (strict): {len(puzzle_words):,}")
    print(f"  bonus wordlist (permissive): {len(bonus_dict):,}")

    levels: list[dict] = []

    # ── Hand-crafted levels 1-10 ────────────────────────────────────
    # Augment the hand-curated bonus lists with every other valid
    # sub-word of the wheel letters that the BSD dictionary considers
    # English. Words show up as bonus rewards when typed during play
    # — we keep the original curated list at the front (UI hint) and
    # append the rest so submission still credits them.
    for n in range(1, 11):
        h = HAND_CRAFTED[n]
        root = h["letters"]
        crossword_answers = {pw[0] for pw in h["words"]}
        all_bonus = subwords(root, bonus_dict, min_len=3)
        curated = HAND_CRAFTED_BONUS[n]
        merged: list[str] = list(curated)
        seen = set(curated) | crossword_answers | {root}
        for w in sorted(all_bonus, key=lambda w: (-len(w), w)):
            if w not in seen:
                merged.append(w)
                seen.add(w)
        levels.append(dict(
            number=n,
            rows=h["rows"],
            cols=h["cols"],
            letters=root,
            words=h["words"],
            bonus=merged,
        ))

    # ── Generated levels 11-100 ─────────────────────────────────────
    rng = random.Random(SEED)
    for i, root in enumerate(ROOTS_11_100):
        n = i + 11
        max_w = difficulty_max_words(n)
        built = build_level(root, puzzle_words, bonus_dict, max_w, rng)
        if built is None:
            sys.exit(f"Failed to build level {n} for root {root}")
        built["number"] = n
        levels.append(built)
        print(f"  L{n:>3}: root={root:<10} grid={built['rows']}x{built['cols']:<2} "
              f"words={len(built['words'])} bonus={len(built['bonus'])}")

    return levels


# ─── Code emitters ──────────────────────────────────────────────────────

def emit_kotlin(levels: list[dict]) -> str:
    out = []
    out.append("package com.wordwheel.game\n")
    out.append("// AUTO-GENERATED by tools/generate_levels.py — do not edit by hand.\n")
    out.append("// Re-run the generator to update levels.\n\n")
    out.append("enum class Dir { ACROSS, DOWN }\n\n")
    out.append("data class PlacedWord(\n")
    out.append("    val word: String,\n    val row: Int,\n    val col: Int,\n    val dir: Dir,\n)\n\n")
    out.append("data class Level(\n")
    out.append("    val rows: Int,\n    val cols: Int,\n    val letters: List<Char>,\n    val words: List<PlacedWord>,\n) {\n")
    out.append("    fun answersSet(): Set<String> = words.map { it.word }.toSet()\n\n")
    out.append("    fun usedCells(): Set<Pair<Int, Int>> {\n")
    out.append("        val used = mutableSetOf<Pair<Int, Int>>()\n")
    out.append("        for (pw in words) {\n")
    out.append("            for (i in pw.word.indices) {\n")
    out.append("                val (r, c) = when (pw.dir) {\n")
    out.append("                    Dir.ACROSS -> pw.row to (pw.col + i)\n")
    out.append("                    Dir.DOWN -> (pw.row + i) to pw.col\n")
    out.append("                }\n                used.add(r to c)\n            }\n        }\n")
    out.append("        return used\n    }\n\n")
    out.append("    fun solutionLetterAt(row: Int, col: Int): Char? {\n")
    out.append("        for (pw in words) {\n")
    out.append("            for ((i, ch) in pw.word.withIndex()) {\n")
    out.append("                val (r, c) = when (pw.dir) {\n")
    out.append("                    Dir.ACROSS -> pw.row to (pw.col + i)\n")
    out.append("                    Dir.DOWN -> (pw.row + i) to pw.col\n")
    out.append("                }\n                if (r == row && c == col) return ch\n")
    out.append("            }\n        }\n        return null\n    }\n\n")
    out.append("    fun bonusWords(): List<String> = LEVEL_BONUS[letters.joinToString(\"\")] ?: emptyList()\n\n")
    out.append("    companion object {\n")
    out.append(f"        const val TOTAL_LEVELS = {len(levels)}\n\n")
    out.append("        fun get(levelNum: Int): Level {\n")
    out.append("            val idx = (levelNum - 1).coerceIn(0, LEVELS.size - 1)\n")
    out.append("            return LEVELS[idx]\n        }\n    }\n}\n\n")

    # Levels array
    out.append("private val LEVELS: List<Level> = listOf(\n")
    for L in levels:
        words_str = ", ".join(
            f'PlacedWord("{w}", {r}, {c}, Dir.{("ACROSS" if d == "A" else "DOWN")})'
            for (w, r, c, d) in L["words"]
        )
        letters_chars = ", ".join(f"'{ch}'" for ch in L["letters"])
        out.append(f"    Level(rows = {L['rows']}, cols = {L['cols']}, "
                   f"letters = listOf({letters_chars}),\n"
                   f"          words = listOf({words_str})),\n")
    out.append(")\n\n")

    # Bonus map — split into chunks because the bonus lists for high
    # levels can have 200+ entries; placing them all in one mapOf()
    # blows past Kotlin's 64 KB <clinit> bytecode limit. Each chunk is
    # its own private function returning a partial map; we merge them
    # at top level via buildMap.
    seen: set[str] = set()
    bonus_entries: list[tuple[str, list[str]]] = []
    for L in levels:
        key = L["letters"]
        if key in seen:
            continue
        seen.add(key)
        bonus_entries.append((key, L["bonus"]))

    out.append("private val LEVEL_BONUS: Map<String, List<String>> = buildMap {\n")
    chunk_size = 10
    n_chunks = (len(bonus_entries) + chunk_size - 1) // chunk_size
    for i in range(n_chunks):
        out.append(f"    putAll(bonusChunk{i + 1}())\n")
    out.append("}\n\n")
    for i in range(n_chunks):
        chunk = bonus_entries[i * chunk_size : (i + 1) * chunk_size]
        out.append(f"private fun bonusChunk{i + 1}(): Map<String, List<String>> = mapOf(\n")
        for key, words in chunk:
            bonus_str = ", ".join(f'"{w}"' for w in words)
            out.append(f'    "{key}" to listOf({bonus_str}),\n')
        out.append(")\n\n")

    return "".join(out)


def emit_swift(levels: list[dict]) -> str:
    out = []
    out.append("import Foundation\n\n")
    out.append("// AUTO-GENERATED by tools/generate_levels.py — do not edit by hand.\n")
    out.append("// Re-run the generator to update levels.\n\n")
    out.append("enum Dir {\n    case across\n    case down\n}\n\n")
    out.append("struct PlacedWord {\n    let word: String\n    let row: Int\n    let col: Int\n    let dir: Dir\n}\n\n")
    out.append("struct Cell: Hashable {\n    let row: Int\n    let col: Int\n}\n\n")
    out.append("struct Level {\n")
    out.append(f"    static let totalLevels = {len(levels)}\n\n")
    out.append("    let rows: Int\n    let cols: Int\n    let letters: [Character]\n    let words: [PlacedWord]\n\n")
    out.append("    var answersSet: Set<String> { Set(words.map { $0.word }) }\n\n")
    out.append("    var usedCells: Set<Cell> {\n        var used = Set<Cell>()\n")
    out.append("        for pw in words {\n            for i in 0..<pw.word.count {\n")
    out.append("                let cell: Cell\n                switch pw.dir {\n")
    out.append("                case .across: cell = Cell(row: pw.row, col: pw.col + i)\n")
    out.append("                case .down:   cell = Cell(row: pw.row + i, col: pw.col)\n")
    out.append("                }\n                used.insert(cell)\n            }\n        }\n        return used\n    }\n\n")
    out.append("    func solutionLetter(row: Int, col: Int) -> Character? {\n")
    out.append("        for pw in words {\n            for (i, ch) in pw.word.enumerated() {\n")
    out.append("                let r: Int, c: Int\n                switch pw.dir {\n")
    out.append("                case .across: r = pw.row; c = pw.col + i\n")
    out.append("                case .down:   r = pw.row + i; c = pw.col\n")
    out.append("                }\n                if r == row && c == col { return ch }\n            }\n        }\n        return nil\n    }\n\n")
    out.append("    var bonusWords: [String] { LEVEL_BONUS[String(letters)] ?? [] }\n\n")
    out.append("    static func get(_ levelNum: Int) -> Level {\n")
    out.append("        let idx = max(0, min(levelNum - 1, LEVELS.count - 1))\n")
    out.append("        return LEVELS[idx]\n    }\n}\n\n")

    # Levels array
    out.append("private let LEVELS: [Level] = [\n")
    for L in levels:
        words_str = ", ".join(
            f'PlacedWord(word: "{w}", row: {r}, col: {c}, dir: .{("across" if d == "A" else "down")})'
            for (w, r, c, d) in L["words"]
        )
        letters_chars = ", ".join(f'"{ch}"' for ch in L["letters"])
        out.append(f"    Level(rows: {L['rows']}, cols: {L['cols']}, "
                   f"letters: [{letters_chars}],\n"
                   f"          words: [{words_str}]),\n")
    out.append("]\n\n")

    # Bonus map
    out.append("private let LEVEL_BONUS: [String: [String]] = [\n")
    seen = set()
    for L in levels:
        key = L["letters"]
        if key in seen:
            continue
        seen.add(key)
        bonus_str = ", ".join(f'"{w}"' for w in L["bonus"])
        out.append(f'    "{key}": [{bonus_str}],\n')
    out.append("]\n")

    return "".join(out)


# ─── Main ───────────────────────────────────────────────────────────────

def main():
    levels = generate_all()

    # JSON dump for inspection
    dump = []
    for L in levels:
        dump.append({
            "n": L["number"],
            "rows": L["rows"],
            "cols": L["cols"],
            "letters": L["letters"],
            "words": [
                {"w": w, "r": r, "c": c, "d": d}
                for (w, r, c, d) in L["words"]
            ],
            "bonus": L["bonus"],
        })
    json_path = os.path.join(REPO, "tools", "levels.json")
    with open(json_path, "w") as f:
        json.dump(dump, f, indent=2)
    print(f"Wrote {json_path}")

    # Kotlin
    kt_path = os.path.join(
        REPO, "android-native", "app", "src", "main", "kotlin",
        "com", "wordwheel", "game", "Level.kt",
    )
    with open(kt_path, "w") as f:
        f.write(emit_kotlin(levels))
    print(f"Wrote {kt_path}")

    # Swift
    sw_path = os.path.join(
        REPO, "ios-native", "WordWheel", "Models", "Level.swift",
    )
    with open(sw_path, "w") as f:
        f.write(emit_swift(levels))
    print(f"Wrote {sw_path}")

    # Summary
    n_words = sum(len(L["words"]) for L in levels)
    n_bonus = sum(len(L["bonus"]) for L in levels)
    print(f"\nGenerated {len(levels)} levels, {n_words} crossword words, "
          f"{n_bonus} bonus words.")


if __name__ == "__main__":
    main()
