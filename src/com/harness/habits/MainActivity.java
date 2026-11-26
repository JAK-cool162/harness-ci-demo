package com.harness.habits;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements HabitRowView.Listener {

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private HabitStore store;
    private List<Habit> habits = new ArrayList<Habit>();
    private final Map<String, HabitRowView> rows = new HashMap<String, HabitRowView>();

    private LinearLayout listContainer;
    private TextView subtitle;
    private float density;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        store = new HabitStore(this);
        habits = store.load();
        setContentView(buildUi());
        renderList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The seven-day window may have rolled over while the app was away.
        for (HabitRowView row : rows.values()) {
            row.invalidate();
        }
        updateSubtitle();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private int color(int id) {
        return getResources().getColor(id, getTheme());
    }

    private int accentColor() {
        return color(R.color.accent);
    }

    /* ---------------------------------------------------------------- UI */

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.bg));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(26), dp(22), dp(8));

        TextView title = new TextView(this);
        title.setText(R.string.screen_title);
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setTextColor(color(R.color.text));
        header.addView(title);

        subtitle = new TextView(this);
        subtitle.setTextSize(14);
        subtitle.setTextColor(color(R.color.muted));
        LinearLayout.LayoutParams subLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(5);
        header.addView(subtitle, subLp);

        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(dp(18), dp(8), dp(18), dp(12));
        scroll.addView(listContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(18), dp(8), dp(18), dp(22));

        Button add = new Button(this);
        add.setText(R.string.add_habit);
        add.setAllCaps(false);
        add.setTextSize(16);
        add.setTextColor(Color.WHITE);
        add.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        GradientDrawable addBg = rounded(accentColor(), 16);
        add.setBackground(addBg);
        add.setStateListAnimator(null);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });
        bar.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return root;
    }

    private GradientDrawable rounded(int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    /* ------------------------------------------------------------- list */

    private void renderList() {
        listContainer.removeAllViews();
        rows.clear();

        if (habits.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.empty_hint);
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(color(R.color.muted));
            empty.setLineSpacing(dp(5), 1f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(56);
            lp.leftMargin = dp(18);
            lp.rightMargin = dp(18);
            listContainer.addView(empty, lp);
        } else {
            for (Habit habit : habits) {
                HabitRowView row = new HabitRowView(this);
                row.setHabit(habit);
                row.setListener(this);

                GradientDrawable card = rounded(color(R.color.card), 20);
                card.setStroke(dp(1), color(R.color.hairline));
                row.setBackground(card);
                if (Build.VERSION.SDK_INT >= 21) {
                    row.setElevation(dp(1));
                }

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(10);
                listContainer.addView(row, lp);
                rows.put(habit.id, row);
            }
        }
        updateSubtitle();
    }

    private void updateSubtitle() {
        if (habits.isEmpty()) {
            subtitle.setText(R.string.sub_empty);
            return;
        }
        String today = FMT.format(Calendar.getInstance().getTime());
        int done = 0;
        for (Habit h : habits) {
            if (h.isDone(today)) {
                done++;
            }
        }
        subtitle.setText(getString(R.string.sub_progress, done, habits.size()));
    }

    /* --------------------------------------------------------- listeners */

    @Override
    public void onToggleDay(Habit habit, String dayKey) {
        habit.toggle(dayKey);
        store.save(habits);
        HabitRowView row = rows.get(habit.id);
        if (row != null) {
            row.invalidate();
        }
        updateSubtitle();
    }

    @Override
    public void onLongPressHabit(final Habit habit) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_msg, habit.name))
                .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        habits.remove(habit);
                        store.save(habits);
                        renderList();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showAddDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_name);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setSingleLine(true);

        FrameLayout wrap = new FrameLayout(this);
        wrap.setPadding(dp(22), dp(6), dp(22), 0);
        wrap.addView(input, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_title)
                .setView(wrap)
                .setPositiveButton(R.string.dialog_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            habits.add(new Habit(name));
                            store.save(habits);
                            renderList();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();
        input.requestFocus();
    }
}
