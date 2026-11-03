<p align="center">
  <img src="icon-orca.png" width="140" alt="Orca icon">
</p>

# Orca — SQL Practice Game (Android)

**Practice SQL like a game — real queries, real datasets, XP, streaks — 100% offline, no account, no ads.**

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-2EE6A8)
![Offline](https://img.shields.io/badge/offline-100%25-blue)
![Version](https://img.shields.io/badge/version-1.2-orange)
![License](https://img.shields.io/badge/license-MIT-green)

## About

I built Orca because most SQL practice happens in a browser tab with toy
tables of five rows. Orca puts a small game on your phone instead: open it,
pick a dataset, and write real SQL against tables with thousands of rows —
an online store's orders, bank transactions with fraud planted in them,
1,200 employees, a product app's event stream.

Every answer is checked by actually running your query and comparing its
result set with the reference solution's — not by matching text. Get it
right and you earn XP; peek at the solution first and the XP for that
problem is gone for good. Import your own CSV and Orca builds practice
problems from *your* columns.

The teaching dialect is MySQL; the on-device engine is SQLite, and the few
places where they differ are flagged in the Learn section.

## Features

- **29 original problems** across Stage 1 (SELECT, WHERE, ORDER BY/LIMIT,
  DISTINCT) and Stage 2 (COUNT/SUM/AVG, GROUP BY/HAVING)
- **4 built-in datasets**, generated deterministically on-device into real
  SQLite tables — fact tables in the thousands of rows:
  Retail & E-Commerce (2,500 orders) · UPI/Bank (2,547 transactions,
  36 planted fraud rows) · HR Employees (1,200 employees) ·
  Product App Analytics (2,969 events, 100 power users)
- **Result-set judging** — your query and the solution both run against the
  same database; column count and values are compared with a 0.01 numeric
  tolerance, and row order is enforced only where the problem uses ORDER BY
- **XP & streaks** — +10 XP for a first-time solve, daily streak with a
  gap rule, mistake counting, progress saved on-device across restarts
- **Interview framing on every problem** — difficulty, a role tag (12 analyst
  tracks), an interview-chance % labelled as an estimate with a one-line
  reason, and "This type asked at" company tags (pattern-level)
- **Hints, solutions, explanations** — with an honest rule: viewing the
  solution first forfeits that problem's XP, and the forfeit persists
- **My Data — CSV import**: bring your own CSV, get a typed persistent table
  plus 4 generated practice problems built from its real columns
- **Learn & Guide built in** — 6 short theory cards and a step-by-step guide
- **Read-only by design** — a hardened validator runs every query; writes
  and DDL are rejected, including disguised `WITH … DELETE` attempts
- **Zero permissions, zero network** — the app requests no Android
  permissions at all and works fully offline

## Screenshots

Screenshots will be added here — the table below is the shot list.

| Home | Datasets | Problem | Learn |
|------|----------|---------|-------|
| _coming soon_ | _coming soon_ | _coming soon_ | _coming soon_ |

## Download & Install

1. Go to the **Releases** page of this repository and download the latest
   `Orca-v1.2.apk`.
2. Open the APK on your phone (Android 8.0 / API 26 or newer).
3. Android will warn that the app comes from outside the Play Store — this
   is normal for a direct install. Allow "install from this source" for your
   browser/files app, then install.
4. Open **Orca** and start with the Retail dataset, Problem 1.

Updates install over the top and keep your progress, as long as they are
signed with the same key — see [docs/BUILD.md](docs/BUILD.md).

## Build from Source

No Gradle: the build is a transparent manual pipeline —
`aapt2 → javac → d8 → zipalign → apksigner` — driven by one script.

```bash
KEYSTORE_PASSWORD='your-password' ./manual-build.sh
```

You need JDK 17 and Android build-tools 34.0.0, plus your own signing
keystore (the script creates one on first run; the project's release
keystore is private and is **not** in this repo). Full instructions:
**[docs/BUILD.md](docs/BUILD.md)**. Game rules and logic in depth:
**[docs/FEATURES.md](docs/FEATURES.md)**.

## Tech Stack

- **Language:** Java (single activity, programmatic UI — no XML layouts,
  no frameworks)
- **Storage/engine:** Android's built-in SQLite (`SQLiteDatabase`)
- **Build:** manual aapt2/javac/d8/zipalign/apksigner pipeline, JDK 17,
  build-tools 34.0.0, minSdk 26 / targetSdk 34
- **Package:** `com.ahmad.sqlchallenge`

## Project Structure

```
orca-android/
├── README.md                  ← you are here
├── CHANGELOG.md
├── LICENSE
├── manual-build.sh            ← the whole build, one script
├── icon-orca.png              ← launcher icon master (1024×1024)
├── docs/
│   ├── BUILD.md               ← full build instructions
│   ├── FEATURES.md            ← game rules & engine logic in depth
│   └── DEVELOPMENT-NOTES.md   ← design & release notes
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/ahmad/sqlchallenge/
    │   ├── MainActivity.java  ← every screen
    │   ├── Engine.java        ← datasets, query runner, checker, CSV import
    │   └── Problems.java      ← problem bank + theory lessons
    └── res/                   ← icon densities, strings, theme
```

## Roadmap

- **Web edition (already exists):** 22 datasets and 199 problems, all 6
  stages, Mistake Book with spaced revision, company packs, mini-projects
  and timed mock interviews — the native apps are catching up to it.
- **Stages 3–6** for Android: CASE/functions/NULL handling, joins,
  subqueries/CTEs/window functions, and business-analysis capstones.
- Boss fights per stage, partial XP (1–9), and the Mistake Book schedule.

## License

MIT — see [LICENSE](LICENSE). Copyright © 2026 Md Ahmad.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
