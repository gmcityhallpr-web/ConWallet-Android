package kr.co.conwallet;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public final class NotificationPrefs {
    private static final String PREF = "notification_prefs";
    public static final String D30 = "notify30Days";
    public static final String D7 = "notify7Days";
    public static final String D3 = "notify3Days";
    public static final String D1 = "notify1Day";
    public static final String D0 = "notify0Day";

    private NotificationPrefs() {}

    public static void ensureDefaults(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit();
        if (!p.contains(D30)) e.putBoolean(D30, true);
        if (!p.contains(D7)) e.putBoolean(D7, true);
        if (!p.contains(D3)) e.putBoolean(D3, true);
        if (!p.contains(D1)) e.putBoolean(D1, true);
        if (!p.contains(D0)) e.putBoolean(D0, true);
        e.apply();
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
        if (get(c, D3)) out.add(3);
        if (get(c, D1)) out.add(1);
        if (get(c, D0)) out.add(0);
        return out;
    }
}
