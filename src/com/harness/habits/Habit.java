package com.harness.habits;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** A single habit and the set of days (yyyy-MM-dd) it was completed. */
public class Habit {

    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    public String id;
    public String name;
    public Set<String> days = new HashSet<String>();

    public Habit(String name) {
        // Millisecond ids collide when several habits are created in one loop.
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public Habit(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public boolean isDone(String key) {
        return days.contains(key);
    }

    public void toggle(String key) {
        if (!days.remove(key)) {
            days.add(key);
        }
    }

    /**
     * Consecutive completed days ending today. If today is not completed yet,
     * the streak is measured up to yesterday, so checking in later still counts.
     */
    public int streak() {
        Calendar c = Calendar.getInstance();
        String today = FMT.format(c.getTime());
        if (!days.contains(today)) {
            c.add(Calendar.DAY_OF_YEAR, -1);
            if (!days.contains(FMT.format(c.getTime()))) {
                return 0;
            }
        }
        int n = 0;
        while (days.contains(FMT.format(c.getTime()))) {
            n++;
            c.add(Calendar.DAY_OF_YEAR, -1);
            if (n > 3650) {
                break;
            }
        }
        return n;
    }
}
