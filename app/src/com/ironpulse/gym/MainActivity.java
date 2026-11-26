package com.ironpulse.gym;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity {

    private WebView webView;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private static final String PREF_VERSION_KEY = "ironpulse_ui_version";
    private static final String GITHUB_VERSION_URL = "https://raw.githubusercontent.com/adamyhy/ironpulse-gym-tracker/main/ota/version.json";

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "ironpulse_gym.db";
        private static final int DATABASE_VERSION = 1;

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE exercises (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "target_count INTEGER DEFAULT 100, " +
                    "unit TEXT DEFAULT 'reps', " +
                    "icon TEXT DEFAULT 'dumbbell', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            db.execSQL("CREATE TABLE logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "exercise_id INTEGER NOT NULL, " +
                    "count INTEGER NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE)");

            db.execSQL("INSERT INTO exercises (name, category, target_count, unit, icon) VALUES ('Push Up', 'Chest & Arms', 100, 'reps', 'pushup')");
            db.execSQL("INSERT INTO exercises (name, category, target_count, unit, icon) VALUES ('Pull Up', 'Back & Biceps', 30, 'reps', 'pullup')");
            db.execSQL("INSERT INTO exercises (name, category, target_count, unit, icon) VALUES ('Bodyweight Squat', 'Legs & Glutes', 150, 'reps', 'squat')");
            db.execSQL("INSERT INTO exercises (name, category, target_count, unit, icon) VALUES ('Plank', 'Core & Abs', 180, 'seconds', 'plank')");
            db.execSQL("INSERT INTO exercises (name, category, target_count, unit, icon) VALUES ('Dips', 'Triceps & Chest', 50, 'reps', 'dips')");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS logs");
            db.execSQL("DROP TABLE IF EXISTS exercises");
            onCreate(db);
        }
    }

    public class WebAppInterface {
        Context context;

        WebAppInterface(Context c) {
            context = c;
        }

        @JavascriptInterface
        public String getTodaySummary() {
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                String query = "SELECT e.id, e.name, e.category, e.target_count, e.unit, e.icon, " +
                        "COALESCE(SUM(l.count), 0) as today_done " +
                        "FROM exercises e " +
                        "LEFT JOIN logs l ON e.id = l.exercise_id AND l.date = ? " +
                        "GROUP BY e.id " +
                        "ORDER BY e.id ASC";

                Cursor cursor = db.rawQuery(query, new String[]{today});
                JSONArray array = new JSONArray();

                while (cursor.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", cursor.getInt(0));
                    obj.put("name", cursor.getString(1));
                    obj.put("category", cursor.getString(2));
                    obj.put("target_count", cursor.getInt(3));
                    obj.put("unit", cursor.getString(4));
                    obj.put("icon", cursor.getString(5));
                    obj.put("today_done", cursor.getInt(6));
                    array.put(obj);
                }
                cursor.close();
                return array.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public boolean addLog(int exerciseId, int count, String notes) {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                ContentValues values = new ContentValues();
                values.put("exercise_id", exerciseId);
                values.put("count", count);
                values.put("date", today);
                values.put("notes", notes != null ? notes : "");
                long res = db.insert("logs", null, values);

                vibrate();
                return res != -1;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean addExercise(String name, String category, int targetCount, String unit, String icon) {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("category", category);
                values.put("target_count", targetCount);
                values.put("unit", unit);
                values.put("icon", icon != null ? icon : "dumbbell");
                long res = db.insert("exercises", null, values);
                return res != -1;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean updateExercise(int id, String name, String category, int targetCount, String unit) {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("name", name);
                values.put("category", category);
                values.put("target_count", targetCount);
                values.put("unit", unit);
                int rows = db.update("exercises", values, "id = ?", new String[]{String.valueOf(id)});
                vibrate();
                return rows > 0;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean deleteExercise(int id) {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("logs", "exercise_id = ?", new String[]{String.valueOf(id)});
                int rows = db.delete("exercises", "id = ?", new String[]{String.valueOf(id)});
                return rows > 0;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public String getRecentHistory(int limit) {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String query = "SELECT l.id, e.name, l.count, e.unit, l.date, l.created_at, l.notes " +
                        "FROM logs l " +
                        "JOIN exercises e ON l.exercise_id = e.id " +
                        "ORDER BY l.id DESC LIMIT ?";
                Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
                JSONArray array = new JSONArray();

                while (cursor.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", cursor.getInt(0));
                    obj.put("name", cursor.getString(1));
                    obj.put("count", cursor.getInt(2));
                    obj.put("unit", cursor.getString(3));
                    obj.put("date", cursor.getString(4));
                    obj.put("created_at", cursor.getString(5));
                    obj.put("notes", cursor.getString(6));
                    array.put(obj);
                }
                cursor.close();
                return array.toString();
            } catch (Exception e) {
                return "[]";
            }
        }

        @JavascriptInterface
        public String getStats() {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                Cursor cToday = db.rawQuery("SELECT COALESCE(SUM(count), 0) FROM logs WHERE date = ?", new String[]{today});
                int repsToday = 0;
                if (cToday.moveToFirst()) repsToday = cToday.getInt(0);
                cToday.close();

                Cursor cAll = db.rawQuery("SELECT COALESCE(SUM(count), 0), COUNT(DISTINCT date) FROM logs", null);
                int totalReps = 0;
                int activeDays = 0;
                if (cAll.moveToFirst()) {
                    totalReps = cAll.getInt(0);
                    activeDays = cAll.getInt(1);
                }
                cAll.close();

                JSONObject res = new JSONObject();
                res.put("reps_today", repsToday);
                res.put("reps_all_time", totalReps);
                res.put("active_days", activeDays);
                res.put("today_date", today);
                return res.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public String getMonthlyDashboard(String yearMonth) {
            try {
                // If yearMonth is empty, use current month (yyyy-MM)
                if (yearMonth == null || yearMonth.isEmpty()) {
                    yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
                }

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                JSONObject root = new JSONObject();
                root.put("month", yearMonth);

                // 1. Total reps and active workout days this month
                String qSummary = "SELECT COALESCE(SUM(count), 0), COUNT(DISTINCT date) " +
                                  "FROM logs WHERE date LIKE ?";
                Cursor cSum = db.rawQuery(qSummary, new String[]{yearMonth + "%"});
                int monthlyReps = 0;
                int monthlyActiveDays = 0;
                if (cSum.moveToFirst()) {
                    monthlyReps = cSum.getInt(0);
                    monthlyActiveDays = cSum.getInt(1);
                }
                cSum.close();
                root.put("monthly_reps", monthlyReps);
                root.put("active_days", monthlyActiveDays);

                // 2. Daily breakdown for heatmap / calendar frequency (days 1 to 31)
                String qDays = "SELECT date, SUM(count) as total_reps, COUNT(id) as sessions " +
                               "FROM logs WHERE date LIKE ? GROUP BY date ORDER BY date ASC";
                Cursor cDays = db.rawQuery(qDays, new String[]{yearMonth + "%"});
                JSONObject dailyMap = new JSONObject();
                while (cDays.moveToNext()) {
                    String d = cDays.getString(0);
                    int reps = cDays.getInt(1);
                    dailyMap.put(d, reps);
                }
                cDays.close();
                root.put("daily_data", dailyMap);

                // 3. Exercise distribution this month
                String qEx = "SELECT e.name, SUM(l.count), e.unit " +
                             "FROM logs l JOIN exercises e ON l.exercise_id = e.id " +
                             "WHERE l.date LIKE ? GROUP BY e.id ORDER BY SUM(l.count) DESC";
                Cursor cEx = db.rawQuery(qEx, new String[]{yearMonth + "%"});
                JSONArray exList = new JSONArray();
                while (cEx.moveToNext()) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", cEx.getString(0));
                    obj.put("reps", cEx.getInt(1));
                    obj.put("unit", cEx.getString(2));
                    exList.put(obj);
                }
                cEx.close();
                root.put("exercise_breakdown", exList);

                // 4. Workout Frequency Score / Consistency Rate
                // Calculate total days passed in month so far
                Calendar cal = Calendar.getInstance();
                int currentDay = cal.get(Calendar.DAY_OF_MONTH);
                int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                root.put("days_in_month", daysInMonth);
                root.put("current_day", currentDay);

                return root.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public int getLocalUiVersion() {
            return prefs.getInt(PREF_VERSION_KEY, 1);
        }

        @JavascriptInterface
        public void checkAndUpdateUi() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    performOtaCheck();
                }
            }).start();
        }

        @JavascriptInterface
        public void triggerVibration() {
            vibrate();
        }

        private void vibrate() {
            try {
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(45);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void performOtaCheck() {
        try {
            notifyWeb("Pemeriksaan update...", false);
            URL url = new URL(GITHUB_VERSION_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int remoteVersion = json.getInt("version");
                int localVersion = prefs.getInt(PREF_VERSION_KEY, 1);

                if (remoteVersion > localVersion) {
                    notifyWeb("Mengunduh update v" + remoteVersion + "...", false);
                    String bundleUrl = json.getString("bundle_url");
                    boolean success = downloadAndExtractBundle(bundleUrl);
                    if (success) {
                        prefs.edit().putInt(PREF_VERSION_KEY, remoteVersion).apply();
                        notifyWeb("Sukses! Memuat versi baru...", true);
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                loadLocalUi();
                            }
                        }, 700);
                    } else {
                        notifyWeb("Gagal mengekstrak update.", false);
                    }
                } else {
                    notifyWeb("Aplikasi sudah versi terbaru (v" + localVersion + ")", false);
                }
            } else {
                notifyWeb("Server pembaruan tidak merespon (" + conn.getResponseCode() + ")", false);
            }
        } catch (Exception e) {
            notifyWeb("Koneksi gagal atau offline.", false);
        }
    }

    private boolean downloadAndExtractBundle(String downloadUrl) {
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            InputStream is = conn.getInputStream();

            File wwwDir = new File(getFilesDir(), "www");
            if (!wwwDir.exists()) wwwDir.mkdirs();

            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;
            byte[] buffer = new byte[2048];

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(wwwDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    FileOutputStream fos = new FileOutputStream(file);
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, count);
                    }
                    fos.close();
                }
                zis.closeEntry();
            }
            zis.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void notifyWeb(final String msg, final boolean refresh) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (webView != null) {
                    webView.evaluateJavascript("if (window.showOtaToast) { window.showOtaToast('" + msg.replace("'", "\\'") + "'); }", null);
                }
            }
        });
    }

    private void ensureLocalFiles() {
        File indexFile = new File(getFilesDir(), "www/index.html");
        if (!indexFile.exists()) {
            try {
                File wwwDir = new File(getFilesDir(), "www");
                if (!wwwDir.exists()) wwwDir.mkdirs();

                InputStream is = getAssets().open("index.html");
                FileOutputStream fos = new FileOutputStream(indexFile);
                byte[] buffer = new byte[2048];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                is.close();
            } catch (Exception ignored) {}
        }
    }

    private void loadLocalUi() {
        File indexFile = new File(getFilesDir(), "www/index.html");
        if (indexFile.exists()) {
            webView.loadUrl("file://" + indexFile.getAbsolutePath());
        } else {
            webView.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#090A0F"));

        prefs = getSharedPreferences("ironpulse_prefs", Context.MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);

        ensureLocalFiles();

        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.parseColor("#090A0F"));

        webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setBackgroundColor(Color.parseColor("#090A0F"));
        webView.addJavascriptInterface(new WebAppInterface(this), "NativeDB");

        loadLocalUi();

        container.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        setContentView(container);
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
