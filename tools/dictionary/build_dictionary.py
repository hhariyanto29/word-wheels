#!/usr/bin/env python3
"""
build_dictionary.py — Produce the bundled bonus-word dictionary.

Reads a source wordlist (default: /usr/share/dict/words on macOS/Linux),
applies length and charset filters, removes the union of NSFW deny-lists,
and writes a sorted, deduplicated, lowercase wordlist that ships with
each platform build (Android assets/, iOS Resources/, web/wasm).

Usage:
  python3 tools/dictionary/build_dictionary.py
  python3 tools/dictionary/build_dictionary.py --source path/to/scowl-words.txt
  python3 tools/dictionary/build_dictionary.py --min-len 3 --max-len 12

Outputs:
  - assets/dictionary.txt            (canonical, checked into the repo)
  - android-native/app/src/main/assets/dictionary.txt (copied for Android build)
  - ios-native/WordWheel/Resources/dictionary.txt    (copied for iOS build)

Design notes:
  - We use exact-match deny-listing. Substring matching produces too
    many false positives (the "scunthorpe problem"); the cost is that
    contributors must enumerate inflections manually, which is what
    nsfw-supplement.txt is for.
  - The default source is BSD `/usr/share/dict/words`. Swap to SCOWL
    by passing --source. The pipeline does not care about provenance;
    that's a license decision for the project owner.
"""
from __future__ import annotations

import argparse
import os
import shutil
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DICT_DIR = os.path.dirname(os.path.abspath(__file__))

DEFAULT_SOURCE = "/usr/share/dict/words"
# common-10k.txt covers modern inflections / everyday words that the
# BSD `words` list omits ("mom", "bro", "app", many -ing/-ed forms).
# Always merged in; the deny-list still applies.
COMMON_SUPPLEMENT = os.path.join(REPO, "tools", "common-10k.txt")
NSFW_BASE = os.path.join(DICT_DIR, "nsfw-base-en.txt")
NSFW_SUPPLEMENT = os.path.join(DICT_DIR, "nsfw-supplement.txt")

CANONICAL_OUT = os.path.join(REPO, "assets", "dictionary.txt")
# Android picks up the canonical via `assets.srcDirs("../../assets")` in
# app/build.gradle.kts, so no separate Android mirror — duplicating it
# triggers a "Duplicate resources" error from MergeSourceSetFolders.
IOS_OUT = os.path.join(REPO, "ios-native", "WordWheel", "Resources", "dictionary.txt")


def read_deny_list(path: str) -> set[str]:
    """Return lowercase words from a deny-list file. Skips empties + comments."""
    out: set[str] = set()
    if not os.path.exists(path):
        return out
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip().lower()
            if not line or line.startswith("#"):
                continue
            # Skip multi-word phrases & non-ASCII entries — our dictionary
            # is single A-Z words only, so nothing on that side could ever
            # match these anyway.
            if " " in line or not line.isascii():
                continue
            out.add(line)
    return out


def _ingest(path: str, deny: set[str], min_len: int, max_len: int,
            seen: set[str]) -> tuple[int, int, int, int]:
    """Add entries from `path` that pass our filters into `seen`. Returns
    (added, skipped_len, skipped_charset, skipped_nsfw)."""
    added = 0
    skipped_len = skipped_charset = skipped_nsfw = 0
    if not os.path.exists(path):
        return 0, 0, 0, 0
    with open(path, encoding="utf-8") as f:
        for raw in f:
            w = raw.strip().lower()
            if not w:
                continue
            if len(w) < min_len or len(w) > max_len:
                skipped_len += 1
                continue
            # Accept only A-Z; no apostrophes (rejects "don't"),
            # no hyphens (rejects "self-aware"), no diacritics.
            if not w.isascii() or not w.isalpha():
                skipped_charset += 1
                continue
            if w in deny:
                skipped_nsfw += 1
                continue
            if w not in seen:
                seen.add(w)
                added += 1
    return added, skipped_len, skipped_charset, skipped_nsfw


