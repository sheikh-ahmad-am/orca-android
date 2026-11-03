# Changelog — Orca for Android

## v1.2 — 2026-10-29 · Renamed to Orca
- The app is now called **Orca** (launcher label and home-screen title). Package
  unchanged (`com.ahmad.sqlchallenge`), so the update installs over v1.1 and
  keeps all progress.
- New launcher icon: a black orca silhouette on a fully transparent background,
  padded ~11% on a square canvas so launchers don't crop the fins or tail
  (master: `icon-orca.png`, 1024×1024; density PNGs in `res/mipmap-*`).
- Verified: badging (label "Orca", versionCode 3 / versionName 1.2, minSdk 26,
  targetSdk 34), signature v2+v3 with the same certificate as v1.0/v1.1,
  zipalign, zero permissions, icon transparency proven from inside the APK,
  all 29 problem solutions re-executed clean.

## v1.1 — 2026-10-29 · Audit fix pass
A full end-to-end audit of v1.0 produced ten findings; all ten are fixed here:
1. Solution-view XP forfeit is now persisted (SharedPreferences) — restarting
   the app no longer restores forfeited XP.
2. Read-only query guard hardened: comments and string literals are stripped
   before keyword checks; a `WITH` is accepted only when its main statement is
   `SELECT` — the `WITH … DELETE/UPDATE/INSERT` bypass is closed. Legitimate
   `WITH … SELECT` and `REPLACE()` still work.
3. CSV import de-duplicates colliding column headers with `_2`/`_3` suffixes
   instead of failing the whole import.
4. UPI/Bank dataset description corrected to the true count: 2,547 transactions.
5. Streak display goes to 0 after a gap, and any accepted solve (first-time or
   re-solve) counts the day; XP stays first-time only.
6. CSV parser rewritten to read the whole document character-wise: quoted
   fields containing newlines parse as single fields.
7. File picker restricted to CSV types; a non-CSV file fails with a clear
   message instead of garbage data.
8. Runaway queries capped at 5,000 result rows with a visible note.
9. The generated "Above Average" CSV problem is skipped when its reference
   answer would be empty (e.g. a constant column).
10. Home screen's current-dataset tile is tappable and opens the chooser.
- Regression: 47/47 checks in the JVM harness (checker rules, validator cases,
  CSV parser cases, header dedup), 29/29 problem solutions clean.

## v1.0 — 2026-10-29 · Initial release
- Native, fully offline Android app (plain Java, single activity, programmatic
  UI, Android's built-in SQLite). No Gradle — a manual aapt2/javac/d8 pipeline.
- 4 built-in synthetic datasets generated deterministically on-device: Retail
  & E-Commerce (2,500 orders), UPI/Bank (2,547 transactions incl. 36 planted
  fraud rows), HR Employees (1,200 employees), Product App Analytics
  (2,969 events).
- 29 original problems across Stage 1 (SELECT, WHERE, ORDER BY/LIMIT,
  DISTINCT) and Stage 2 (aggregates, GROUP BY/HAVING), each with difficulty,
  role tag, an interview-chance estimate with a reason, "This type asked at"
  companies, a hint, a worked solution and an explanation.
- Problem screen with schema + sample peek, Run vs Submit/Check, result-set
  comparison (column count + values; row order enforced only on ORDER BY
  problems). Accepted = +10 XP, first solve only; viewing the solution first
  forfeits the XP.
- CSV import ("My Data"): type inference, persistent tables, and 4 generated
  practice problems per imported table.
- Learn (6 theory cards) and Guide screens. Progress stored on-device and
  survives restarts.
