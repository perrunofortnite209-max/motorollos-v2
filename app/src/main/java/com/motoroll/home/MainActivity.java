package com.motoroll.home;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Motoroll Home 2.0
 * A deliberately lightweight HOME replacement for Motoroll OS.
 * No network, background service, blur, live wallpaper, or third-party UI library.
 */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 7, 13);
    private static final int PANEL = Color.rgb(15, 18, 32);
    private static final int PANEL_RAISED = Color.rgb(22, 25, 44);
    private static final int TEXT = Color.rgb(246, 247, 255);
    private static final int MUTED = Color.rgb(145, 153, 177);
    private static final int BLUE = Color.rgb(62, 126, 255);
    private static final int CYAN = Color.rgb(91, 190, 255);
    private static final int PURPLE = Color.rgb(145, 91, 255);
    private static final int LINE = Color.rgb(37, 43, 70);

    private static final String[] HOME_PACKAGE_PRIORITY = {
            "com.google.android.apps.photos",
            "com.google.android.youtube",
            "com.android.vending",
            "com.android.settings",
            "com.google.android.gm",
            "com.google.android.apps.maps",
            "com.google.android.calendar",
            "com.google.android.keep"
    };

    private static final String[][] DOCK_PACKAGE_PRIORITY = {
            {"com.google.android.dialer", "com.android.dialer"},
            {"com.google.android.apps.messaging", "com.android.mms"},
            {"com.android.chrome"},
            {"com.motorola.camera5"}
    };

    private final List<ResolveInfo> launchableApps = new ArrayList<>();
    private GridView homeGrid;
    private GridView drawerGrid;
    private EditText drawerSearch;
    private boolean drawerOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        loadApps();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        if (drawerOpen && drawerSearch != null) {
            refreshDrawerGrid(drawerSearch.getText() == null ? "" : drawerSearch.getText().toString());
        } else if (homeGrid != null) {
            refreshHomeGrid();
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 30 && window.getInsetsController() != null) {
            window.getInsetsController().setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            window.getDecorView().setSystemUiVisibility(0);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams lp(int widthDp, int heightDp) {
        int width = widthDp < 0 ? widthDp : dp(widthDp);
        int height = heightDp < 0 ? heightDp : dp(heightDp);
        return new LinearLayout.LayoutParams(width, height);
    }

    private GradientDrawable rounded(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        view.setTypeface(android.graphics.Typeface.create(
                bold ? "sans-serif" : "sans-serif", bold
                        ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        return view;
    }

    private void applySafeInsets(View content) {
        final int horizontal = dp(22);
        content.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(horizontal + left, dp(14) + top,
                    horizontal + right, dp(13) + bottom);
            return insets;
        });
        content.requestApplyInsets();
    }

    private FrameLayout newRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(new AmbientBackground(this), new FrameLayout.LayoutParams(-1, -1));
        return root;
    }

    private void loadApps() {
        launchableApps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> found = pm.queryIntentActivities(query, 0);
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo info : found) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName)) continue;
            if (seenPackages.add(packageName)) launchableApps.add(info);
        }
        Collator collator = Collator.getInstance();
        Collections.sort(launchableApps, (a, b) -> collator.compare(
                String.valueOf(a.loadLabel(pm)), String.valueOf(b.loadLabel(pm))));
    }

    private void showHome() {
        drawerOpen = false;
        drawerSearch = null;
        drawerGrid = null;
        homeGrid = null;

        FrameLayout root = newRoot();
        LinearLayout shell = column();
        applySafeInsets(shell);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        shell.addView(buildTopBar(), lp(-1, 48));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content = column();
        content.setClipToPadding(false);
        content.setPadding(0, dp(27), 0, dp(14));
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView eyebrow = text("TU ESPACIO · TU FIRMA", 10, CYAN, true);
        eyebrow.setLetterSpacing(0.16f);
        content.addView(eyebrow);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("h:mm");
        clock.setTextColor(TEXT);
        clock.setTextSize(68);
        clock.setTypeface(android.graphics.Typeface.create("sans-serif-light", 0));
        LinearLayout.LayoutParams clockParams = lp(-2, -2);
        clockParams.topMargin = dp(7);
        content.addView(clock, clockParams);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d 'de' MMMM");
        date.setFormat12Hour("EEEE, d 'de' MMMM");
        date.setTextColor(MUTED);
        date.setTextSize(13);
        LinearLayout.LayoutParams dateParams = lp(-2, -2);
        dateParams.topMargin = dp(2);
        content.addView(date, dateParams);

        content.addView(buildSignatureCard(), topMargin(lp(-1, 82), 22));
        content.addView(buildSearchPill(), topMargin(lp(-1, 52), 17));

        LinearLayout section = row();
        TextView favorites = text("APPS FRECUENTES", 11, TEXT, true);
        favorites.setLetterSpacing(0.08f);
        section.addView(favorites, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView all = text("VER TODAS  ↗", 10, CYAN, true);
        all.setLetterSpacing(0.04f);
        section.addView(all);
        all.setOnClickListener(v -> showDrawer(""));
        content.addView(section, topMargin(lp(-1, -2), 25));

        homeGrid = new GridView(this);
        configureGrid(homeGrid);
        homeGrid.setNumColumns(4);
        homeGrid.setVerticalSpacing(dp(2));
        homeGrid.setAdapter(new AppGridAdapter(getHomeFavorites()));
        content.addView(homeGrid, topMargin(lp(-1, 192), 12));

        shell.addView(buildDock(), lp(-1, 78));
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = row();
        MotorollMark mark = new MotorollMark(this);
        bar.addView(mark, lp(39, 39));

        LinearLayout labels = column();
        labels.setGravity(Gravity.CENTER_VERTICAL);
        labels.setPadding(dp(11), 0, 0, 0);
        TextView title = text("MOTOROLL", 15, TEXT, true);
        title.setLetterSpacing(0.12f);
        TextView subtitle = text("OS  /  PERSONAL EDITION", 8, MUTED, true);
        subtitle.setLetterSpacing(0.09f);
        labels.addView(title);
        LinearLayout.LayoutParams sub = lp(-2, -2);
        sub.topMargin = dp(3);
        labels.addView(subtitle, sub);
        bar.addView(labels, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView settings = text("⋯", 24, TEXT, false);
        settings.setGravity(Gravity.CENTER);
        settings.setBackground(rounded(PANEL_RAISED, 18, LINE, 1));
        bar.addView(settings, lp(43, 43));
        settings.setOnClickListener(v -> openSettings());
        return bar;
    }

    private View buildSignatureCard() {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(11), dp(15), dp(11));
        card.setBackground(gradient(Color.rgb(20, 22, 42), Color.rgb(15, 20, 39), 22));
        card.setForeground(rounded(Color.TRANSPARENT, 22, Color.rgb(47, 49, 83), 1));

        LinearLayout copy = column();
        TextView overline = text("MOTOROLL OS", 9, CYAN, true);
        overline.setLetterSpacing(0.14f);
        TextView headline = text("Ligero. Fluido. Tuyo.", 14, TEXT, true);
        TextView detail = text("Diseñado para tu día a día", 10, MUTED, false);
        copy.addView(overline);
        copy.addView(headline, topMargin(lp(-1, -2), 4));
        copy.addView(detail, topMargin(lp(-1, -2), 3));
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

        MotorollMark mark = new MotorollMark(this);
        card.addView(mark, lp(54, 54));
        return card;
    }

    private View buildSearchPill() {
        LinearLayout pill = row();
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(16), 0, dp(16), 0);
        pill.setBackground(rounded(PANEL, 19, LINE, 1));
        TextView searchIcon = text("⌕", 25, CYAN, false);
        pill.addView(searchIcon, lp(30, -2));
        TextView hint = text("Buscar aplicaciones", 13, MUTED, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, -2, 1f);
        hp.leftMargin = dp(8);
        pill.addView(hint, hp);
        TextView shortcut = text("↗", 16, PURPLE, true);
        pill.addView(shortcut);
        pill.setOnClickListener(v -> showDrawer(""));
        return pill;
    }

    private LinearLayout buildDock() {
        LinearLayout dock = row();
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(7), dp(7), dp(7), dp(7));
        dock.setBackground(rounded(Color.rgb(13, 16, 29), 25, Color.rgb(42, 45, 75), 1));

        List<ResolveInfo> selected = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (String[] candidates : DOCK_PACKAGE_PRIORITY) {
            ResolveInfo info = firstMatchingPackage(candidates);
            if (info != null && added.add(info.activityInfo.packageName)) selected.add(info);
        }
        for (ResolveInfo info : selected) dock.addView(createDockApp(info), new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout all = column();
        all.setGravity(Gravity.CENTER);
        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(gradient(BLUE, PURPLE, 17));
        GridMark dots = new GridMark(this);
        FrameLayout.LayoutParams dotsParams = new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER);
        iconBox.addView(dots, dotsParams);
        all.addView(iconBox, lp(47, 43));
        TextView label = text("Apps", 9, MUTED, true);
        label.setGravity(Gravity.CENTER);
        all.addView(label, topMargin(lp(-1, 15), 2));
        all.setOnClickListener(v -> showDrawer(""));
        dock.addView(all, new LinearLayout.LayoutParams(0, -1, 1f));
        return dock;
    }

    private View createDockApp(ResolveInfo info) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        FrameLayout iconBox = new FrameLayout(this);
        iconBox.setBackground(rounded(Color.rgb(25, 29, 49), 17, Color.rgb(45, 51, 81), 1));
        ImageView icon = new ImageView(this);
        try {
            icon.setImageDrawable(info.loadIcon(getPackageManager()));
        } catch (Exception ignored) { }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconBox.addView(icon, new FrameLayout.LayoutParams(dp(27), dp(27), Gravity.CENTER));
        item.addView(iconBox, lp(47, 43));
        TextView label = text(shortLabel(info), 9, MUTED, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        item.addView(label, topMargin(lp(-1, 15), 2));
        item.setOnClickListener(v -> launchApp(info));
        return item;
    }

    private ResolveInfo firstMatchingPackage(String[] packages) {
        for (String packageName : packages) {
            for (ResolveInfo info : launchableApps) {
                if (packageName.equals(info.activityInfo.packageName)) return info;
            }
        }
        return null;
    }

    private void refreshHomeGrid() {
        if (homeGrid == null) return;
        homeGrid.setAdapter(new AppGridAdapter(getHomeFavorites()));
    }

    private List<ResolveInfo> getHomeFavorites() {
        List<ResolveInfo> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (String packageName : HOME_PACKAGE_PRIORITY) {
            ResolveInfo info = firstMatchingPackage(new String[]{packageName});
            if (info != null && used.add(info.activityInfo.packageName)) result.add(info);
        }
        for (ResolveInfo info : launchableApps) {
            if (result.size() >= 8) break;
            if (used.add(info.activityInfo.packageName)) result.add(info);
        }
        return result;
    }

    private void configureGrid(GridView grid) {
        grid.setNumColumns(4);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(1));
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setVerticalScrollBarEnabled(false);
        grid.setOverScrollMode(View.OVER_SCROLL_NEVER);
        grid.setCacheColorHint(Color.TRANSPARENT);
    }

    private final class AppGridAdapter extends BaseAdapter {
        private final List<ResolveInfo> items;

        AppGridAdapter(List<ResolveInfo> apps) {
            items = new ArrayList<>(apps);
        }

        @Override public int getCount() { return items.size(); }
        @Override public ResolveInfo getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            LinearLayout cell;
            AppCellHolder holder;
            if (convertView == null) {
                cell = column();
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(3), dp(2), dp(3));
                cell.setMinimumHeight(dp(93));

                FrameLayout iconBox = new FrameLayout(MainActivity.this);
                iconBox.setBackground(rounded(Color.rgb(18, 22, 38), 17, Color.rgb(39, 44, 71), 1));
                ImageView icon = new ImageView(MainActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconBox.addView(icon, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
                cell.addView(iconBox, lp(58, 55));

                TextView label = text("", 10, TEXT, false);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(1);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                cell.addView(label, topMargin(lp(-1, -2), 7));

                holder = new AppCellHolder(icon, label);
                cell.setTag(holder);
            } else {
                cell = (LinearLayout) convertView;
                holder = (AppCellHolder) cell.getTag();
            }

            ResolveInfo info = getItem(position);
            holder.label.setText(shortLabel(info));
            try {
                holder.icon.setImageDrawable(info.loadIcon(getPackageManager()));
            } catch (Exception ignored) {
                holder.icon.setImageDrawable(null);
            }
            cell.setContentDescription(shortLabel(info));
            cell.setOnClickListener(v -> launchApp(info));
            return cell;
        }
    }

    private static final class AppCellHolder {
        final ImageView icon;
        final TextView label;
        AppCellHolder(ImageView icon, TextView label) {
            this.icon = icon;
            this.label = label;
        }
    }

    private String shortLabel(ResolveInfo info) {
        String label = String.valueOf(info.loadLabel(getPackageManager()));
        if (label.length() > 13) return label.substring(0, 12).trim() + "…";
        return label;
    }

    private void showDrawer(String initialQuery) {
        drawerOpen = true;
        homeGrid = null;
        FrameLayout root = newRoot();
        LinearLayout shell = column();
        applySafeInsets(shell);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        LinearLayout header = row();
        TextView back = text("‹", 34, CYAN, false);
        back.setGravity(Gravity.CENTER);
        header.addView(back, lp(40, 44));
        back.setOnClickListener(v -> showHome());
        LinearLayout titles = column();
        TextView title = text("Aplicaciones", 20, TEXT, true);
        TextView subtitle = text("MOTOROLL APP LIBRARY", 8, MUTED, true);
        subtitle.setLetterSpacing(0.12f);
        titles.addView(title);
        titles.addView(subtitle, topMargin(lp(-2, -2), 3));
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        shell.addView(header, lp(-1, 48));

        drawerSearch = new EditText(this);
        drawerSearch.setSingleLine(true);
        drawerSearch.setTextSize(14);
        drawerSearch.setTextColor(TEXT);
        drawerSearch.setHintTextColor(MUTED);
        drawerSearch.setHint("Buscar una aplicación");
        drawerSearch.setPadding(dp(16), 0, dp(16), 0);
        drawerSearch.setBackground(rounded(PANEL, 18, LINE, 1));
        if (initialQuery != null && !initialQuery.isEmpty()) drawerSearch.setText(initialQuery);
        shell.addView(drawerSearch, topMargin(lp(-1, 51), 16));

        TextView count = text(launchableApps.size() + " aplicaciones", 10, MUTED, false);
        shell.addView(count, topMargin(lp(-1, 19), 17));

        drawerGrid = new GridView(this);
        configureGrid(drawerGrid);
        drawerGrid.setVerticalSpacing(dp(4));
        shell.addView(drawerGrid, topMargin(new LinearLayout.LayoutParams(-1, 0, 1f), 9));
        refreshDrawerGrid(initialQuery == null ? "" : initialQuery);

        drawerSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshDrawerGrid(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void refreshDrawerGrid(String query) {
        if (drawerGrid == null) return;
        String filter = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo info : launchableApps) {
            String label = String.valueOf(info.loadLabel(getPackageManager()));
            if (filter.isEmpty() || label.toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(info);
            }
        }
        drawerGrid.setAdapter(new AppGridAdapter(filtered));
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams params, int marginDp) {
        params.topMargin = dp(marginDp);
        return params;
    }

    private void launchApp(ResolveInfo info) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir la aplicación", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Ajustes no está disponible", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) {
            showHome();
            return;
        }
        super.onBackPressed();
    }

    private class AmbientBackground extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RadialGradient blueGlow;
        private RadialGradient purpleGlow;

        AmbientBackground(Activity context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            float blueRadius = Math.max(width, height) * 0.57f;
            float purpleRadius = Math.max(width, height) * 0.48f;
            blueGlow = new RadialGradient(width * 0.96f, height * 0.18f, blueRadius,
                    new int[]{0x282F62B8, 0x1014214D, 0x0005070D},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
            purpleGlow = new RadialGradient(width * 0.02f, height * 0.76f, purpleRadius,
                    new int[]{0x252F1E65, 0x10180F36, 0x0005070D},
                    new float[]{0f, 0.56f, 1f}, Shader.TileMode.CLAMP);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(BG);
            if (blueGlow != null) {
                paint.setShader(blueGlow);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
            if (purpleGlow != null) {
                paint.setShader(purpleGlow);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
            paint.setShader(null);
        }
    }

    private class MotorollMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MotorollMark(Activity context) { super(context); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.43f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setShader(new android.graphics.LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    new int[]{CYAN, BLUE, PURPLE}, null, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setStrokeWidth(dp(2.6f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setShader(new android.graphics.LinearGradient(
                    cx - radius, cy, cx + radius, cy,
                    CYAN, PURPLE, Shader.TileMode.CLAMP));
            Path path = new Path();
            path.moveTo(cx - radius * 0.57f, cy - radius * 0.20f);
            path.lineTo(cx - radius * 0.27f, cy + radius * 0.29f);
            path.lineTo(cx, cy - radius * 0.02f);
            path.lineTo(cx + radius * 0.27f, cy + radius * 0.29f);
            path.lineTo(cx + radius * 0.57f, cy - radius * 0.20f);
            canvas.drawPath(path, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private class GridMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        GridMark(Activity context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float side = Math.min(getWidth(), getHeight()) * 0.17f;
            float gap = side * 0.55f;
            float total = side * 3 + gap * 2;
            float left = (getWidth() - total) / 2f;
            float top = (getHeight() - total) / 2f;
            paint.setColor(Color.WHITE);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    float x = left + col * (side + gap);
                    float y = top + row * (side + gap);
                    canvas.drawRoundRect(x, y, x + side, y + side, side * 0.25f, side * 0.25f, paint);
                }
            }
        }
    }
}