def build(source: str, min_len: int, max_len: int) -> list[str]:
    deny = read_deny_list(NSFW_BASE) | read_deny_list(NSFW_SUPPLEMENT)
    print(f"deny-list: {len(deny)} entries", file=sys.stderr)

    seen: set[str] = set()
    a, sl, sc, sn = _ingest(source, deny, min_len, max_len, seen)
    print(f"source={source}: added={a} "
          f"(skipped: len={sl}, charset={sc}, nsfw={sn})", file=sys.stderr)

    if os.path.exists(COMMON_SUPPLEMENT):
        a2, sl2, sc2, sn2 = _ingest(COMMON_SUPPLEMENT, deny, min_len, max_len, seen)
        print(f"common-supplement={COMMON_SUPPLEMENT}: added={a2} "
              f"(skipped: len={sl2}, charset={sc2}, nsfw={sn2})", file=sys.stderr)

    out = sorted(seen)
    print(f"total: {len(out)} words", file=sys.stderr)
    return out


def write(path: str, words: list[str]) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(words))
        f.write("\n")
    print(f"wrote {path}: {len(words)} words ({os.path.getsize(path)} bytes)",
          file=sys.stderr)


def check_canonical() -> None:
    """CI-friendly integrity check on the checked-in dictionary. Doesn't
    re-build (CI runners may not have a source wordlist installed); just
    verifies the canonical file exists, is sorted, contains nothing from
    the deny-list, and that all 3 platform mirrors match it."""
    if not os.path.exists(CANONICAL_OUT):
        sys.exit(f"canonical dictionary missing at {CANONICAL_OUT}")

    with open(CANONICAL_OUT, encoding="utf-8") as f:
        words = [w.strip() for w in f if w.strip()]

    if words != sorted(words):
        sys.exit("dictionary is not sorted (rerun build_dictionary.py)")

    deny = read_deny_list(NSFW_BASE) | read_deny_list(NSFW_SUPPLEMENT)
    leaks = [w for w in words if w in deny]
    if leaks:
        sample = ", ".join(leaks[:5])
        more = f" (+{len(leaks)-5} more)" if len(leaks) > 5 else ""
        sys.exit(f"deny-list leak in canonical: {sample}{more}")

    # iOS bundles its own copy under Resources/. Android reads the
    # canonical directly via assets.srcDirs("../../assets").
    if not os.path.exists(IOS_OUT):
        sys.exit(f"iOS mirror missing: {IOS_OUT}")
    with open(IOS_OUT, encoding="utf-8") as f:
        ios_words = [w.strip() for w in f if w.strip()]
    if ios_words != words:
        sys.exit(f"iOS mirror out of sync: {IOS_OUT}\n"
                 "rerun build_dictionary.py to refresh")

    print(f"OK: {len(words)} words, sorted, no leaks, iOS mirror matches.",
          file=sys.stderr)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--source", default=DEFAULT_SOURCE)
    p.add_argument("--min-len", type=int, default=3)
    p.add_argument("--max-len", type=int, default=15)
    p.add_argument("--check", action="store_true",
                   help="Validate canonical + mirrors instead of regenerating. "
                        "Doesn't require a source wordlist; safe to run in CI.")
    args = p.parse_args()

    if args.check:
        check_canonical()
        return

    if not os.path.exists(args.source):
        sys.exit(f"source wordlist not found: {args.source}\n"
                 "Pass --source pointing to a SCOWL/SOWPODS wordlist, or install "
                 "a system dict (apt/brew install words).")

    words = build(args.source, args.min_len, args.max_len)

    write(CANONICAL_OUT, words)
    # Mirror to iOS Resources/. Android picks the canonical up via the
    # srcDirs alias in app/build.gradle.kts.
    os.makedirs(os.path.dirname(IOS_OUT), exist_ok=True)
    shutil.copyfile(CANONICAL_OUT, IOS_OUT)
    print(f"copied to {IOS_OUT}", file=sys.stderr)


if __name__ == "__main__":
    main()
