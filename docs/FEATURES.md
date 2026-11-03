# Orca — Features & Game Logic (Android)

How Orca actually works under the hood: the rules I hold it to, the checker,
the validator, the datasets and the CSV import. Everything here describes
the shipped v1.2 behaviour.

## The loop

Pick a dataset → open a problem → write SQL → **Run** to see your result →
**Submit/Check** for a verdict → earn XP, keep the streak alive.

## XP and scoring rules

- **Accepted = +10 XP, first solve only.** Re-solving a problem you already
  solved awards 0 XP (it still counts toward your daily streak — see below).
  There is no partial XP in the native apps: a verdict is Accepted or not.
- **Wrong answer = 0 XP and the mistake counter goes up by 1.** Retries are
  unlimited; only the first Accepted solve pays out.
- **Submitting a query that errors also counts as a mistake.** Errors from
  the free **Run** button do not — Run is scratch space, Submit is the exam.
- **Viewing the solution first forfeits that problem's XP.** The moment you
  tap "Show solution", the problem is marked as revealed. Even if you then
  write the correct query yourself, it pays 0 XP. The forfeit is *persisted*
  (saved with your progress), so restarting the app does not restore it —
  that was a real loophole in v1.0 and closing it was v1.1's headline fix.
- Hints are free. Looking at a hint never costs XP.

## The checker (how Submit judges you)

Submit runs **your query and the reference solution against the same
database** and compares the two result sets. Your SQL text is never compared
to the solution's text — any query that produces the right result is
Accepted, however you wrote it.

- Column **count** must match, and every cell value must match.
- Numbers are compared with a **0.01 absolute tolerance**, so `100`,
  `100.0` and float residue like `100.0000004` all agree; beyond 0.01 is
  a miss.
- Text comparison is case-sensitive.
- **Row order is ignored — except on problems whose solution uses
  ORDER BY**, where your rows must come back in the same order.
- Duplicate rows are enforced as a multiset: the same rows the same number
  of times.
- Two empty result sets match each other (where that is genuinely the
  right answer).
- Column *names* are deliberately not compared — Orca judges your result,
  not your exact words or aliases.
- Results are capped at **5,000 rows** while running; a query that returns
  more is cut at 5,000 with a visible "Result cut at 5,000 rows" note
  instead of loading unbounded rows into memory.
- The verdict tells you the shape of the miss: expected vs your
  rows × columns.

## The validator (read-only by design)

Every query — Run and Submit — passes a read-only guard first, so a
practice app can never mutate its own datasets:

- Comments and string literals are stripped before any keyword checks, so
  a leading `-- Write your SQL here` line is harmless and a blocked word
  inside a string (e.g. `WHERE status = 'deleted'`) does not false-trigger.
- A query is allowed only if it is a plain `SELECT`, or a `WITH` whose main
  statement (the depth-0 keyword after the CTE definitions) is `SELECT`.
  `WITH … SELECT` and `WITH RECURSIVE` are fine.
- Write/DDL keywords appearing as real tokens anywhere outside literals —
  `INSERT`, `UPDATE`, `DELETE`, `DROP`, `ALTER`, `CREATE`, `REPLACE INTO`,
  `ATTACH`, `DETACH`, `PRAGMA`, `VACUUM` — reject the query. This closes
  the `WITH … DELETE` bypass: SQLite would happily execute it, so the
  guard, not the engine, is the wall.
- Only a single statement is allowed.
- The `REPLACE()` string function stays legal; only `REPLACE INTO` is
  blocked.

## Datasets

All four built-in datasets are **synthetic**, generated deterministically
on-device (fixed seeds) into real SQLite tables — the same data every
install, which is what makes the reference answers stable.

| Dataset | Tables | Contents |
|---|---|---|
| Retail & E-Commerce Store | customers (300), products (120), orders (2,500) | A growing online store's sales |
| UPI / Bank Transactions | accounts (250), transactions (2,547) | Bank accounts and transfers, with fraud planted |
| HR Employees | departments (8), employees (1,200) | Designations and salaries across 8 departments |
| Product App Analytics | users (800), events (2,969) | Login/view/purchase/share events, 100 power users mixed in |

