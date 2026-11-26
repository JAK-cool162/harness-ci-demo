package com.harness.habits;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * One habit card: name, streak, and a rolling strip of the last seven days.
 * Drawn by hand so the week strip stays perfectly aligned at any width.
 */
public class HabitRowView extends View {

    public interface Listener {
        void onToggleDay(Habit habit, String dayKey);
        void onLongPressHabit(Habit habit);
    }

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    /** Indexed by Calendar.DAY_OF_WEEK - 1 (Sunday first). */
    private static final String[] LETTERS = {"S", "M", "T", "W", "T", "F", "S"};

    private Habit habit;
    private Listener listener;

    private final TextPaint namePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint metaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path checkPath = new Path();

    private int padH, padV, nameArea, gap, labelArea, labelGap, circleD;
    private float radius, cy, labelBase;
    private final float[] colX = new float[7];
    private final String[] keys = new String[7];
    private final String[] labels = new String[7];
    private final boolean[] done = new boolean[7];
    private final boolean[] isToday = new boolean[7];

    private int touchSlop;
    private float downX, downY;
    private boolean longFired;

    private final Runnable longPressRunnable = new Runnable() {
        @Override
        public void run() {
            if (habit == null || listener == null) {
                return;
            }
            longFired = true;
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            listener.onLongPressHabit(habit);
        }
    };

    public HabitRowView(Context context) {
        super(context);
        float den = getResources().getDisplayMetrics().density;
        float spx = getResources().getDisplayMetrics().scaledDensity;

        padH = Math.round(18 * den);
        padV = Math.round(16 * den);
        nameArea = Math.round(24 * den);
        gap = Math.round(13 * den);
        labelArea = Math.round(11 * den);
        labelGap = Math.round(6 * den);
        circleD = Math.round(30 * den);

        namePaint.setTextSize(17 * spx);
        namePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        namePaint.setColor(color(R.color.text));

        metaPaint.setTextSize(12.5f * spx);
        metaPaint.setColor(color(R.color.muted));
        metaPaint.setTextAlign(Paint.Align.RIGHT);

        labelPaint.setTextSize(10.5f * spx);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(color(R.color.muted));

        shapePaint.setStrokeWidth(2 * den);

        checkPaint.setStyle(Paint.Style.STROKE);
        checkPaint.setStrokeWidth(2.4f * den);
        checkPaint.setStrokeCap(Paint.Cap.ROUND);
        checkPaint.setStrokeJoin(Paint.Join.ROUND);
        checkPaint.setColor(0xFFFFFFFF);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);
    }

    private int color(int id) {
        return getResources().getColor(id, getContext().getTheme());
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    public void setHabit(Habit habit) {
        this.habit = habit;
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Recomputes the rolling seven-day window. Cheap enough to run per draw. */
    private void refreshWindow() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            keys[i] = FMT.format(c.getTime());
            labels[i] = LETTERS[c.get(Calendar.DAY_OF_WEEK) - 1];
            done[i] = habit.isDone(keys[i]);
            isToday[i] = (i == 6);
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private String metaText() {
        int s = habit.streak();
        if (s <= 0) {
            return "start today";
        }
        if (s == 1) {
            return "1 day streak";
        }
        return s + " day streak";
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = padV * 2 + nameArea + gap + labelArea + labelGap + circleD;
        setMeasuredDimension(resolveSize(w, widthSpec), resolveSize(h, heightSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (habit == null) {
            return;
        }
        refreshWindow();

        int w = getWidth();
        float avail = w - padH * 2f;
        float colW = avail / 7f;
        float nameBaseline = padV + nameArea * 0.74f;

        int accent = color(R.color.accent);
        int muted = color(R.color.muted);

        String meta = metaText();
        float metaW = metaPaint.measureText(meta);
        metaPaint.setColor(habit.streak() > 0 ? accent : muted);
        canvas.drawText(meta, w - padH, nameBaseline, metaPaint);

        float maxName = avail - metaW - dp(14);
        CharSequence name = TextUtils.ellipsize(
                habit.name, namePaint, Math.max(dp(40), maxName), TextUtils.TruncateAt.END);
        canvas.drawText(name.toString(), padH, nameBaseline, namePaint);

        labelBase = padV + nameArea + gap + labelArea * 0.85f;
        cy = padV + nameArea + gap + labelArea + labelGap + circleD / 2f;
        radius = circleD / 2f;

        int ring = color(R.color.ring);
        int soft = color(R.color.accent_soft);

        for (int i = 0; i < 7; i++) {
            colX[i] = padH + colW * i + colW / 2f;

            labelPaint.setColor(isToday[i] ? accent : muted);
            canvas.drawText(labels[i], colX[i], labelBase, labelPaint);

            if (done[i]) {
                shapePaint.setStyle(Paint.Style.FILL);
                shapePaint.setColor(accent);
                canvas.drawCircle(colX[i], cy, radius, shapePaint);

                checkPath.reset();
                checkPath.moveTo(colX[i] - radius * 0.40f, cy + radius * 0.02f);
                checkPath.lineTo(colX[i] - radius * 0.10f, cy + radius * 0.34f);
                checkPath.lineTo(colX[i] + radius * 0.44f, cy - radius * 0.30f);
                canvas.drawPath(checkPath, checkPaint);
            } else {
                if (isToday[i]) {
                    shapePaint.setStyle(Paint.Style.FILL);
                    shapePaint.setColor(soft);
                    canvas.drawCircle(colX[i], cy, radius, shapePaint);
                }
                shapePaint.setStyle(Paint.Style.STROKE);
                shapePaint.setStrokeWidth(dp(2));
                shapePaint.setColor(isToday[i] ? accent : ring);
                canvas.drawCircle(colX[i], cy, radius - dp(1), shapePaint);
            }
        }
    }

    /** Column under the finger, or -1 for the text area. */
    private int columnAt(float x, float y) {
        if (y < cy - radius - dp(6)) {
            return -1;
        }
        for (int i = 0; i < 7; i++) {
            if (Math.abs(x - colX[i]) <= radius + dp(4)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                longFired = false;
                removeCallbacks(longPressRunnable);
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > touchSlop
                        || Math.abs(e.getY() - downY) > touchSlop) {
                    removeCallbacks(longPressRunnable);
                }
                return true;

            case MotionEvent.ACTION_UP:
                removeCallbacks(longPressRunnable);
                if (!longFired && habit != null && listener != null) {
                    int idx = columnAt(e.getX(), e.getY());
                    // Tapping the card body checks off today.
                    listener.onToggleDay(habit, keys[idx >= 0 ? idx : 6]);
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPressRunnable);
                return true;

            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(longPressRunnable);
        super.onDetachedFromWindow();
    }
}
