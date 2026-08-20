package kr.co.conwallet;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class NotificationPrefs {
    private static final String PREF = "notification_prefs";
    public static final String D30 = "notify30Days";
    public static final String D7 = "notify7Days";
    public static final String D1 = "notify1Day";

    private NotificationPrefs() {}

    public static void ensureDefaults(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!p.contains(D30)) p.edit().putBoolean(D30, true).putBoolean(D7, true).putBoolean(D1, true).apply();
    }

    public static boolean get(Context c, String key) {
        ensureDefaults(c);
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(key, true);
    }

    public static void set(Context c, String key, boolean value) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static List<Integer> enabledOffsets(Context c) {
        List<Integer> out = new ArrayList<>();
        if (get(c, D30)) out.add(30);
        if (get(c, D7)) out.add(7);
        if (get(c, D1)) out.add(1);
        return out;
    }
}