Details worth knowing:

- **UPI fraud pattern:** 36 planted burst rows land on exactly 3 accounts,
  lifting them to 23, 22 and 21 transactions in total while normal accounts
  sit at 8–12. A `HAVING COUNT(*) > 18` cutoff therefore isolates exactly
  the 3 fraud accounts — the dataset is built so the "right" analysis works.
- **HR:** exactly 4 departments have ≥160 employees (220/200/190/180),
  which the Stage 2 summary problems rely on.
- **Product:** the 100 power users have 12–20 events each; everyone else
  has at most 3, so a `> 8` cutoff finds exactly the power users.

Plus **My Data**: tables you import yourself from CSV (below).

## Problems

29 original problems — Retail 11, UPI/Bank 6, HR 6, Product 6 — in two
stages:

- **Stage 1:** SELECT, WHERE, ORDER BY/LIMIT, DISTINCT
- **Stage 2:** aggregates (COUNT/SUM/AVG/MIN/MAX), GROUP BY, HAVING

Every problem carries:

- a difficulty (Easy/Medium/Hard),
- a **role tag** across 12 analyst tracks (Data, Business, Product, Risk,
  MIS, Operations, Financial, Marketing, Sales, HR/People, Supply Chain,
  Healthcare),
- an **interview-chance % labelled as an estimate**, scored from topic
  frequency (core patterns 75–92%, medium 45–70%, niche 20–44%) with a
  one-line reason,
- **"This type asked at"** company tags — pattern-level ("this *type* of
  question is asked at…"), never a claim that the exact problem appeared
  verbatim at that company,
- a hint, the reference solution, and an explanation of why it works.

## CSV import ("My Data")

- The parser reads the **whole document character-wise**: quoted commas,
  escaped quotes and quoted fields containing newlines all parse
  correctly; CRLF is handled, a BOM is stripped, blank lines are skipped,
  a trailing empty field is preserved, fields are trimmed. Imports are
  capped at 20,000 rows.
- **Headers are sanitized** into safe SQL identifiers. If two headers
  sanitize to the same name (`Price`, `PRICE`, `price `), the duplicates
  are de-duplicated with `_2`, `_3` suffixes instead of failing the import.
- **Type inference** checks every non-empty value: INTEGER if all values
  are integers, else REAL if all are numeric, else TEXT. An all-empty
  column becomes TEXT. Short rows are padded with NULL; extra fields are
  dropped. The import runs in one transaction — it lands whole or not at all.
- Imported tables **persist** in the app's database across restarts.
- **4 practice problems are generated per table** from its real schema:
  a first-look column peek, an "above average" WHERE built from the first
  numeric column's *actual* average, a GROUP BY count, and a top-5 —
  each with a reference solution that runs against your table. The Above
  Average template is skipped when its answer would be empty (for example
  a constant column), so it can never be a free solve.

## Streaks

- Solving on consecutive days builds your streak. **Any** Accepted solve
  counts the day — first-time or re-solve (XP is still first-time only).
- If your last solve was neither today nor yesterday, the home screen
  shows the streak as **0** and the next solve restarts it at 1. A gap
  breaks the streak; the number never sits stale.

## Learn

Six short theory cards matching the problem stages: SELECT, WHERE,
ORDER BY & LIMIT, DISTINCT, aggregates, and GROUP BY & HAVING — each with
the idea, the syntax shape and the common mistake. One honest dialect
note: the engine is SQLite, so `strftime` stands in for MySQL's
`DATE_FORMAT` where a date problem needs it, and the cards say so.

## Persistence

Progress — XP, solved problem ids, the revealed (solution-viewed) set,
streak and last-solve day, mistake count, current dataset — is stored
on-device (SharedPreferences), and imported CSV tables live in the app's
SQLite database. Everything survives restarts; nothing leaves the phone.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
