package com.harness.habits;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Persists habits as JSON inside SharedPreferences. */
public class HabitStore {

    private static final String PREFS = "habits";
    private static final String KEY = "list";

    private final SharedPreferences prefs;

    public HabitStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<Habit> load() {
        List<Habit> out = new ArrayList<Habit>();
        String raw = prefs.getString(KEY, null);
        if (raw == null) {
            return seed();
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Habit h = new Habit(
                        o.optString("id", "h" + i),
                        o.optString("name", "Habit"));
                JSONArray ds = o.optJSONArray("days");
                if (ds != null) {
                    for (int j = 0; j < ds.length(); j++) {
                        h.days.add(ds.getString(j));
                    }
                }
                out.add(h);
            }
        } catch (Exception e) {
            // Corrupt payload: start clean rather than crash on launch.
            return new ArrayList<Habit>();
        }
        return out;
    }

    public void save(List<Habit> habits) {
        JSONArray arr = new JSONArray();
        try {
            for (Habit h : habits) {
                JSONObject o = new JSONObject();
                o.put("id", h.id);
                o.put("name", h.name);
                o.put("days", new JSONArray(h.days));
                arr.put(o);
            }
        } catch (Exception e) {
            return;
        }
        prefs.edit().putString(KEY, arr.toString()).apply();
    }

    /** First launch only: a few sensible habits, no fake history. */
    private List<Habit> seed() {
        List<Habit> out = new ArrayList<Habit>();
        out.add(new Habit("Drink water"));
        out.add(new Habit("Read 10 pages"));
        out.add(new Habit("Move for 10 minutes"));
        save(out);
        return out;
    }
}
