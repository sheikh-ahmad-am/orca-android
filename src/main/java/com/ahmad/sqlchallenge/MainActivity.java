package com.ahmad.sqlchallenge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    // palette
    static final int BG = Color.rgb(11, 18, 32);
    static final int CARD = Color.rgb(20, 32, 54);
    static final int CARD2 = Color.rgb(16, 42, 46);
    static final int STROKE = Color.rgb(52, 84, 120);
    static final int ACCENT = Color.rgb(46, 230, 168);
    static final int GOLD = Color.rgb(255, 196, 64);
    static final int TEXT = Color.rgb(235, 244, 255);
    static final int MUTED = Color.rgb(150, 170, 200);
    static final int RED = Color.rgb(255, 107, 107);

    static final int REQ_CSV = 77;

    private FrameLayout rootFrame;
    private SharedPreferences prefs;
    private Set<String> revealed = new HashSet<String>();
    private String currentScreen = "home";

    // ---------- helpers ----------
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    LinearLayout page() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(24));
        l.setBackgroundColor(BG);
        return l;
    }

    void setPage(View inner) {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(inner);
        rootFrame.removeAllViews();
        rootFrame.addView(sv);
    }

    TextView tv(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    GradientDrawable roundBg(int color, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    LinearLayout card(int bgColor) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(roundBg(bgColor, STROKE, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        c.setLayoutParams(lp);
        return c;
    }

    Button btn(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(primary ? Color.rgb(6, 26, 20) : TEXT);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(roundBg(primary ? ACCENT : CARD, primary ? ACCENT : STROKE, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(5), 0, dp(5));
        b.setLayoutParams(lp);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        return b;
    }

    LinearLayout headerRow(String title, final String backTo) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));
        Button back = new Button(this);
        back.setText("← Back");
        back.setAllCaps(false);
        back.setTextSize(13);
        back.setTextColor(TEXT);
        back.setBackground(roundBg(CARD, STROKE, 10));
        back.setPadding(dp(10), dp(4), dp(10), dp(4));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { go(backTo); }
        });
        row.addView(back);
        row.addView(tv("   " + title, 19, TEXT, true));
        return row;
    }

    void go(String screen) {
        if ("home".equals(screen)) showHome();
        else if ("datasets".equals(screen)) showDatasets();
        else if ("problems".equals(screen)) showProblems(prefs.getString("dataset", "retail"));
        else if ("mydata".equals(screen)) showMyData();
        else if ("learn".equals(screen)) showLearn();
        else if ("guide".equals(screen)) showGuide();
    }

    Set<String> solvedSet() {
        return new HashSet<String>(prefs.getStringSet("solved", new HashSet<String>()));
    }

    /** Persists the solution-viewed set (XP forfeit). It used to live only in
     *  memory, so a restart quietly restored the XP the button promised to
     *  forfeit — now it is saved on every change and reloaded at startup. */
    void persistRevealed() {
        prefs.edit().putStringSet("revealed", new HashSet<String>(revealed)).apply();
    }

    Set<String> revealedSet() {
        return new HashSet<String>(prefs.getStringSet("revealed", new HashSet<String>()));
    }

    void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    void runBg(final Runnable work, final Runnable done) {
        new Thread(new Runnable() {
            public void run() {
                work.run();
                if (done != null) runOnUiThread(done);
            }
        }).start();
    }

    // ---------- lifecycle ----------
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("progress", MODE_PRIVATE);
        revealed = revealedSet();
        rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(BG);
        setContentView(rootFrame);
        showHome();
    }

    @Override public void onBackPressed() {
        if (!"home".equals(currentScreen)) showHome();
        else super.onBackPressed();
    }

    // ---------- HOME ----------
    void showHome() {
        currentScreen = "home";
        final LinearLayout l = page();
        l.addView(tv("Orca", 24, TEXT, true));
        l.addView(tv("Practice SQL on real tables, earn XP, get interview-ready. Fully offline.", 13, MUTED, false));

        int xp = prefs.getInt("xp", 0);
        int level = xp / 100 + 1;
        int solved = solvedSet().size();
        int streak = displayedStreak();
        String dsName = Engine.datasetById(prefs.getString("dataset", "retail")).name;

        // XP tile (full width, glass-style gradient card)
        LinearLayout xpTile = card(CARD2);
        xpTile.addView(tv("TOTAL XP", 11, ACCENT, true));
        xpTile.addView(tv(xp + " XP  •  Level " + level, 22, TEXT, true));
        xpTile.addView(tv("Every solved problem = +10 XP. Level up every 100 XP.", 12, MUTED, false));
        l.addView(xpTile);

        // streak + solved row
        LinearLayout rowTiles = new LinearLayout(this);
        rowTiles.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout streakTile = card(CARD);
        streakTile.addView(tv("DAILY STREAK", 11, GOLD, true));
        streakTile.addView(tv(streak + (streak == 1 ? " day" : " days"), 18, TEXT, true));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        half.setMargins(0, 0, dp(5), 0);
        streakTile.setLayoutParams(half);
        LinearLayout solvedTile = card(CARD);
        solvedTile.addView(tv("PROBLEMS SOLVED", 11, ACCENT, true));
        solvedTile.addView(tv(String.valueOf(solved), 18, TEXT, true));
        LinearLayout.LayoutParams half2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        half2.setMargins(dp(5), 0, 0, 0);
        solvedTile.setLayoutParams(half2);
        rowTiles.addView(streakTile);
        rowTiles.addView(solvedTile);
        l.addView(rowTiles);

        LinearLayout dsTile = card(CARD);
        dsTile.addView(tv("CURRENT DATASET", 11, MUTED, true));
        dsTile.addView(tv(dsName, 17, TEXT, true));
        dsTile.addView(tv("Tap this tile to switch datasets. All built-in data is SYNTHETIC practice data.", 12, MUTED, false));
        dsTile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showDatasets(); }
        });
        l.addView(dsTile);

        Button bProblems = btn("Play — Problems", true);
        bProblems.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showProblems(prefs.getString("dataset", "retail")); }
        });
        l.addView(bProblems);
        Button bDs = btn("Datasets", false);
        bDs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showDatasets(); }
        });
        l.addView(bDs);
        Button bMy = btn("My Data — import a CSV", false);
        bMy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showMyData(); }
        });
        l.addView(bMy);
        Button bLearn = btn("Learn — theory cards", false);
        bLearn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showLearn(); }
        });
        l.addView(bLearn);
        Button bGuide = btn("Guide — how to use this app", false);
        bGuide.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showGuide(); }
        });
        l.addView(bGuide);
        int mistakes = prefs.getInt("mistakes", 0);
        if (mistakes > 0) l.addView(tv("Mistakes so far: " + mistakes + " — every wrong try is noted here. Retry any problem until it clicks.", 12, MUTED, false));
        setPage(l);
    }

    // ---------- DATASETS ----------
    void showDatasets() {
        currentScreen = "datasets";
        final LinearLayout l = page();
        l.addView(headerRow("Datasets", "home"));
        l.addView(tv("Built-in datasets are generated on your phone with a fixed seed, so the numbers are the same every run. All are labelled SYNTHETIC — practice data, not any company's real data.", 12, MUTED, false));
        final String current = prefs.getString("dataset", "retail");
        for (final Engine.DatasetInfo d : Engine.datasets()) {
            if ("mydata".equals(d.id)) continue;
            final LinearLayout c = card(CARD);
            c.addView(tv(d.name + (d.id.equals(current) ? "  •  ACTIVE" : ""), 16, TEXT, true));
            c.addView(tv("SYNTHETIC", 10, GOLD, true));
            c.addView(tv(d.blurb, 13, MUTED, false));
            final TextView meta = tv("Tables: " + join(d.tables) + "  •  preparing counts…", 12, MUTED, false);
            c.addView(meta);
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    prefs.edit().putString("dataset", d.id).apply();
                    toast("Dataset set: " + d.name);
                    showProblems(d.id);
                }
            });
            l.addView(c);
            runBg(new Runnable() {
                public void run() {
                    SQLiteDatabase db = Engine.openDataset(MainActivity.this, d.id);
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Tables: ");
                    for (int i = 0; i < d.tables.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(d.tables[i]).append(" (").append(Engine.rowCount(db, d.tables[i])).append(")");
                    }
                    db.close();
                    runOnUiThread(new Runnable() { public void run() { meta.setText(sb.toString()); } });
                }
            }, null);
        }
        Button bMy = btn("My Data — your own CSV tables", false);
        bMy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showMyData(); }
        });
        l.addView(bMy);
        setPage(l);
    }

    String join(String[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) { if (i > 0) sb.append(", "); sb.append(arr[i]); }
        return sb.toString();
    }

    // ---------- PROBLEMS LIST ----------
    void showProblems(final String datasetId) {
        currentScreen = "problems";
        final LinearLayout l = page();
        l.addView(headerRow("Problems", "home"));
        final Engine.DatasetInfo d = Engine.datasetById(datasetId);
        l.addView(tv(d.name, 16, ACCENT, true));
        final TextView loading = tv("Preparing dataset on this phone (first time takes a few seconds)…", 13, MUTED, false);
        l.addView(loading);
        final LinearLayout listHolder = new LinearLayout(this);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        l.addView(listHolder);
        setPage(l);
        runBg(new Runnable() {
            public void run() {
                final List<Problems.Problem> probs = new ArrayList<Problems.Problem>();
                SQLiteDatabase db = Engine.openDataset(MainActivity.this, datasetId);
                if ("mydata".equals(datasetId)) {
                    for (String t : Engine.tablesOf(db)) {
                        List<String> cols = new ArrayList<String>();
                        List<String> types = new ArrayList<String>();
                        for (String ct : Engine.columnsOf(db, t)) {
                            String[] parts = ct.split(" ", 2);
                            cols.add(parts[0]); types.add(parts.length > 1 ? parts[1] : "TEXT");
                        }
                        probs.addAll(Engine.csvProblems(db, t, cols, types));
                    }
                } else {
                    for (Problems.Problem p : Problems.all())
                        if (p.dataset.equals(datasetId)) probs.add(p);
                }
                db.close();
                final Set<String> solved = solvedSet();
                runOnUiThread(new Runnable() {
                    public void run() {
                        loading.setText(probs.size() + " problems • interview chance is an estimate • tap one to play");
                        if (probs.size() == 0) {
                            listHolder.addView(tv("No problems yet for this dataset. Import a CSV from My Data first.", 13, MUTED, false));
                        }
                        for (final Problems.Problem p : probs) {
                            LinearLayout c = card(CARD);
                            c.addView(tv(p.title + (solved.contains(p.id) ? "   ✓ SOLVED" : ""), 15, TEXT, true));
                            c.addView(tv(p.difficulty + "  •  " + p.role + "  •  " + p.stage + " — " + p.topic, 12, MUTED, false));
                            c.addView(tv("Interview chance (estimate): " + p.chance + "%", 12, GOLD, true));
                            c.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) { showProblem(p); }
                            });
                            listHolder.addView(c);
                        }
                    }
                });
            }
        }, null);
    }

    // ---------- PROBLEM SCREEN ----------
    void showProblem(final Problems.Problem p) {
        currentScreen = "problem";
        final LinearLayout l = page();
        l.addView(headerRow("Problem", "problems"));
        l.addView(tv(p.title, 19, TEXT, true));
        l.addView(tv(p.difficulty + "  •  " + p.role + "  •  " + p.stage + " — " + p.topic, 12, MUTED, false));
        l.addView(tv("Interview chance (estimate): " + p.chance + "% — " + p.chanceReason, 12, GOLD, false));
        l.addView(tv("This type asked at: " + p.companies + " (question types, not this exact problem)", 12, MUTED, false));

        LinearLayout qCard = card(CARD2);
        qCard.addView(tv("BUSINESS QUESTION", 11, ACCENT, true));
        qCard.addView(tv(p.question, 15, TEXT, false));
        l.addView(qCard);

        final LinearLayout schemaCard = card(CARD);
        schemaCard.addView(tv("SCHEMA + SAMPLE PEEK", 11, ACCENT, true));
        final TextView schemaText = tv("Loading tables…", 12, MUTED, false);
        schemaText.setTypeface(Typeface.MONOSPACE);
        schemaCard.addView(schemaText);
        l.addView(schemaCard);

        final TextView hintText = tv("", 13, GOLD, false);
        hintText.setVisibility(View.GONE);
        Button bHint = btn("Show hint", false);
        bHint.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hintText.setText("Hint: " + p.hint);
                hintText.setVisibility(View.VISIBLE);
            }
        });
        l.addView(bHint);
        l.addView(hintText);

        final EditText editor = new EditText(this);
        editor.setHint("Write your SQL here…");
        editor.setHintTextColor(MUTED);
        editor.setTextColor(TEXT);
        editor.setTextSize(14);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setMinLines(4);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setBackground(roundBg(Color.rgb(8, 13, 24), STROKE, 10));
        editor.setPadding(dp(10), dp(10), dp(10), dp(10));
        l.addView(editor);

        final TextView verdict = tv("", 15, TEXT, true);
        final LinearLayout resultHolder = new LinearLayout(this);
        resultHolder.setOrientation(LinearLayout.VERTICAL);
        final TextView explText = tv("", 13, MUTED, false);
        explText.setVisibility(View.GONE);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button bRun = btn("Run", false);
        Button bSubmit = btn("Submit / Check", true);
        LinearLayout.LayoutParams hb = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hb.setMargins(0, 0, dp(4), 0);
        bRun.setLayoutParams(hb);
        LinearLayout.LayoutParams hb2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hb2.setMargins(dp(4), 0, 0, 0);
        bSubmit.setLayoutParams(hb2);
        btnRow.addView(bRun);
        btnRow.addView(bSubmit);
        l.addView(btnRow);
        l.addView(verdict);
        l.addView(resultHolder);

        Button bSolution = btn("Show solution (no XP after viewing)", false);
        bSolution.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                revealed.add(p.id);
                persistRevealed();
                explText.setText("Solution:\n" + p.solution + "\n\nWhy it works: " + p.explanation);
                explText.setVisibility(View.VISIBLE);
            }
        });
        l.addView(bSolution);
        l.addView(explText);
        setPage(l);

        // schema + sample load
        runBg(new Runnable() {
            public void run() {
                SQLiteDatabase db = Engine.openDataset(MainActivity.this, p.dataset);
                final StringBuilder sb = new StringBuilder();
                List<String> tables = Engine.tablesOf(db);
                List<String> show = new ArrayList<String>();
                if ("mydata".equals(p.dataset)) {
                    // only the imported table this problem targets: parse from solution FROM clause
                    String sol = p.solution.toUpperCase(Locale.US);
                    for (String t : tables) if (sol.contains(" " + t.toUpperCase(Locale.US))) show.add(t);
                    if (show.isEmpty() && tables.size() > 0) show.add(tables.get(0));
                } else show.addAll(tables);
                for (String t : show) {
                    sb.append(t).append(" (").append(Engine.rowCount(db, t)).append(" rows)\n");
                    for (String col : Engine.columnsOf(db, t)) sb.append("   • ").append(col).append("\n");
                    Engine.QueryResult sample = Engine.runQuery(db, "SELECT * FROM " + t + " LIMIT 3");
                    if (sample.error == null && sample.rows.size() > 0) {
                        sb.append("   sample: ");
                        for (int i = 0; i < Math.min(3, sample.rows.size()); i++) {
                            if (i > 0) sb.append("  |  ");
                            String[] row = sample.rows.get(i);
                            for (int j = 0; j < row.length; j++) { if (j > 0) sb.append(", "); sb.append(row[j]); }
                        }
                        sb.append("\n");
                    }
                    sb.append("\n");
                }
                db.close();
                runOnUiThread(new Runnable() { public void run() { schemaText.setText(sb.toString().trim()); } });
            }
        }, null);

        bRun.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String sql = editor.getText().toString();
                verdict.setText("Running…"); verdict.setTextColor(MUTED);
                runBg(new Runnable() {
                    public void run() {
                        SQLiteDatabase db = Engine.openDataset(MainActivity.this, p.dataset);
                        final Engine.QueryResult res = Engine.runQuery(db, sql);
                        db.close();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                resultHolder.removeAllViews();
                                if (res.error != null) {
                                    verdict.setText("SQL error: " + res.error);
                                    verdict.setTextColor(RED);
                                } else {
                                    verdict.setText("Your query ran: " + res.rows.size() + " rows. Submit when it looks right."
                                        + (res.note != null ? "\n" + res.note : ""));
                                    verdict.setTextColor(MUTED);
                                    resultHolder.addView(resultTable(res, 30));
                                }
                            }
                        });
                    }
                }, null);
            }
        });

        bSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String sql = editor.getText().toString();
                verdict.setText("Checking…"); verdict.setTextColor(MUTED);
                runBg(new Runnable() {
                    public void run() {
                        SQLiteDatabase db = Engine.openDataset(MainActivity.this, p.dataset);
                        final Engine.QueryResult mine = Engine.runQuery(db, sql);
                        final Engine.QueryResult expected = Engine.runQuery(db, p.solution);
                        db.close();
                        final boolean ok = mine.error == null && Engine.resultsMatch(mine, expected, p.orderSensitive);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                resultHolder.removeAllViews();
                                if (mine.error != null) {
                                    verdict.setText("Not quite — SQL error: " + mine.error);
                                    verdict.setTextColor(RED);
                                    noteMistake();
                                    return;
                                }
                                if (ok) {
                                    Set<String> solved = solvedSet();
                                    boolean firstTime = !solved.contains(p.id);
                                    if (firstTime) {
                                        solved.add(p.id);
                                        SharedPreferences.Editor e = prefs.edit();
                                        e.putStringSet("solved", solved);
                                        if (!revealed.contains(p.id)) e.putInt("xp", prefs.getInt("xp", 0) + 10);
                                        e.apply();
                                    }
                                    // Any accepted solve counts the day toward the
                                    // streak — first-time or re-solve alike (XP is
                                    // still first-time only). updateStreak no-ops
                                    // when today is already counted.
                                    updateStreak();
                                    verdict.setText(revealed.contains(p.id) && firstTime
                                        ? "Accepted ✓ — correct, but no XP because the solution was viewed."
                                        : firstTime ? "Accepted ✓  +10 XP" : "Accepted ✓ (already solved earlier)");
                                    verdict.setTextColor(ACCENT);
                                    explText.setText("Why it works: " + p.explanation);
                                    explText.setVisibility(View.VISIBLE);
                                } else {
                                    noteMistake();
                                    verdict.setText("Not quite. Expected " + expected.rows.size() + " rows × " +
                                        expected.columns.length + " cols • Yours " + mine.rows.size() + " rows × " +
                                        mine.columns.length + " cols. Check the hint and try again.");
                                    verdict.setTextColor(RED);
                                    resultHolder.addView(tv("Your result (first rows):", 12, MUTED, true));
                                    resultHolder.addView(resultTable(mine, 10));
                                }
                            }
                        });
                    }
                }, null);
            }
        });
    }

    void noteMistake() {
        prefs.edit().putInt("mistakes", prefs.getInt("mistakes", 0) + 1).apply();
    }

    String dayString(int offsetDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, offsetDays);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    /** The streak Home may show: if the last solve day is neither today nor
     *  yesterday, the streak is broken and displays 0 until a solve today
     *  starts a new one (the old code showed the stale number indefinitely). */
    int displayedStreak() {
        String last = prefs.getString("lastDay", "");
        if (dayString(0).equals(last) || dayString(-1).equals(last))
            return prefs.getInt("streak", 0);
        return 0;
    }

    void updateStreak() {
        String today = dayString(0);
        String last = prefs.getString("lastDay", "");
        if (today.equals(last)) return;
        int streak = dayString(-1).equals(last) ? prefs.getInt("streak", 0) + 1 : 1;
        prefs.edit().putInt("streak", streak).putString("lastDay", today).apply();
    }

    View resultTable(Engine.QueryResult res, int maxRows) {
        TableLayout table = new TableLayout(this);
        table.setBackground(roundBg(Color.rgb(8, 13, 24), STROKE, 8));
        table.setPadding(dp(6), dp(6), dp(6), dp(6));
        TableRow head = new TableRow(this);
        for (String col : res.columns) {
            TextView c = tv(" " + col + " ", 12, ACCENT, true);
            c.setTypeface(Typeface.MONOSPACE);
            head.addView(c);
        }
        table.addView(head);
        int n = Math.min(maxRows, res.rows.size());
        for (int i = 0; i < n; i++) {
            TableRow row = new TableRow(this);
            for (String cell : res.rows.get(i)) {
                TextView c = tv(" " + cell + " ", 12, TEXT, false);
                c.setTypeface(Typeface.MONOSPACE);
                row.addView(c);
            }
            table.addView(row);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.addView(table);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(hs);
        if (res.rows.size() > n) wrap.addView(tv("… " + res.rows.size() + " rows total, showing first " + n, 11, MUTED, false));
        return wrap;
    }

    // ---------- MY DATA ----------
    void showMyData() {
        currentScreen = "mydata";
        final LinearLayout l = page();
        l.addView(headerRow("My Data", "home"));
        l.addView(tv("Import your own CSV file and the app turns it into a real table, then generates practice problems from its columns. Files stay on this phone.", 13, MUTED, false));
        Button bImport = btn("Import a CSV file", true);
        bImport.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "text/csv", "text/comma-separated-values", "text/plain"});
                startActivityForResult(intent, REQ_CSV);
            }
        });
        l.addView(bImport);
        final LinearLayout holder = new LinearLayout(this);
        holder.setOrientation(LinearLayout.VERTICAL);
        l.addView(holder);
        setPage(l);
        runBg(new Runnable() {
            public void run() {
                SQLiteDatabase db = Engine.openDataset(MainActivity.this, "mydata");
                final List<String> lines = new ArrayList<String>();
                for (String t : Engine.tablesOf(db)) {
                    lines.add(t + " — " + Engine.rowCount(db, t) + " rows");
                    lines.add("   " + join(Engine.columnsOf(db, t).toArray(new String[0])));
                }
                db.close();
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (lines.isEmpty()) {
                            holder.addView(tv("No imported tables yet.", 13, MUTED, false));
                        } else {
                            holder.addView(tv("YOUR IMPORTED TABLES", 11, ACCENT, true));
                            for (String s : lines) holder.addView(tv(s, 13, TEXT, false));
                            Button bPlay = btn("Practice on my data", true);
                            bPlay.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    prefs.edit().putString("dataset", "mydata").apply();
                                    showProblems("mydata");
                                }
                            });
                            holder.addView(bPlay);
                        }
                    }
                });
            }
        }, null);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CSV || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        String displayName = "my_table.csv";
        try {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && c.moveToFirst()) displayName = c.getString(idx);
                c.close();
            }
        } catch (Exception ignored) { }
        final String fname = displayName;
        // If a non-CSV slipped past the picker (some file apps ignore the
        // type filter), say so plainly instead of failing with garbage.
        // Engine.importCsv additionally sniffs binary content (NUL / %PDF /
        // zip magic) and returns the same clear message.
        {
            String lower = fname.toLowerCase(Locale.US);
            String mime = null;
            try { mime = getContentResolver().getType(uri); } catch (Exception ignored) { }
            boolean csvName = lower.endsWith(".csv") || lower.endsWith(".txt");
            boolean nonTextMime = mime != null && !mime.startsWith("text/");
            if (!csvName && nonTextMime) {
                toast("That file is not a CSV — pick a .csv file exported from your spreadsheet app.");
                return;
            }
        }
        toast("Importing " + fname + "…");
        runBg(new Runnable() {
            public void run() {
                Engine.ImportSummary sum;
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    SQLiteDatabase db = Engine.openDataset(MainActivity.this, "mydata");
                    sum = Engine.importCsv(db, in, fname);
                    db.close();
                } catch (Exception e) {
                    sum = new Engine.ImportSummary();
                    sum.error = "Could not open that file: " + e.getMessage();
                }
                final Engine.ImportSummary s2 = sum;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (s2.error != null) {
                            toast(s2.error);
                        } else {
                            toast("Imported " + s2.table + ": " + s2.rows + " rows, " + s2.columns.size() + " columns");
                            prefs.edit().putString("dataset", "mydata").apply();
                        }
                        showMyData();
                    }
                });
            }
        }, null);
    }

    // ---------- LEARN ----------
    void showLearn() {
        currentScreen = "learn";
        LinearLayout l = page();
        l.addView(headerRow("Learn", "home"));
        l.addView(tv("Short theory cards for Stage 1 and Stage 2. Tap a card to open it. Note: the on-phone engine is SQLite, so a few functions differ from MySQL — the cards flag those spots.", 12, MUTED, false));
        for (final Problems.Lesson lesson : Problems.lessons()) {
            LinearLayout c = card(CARD);
            c.addView(tv(lesson.topic, 15, TEXT, true));
            final TextView body = tv(lesson.body, 13, MUTED, false);
            body.setTypeface(Typeface.MONOSPACE);
            body.setVisibility(View.GONE);
            c.addView(body);
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });
            l.addView(c);
        }
        setPage(l);
    }

    // ---------- GUIDE ----------
    void showGuide() {
        currentScreen = "guide";
        LinearLayout l = page();
        l.addView(headerRow("Guide", "home"));
        String[][] sections = {
            {"1. Pick a dataset", "Home → Datasets. Start with Retail & E-Commerce, or pick the dataset made for your target role (UPI/Bank for Risk, Product App for Product Analyst, HR for HR Analyst). Built-in data is synthetic practice data, generated the same way every time."},
            {"2. Solve problems", "Play — Problems lists this dataset's problems with difficulty, role tag and an interview-chance estimate. Open one, read the business question, check the schema card and sample rows, then write SQL in the editor."},
            {"3. Run vs Submit", "Run just shows your result (or the SQL error) with no scoring — use it while drafting. Submit/Check compares your full result against the solution's result and gives the verdict: Accepted (+10 XP) or Not quite, with expected vs your row counts. Wrong tries are counted as mistakes; you can retry as often as you like."},
            {"4. Stuck?", "Use the hint first. The solution button explains the answer, but viewing it before solving means no XP for that problem — it is for learning, not scoring."},
            {"5. Bring your own data", "My Data → Import a CSV. The app infers the column types, creates a real table, and generates practice problems (select, filter, group-by, top-5) from your own columns."},
            {"6. Keep the streak", "Solving on consecutive days builds your streak, shown on Home next to your XP and level (100 XP per level). Theory cards live under Learn."},
        };
        for (String[] s : sections) {
            LinearLayout c = card(CARD);
            c.addView(tv(s[0], 15, ACCENT, true));
            c.addView(tv(s[1], 13, TEXT, false));
            l.addView(c);
        }
        setPage(l);
    }
}
