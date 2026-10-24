# Third-party source and binary provenance

This directory contains Rachel's Native desktop adaptations. They are modified
builds and must not be presented as official JetBrains Skiko releases.

- **Skiko** — JetBrains, Apache-2.0. The official `v0.156.0` baseline is commit
  `71d1a686fe49bac19ef1767b9bded4800a5a67c5` in
  <https://github.com/JetBrains/skiko>.
- **Skiko Native reference** — Shivam / brahmkshatriya and contributors,
  Apache-2.0. Desktop Native implementation and Windows static CRT integration
  are derived from commit `85a7cf78045555f885fa774ac6007c6258d2bfb7` in
  <https://github.com/brahmkshatriya/skiko-native>. Its three-way merge base is
  `c4b1940eaa73abfcbaa3f3f5d5972ab0ac7b9810`.
- **Skia and bundled dependencies** — the original JetBrains Skia binary
  release `m156-24300fbbcb`, as selected by Skiko `0.156.0`. Its original
  licenses and notices must accompany redistributed binaries. Archive hashes
  are recorded in `upstream/lock.json`.
- **SDL 3.4.18** — Sam Lantinga and contributors, zlib license. The window
  sample uses the official SDL development package. Sources and binary hashes
  are recorded in `upstream/sdl.json`. See `licenses/SDL.txt`.

The original copyright/license files remain in the ignored source checkouts.
Rachel's reviewed file selection and adaptations are recorded in
`upstream/skiko-native-files.json`, `skiko/adapt.py` and `scripts/sync_upstream.py`.
Source artifacts must be produced from these prepared sources, including both
upstream code and adaptations, with their original notices intact.
