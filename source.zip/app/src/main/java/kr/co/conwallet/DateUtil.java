package kr.co.conwallet;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {
    private DateUtil() {}

    public static String shortDate(Long millis) {
        if (millis == null) return "유효기간 없음";
        return new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(new Date(millis));
    }

    public static String iso(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }

    public static Long parseIso(String s) {
        if (s == null || s.trim().isEmpty() || "null".equals(s)) return null;
        String[] patterns = new String[] {
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                f.setTimeZone(TimeZone.getTimeZone("UTC"));
                return f.parse(s).getTime();
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static long endOfDay(int year, int monthZeroBased, int day) {
        Calendar c = Calendar.getInstance();
        c.set(year, monthZeroBased, day, 23, 59, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    public static int[] ymd(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return new int[]{c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)};
    }
}
