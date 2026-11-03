# Development Notes — Orca for Android

Design decisions, release history and honest scope notes, kept from the
project's own build log (lightly edited for publication — machine-local
paths removed).

## Where Orca came from

Orca started as a web app: a full SQL challenge game with 22 datasets, 199
problems across all 6 stages, role tracks for 12 analyst careers, a Mistake
Book with spaced revision, company packs, mini-projects and mock interviews.
For the phone, I chose a **native Android build over a WebView/Capacitor
wrap**: plain Java, a single activity, programmatic UI, framework-only
code, Android's built-in SQLite — and a scope cut to Stages 1–2 so the
native version is complete and honest rather than a thin shell of the web
one. The price of that choice is real: partial XP, boss fights, packs and
the Mistake Book schedule live in the web edition for now (see Roadmap in
the README).

More design decisions that shaped the app:

- **Problems are also generated from the player's own CSVs** — multiple-file
  support from day 1 was a core requirement, not an add-on. A small
  built-in demo dataset ships so the app works before any upload.
- **Interview framing is labelled honestly.** The interview-chance % is an
  estimate scored from topic frequency (core patterns 75–92%, medium
  45–70%, niche 20–44%), and company tags say "This type asked at" —
  pattern-level, never a claim that the exact problem appeared verbatim.
- **Datasets are labelled synthetic, never passed off as real.** Where
  real row-level data isn't public, the datasets use realistic simulated
  records and say so.
- The original problems are written for this app — platform style inspired
  by DataLemur / StrataScratch / LeetCode Database / HackerRank, never
  copied content or branding.

## Build approach

No Gradle: the sandbox I built in kept killing Gradle's daemon, and a
manual pipeline turned out to be a feature — `aapt2 compile/link → javac
-source 8 → d8 → zipalign → apksigner`, every step visible in one script
(`manual-build.sh`). JDK 17 + build-tools 34.0.0.

Signing uses **one permanent keystore**, created once and never
regenerated — a new keystore would make Android treat every update as a
different app and wipe the update path (and players' progress continuity).
The keystore and its password are private and are not in this repo; the
public build script is parameterized instead (see BUILD.md). Every release
below was verified to carry the same signer certificate as v1.0, so each
installs over the previous one.

## Release history

### v1.2 — 2026-10-29 · Renamed to Orca
- Renamed on request: launcher label and home-screen title read "Orca";
  package unchanged so the update keeps all progress.
- New icon: my orca silhouette — black animal only, background removed,
  fully transparent, padded ~11% centered on a square canvas so launchers
  don't crop the fins/tail. Master `icon-orca.png` (1024×1024); density
  PNGs regenerated (48/72/96/144/192).
- Verified: badging (label "Orca", versionCode 3 / versionName 1.2,
  minSdk 26 / targetSdk 34), `apksigner verify` (v2+v3, same cert),
  `zipalign -c 4`, zero permissions; the icon was extracted back out of
  the APK and proven transparent (alpha channel, transparent corners,
  pure-black silhouette matching the master).

### v1.1 — 2026-10-29 · Audit fix pass
v1.0 got a full end-to-end audit (rebuild from source, every button's
wiring read, the checker exercised cell-by-cell, all solutions executed,
CSV edge cases, persistence). Ten findings, all fixed (details in the
CHANGELOG): persisted solution-view forfeit, the hardened query validator
(closing a `WITH … DELETE` bypass), CSV header dedup, the whole-document
CSV parser, the corrected 2,547-transaction count, streak gap handling,
CSV-only file picking, the 5,000-row result cap, the empty-template skip,
and the tappable dataset tile. Regression: a fresh JVM harness, 47/47,
run against the real compiled classes; all 29 solutions re-executed clean.

### v1.0 — 2026-10-29 · Initial release
- Bento home dashboard (XP + level, streak, solved count, current
  dataset; dark glass-style gradient cards).
- 4 synthetic datasets generated deterministically on-device (fixed
  seeds): Retail (300 customers, 120 products, 2,500 orders), UPI/Bank
  (250 accounts, 2,547 transactions incl. 36 planted fraud-burst rows on
  3 accounts), HR (8 departments, 1,200 employees), Product App (800
  users, 2,969 events, 100 power users).
- 29 problems, each with difficulty, role tag, interview-chance estimate
  with reason, "This type asked at" companies, hint, solution, explanation.
- Problem screen: schema + 3-row sample peek, Run vs Submit/Check with
  result-set comparison (order enforced only on ORDER BY problems).
- CSV import with type inference and 4 generated template problems per
  table; 6 Learn theory cards; Guide screen; progress and imported
  tables persist across restarts.

## v1 scope limits (honest)

- Stages 1–2 only: no joins, subqueries or window functions yet; XP is
  10 or 0 (no partial credit) in the native apps.
- The engine is SQLite, not MySQL: `strftime` stands in for `DATE_FORMAT`
  in the one date problem, and the Learn cards flag the difference.
- The UPI dataset feeds planted-fraud problems, but there is no separate
  "fraud investigation" flow yet.
- Icon rendering caveat: some phone launchers paint their own background
  behind transparent icons — that's the launcher, not the icon file,
  which ships genuinely transparent.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
