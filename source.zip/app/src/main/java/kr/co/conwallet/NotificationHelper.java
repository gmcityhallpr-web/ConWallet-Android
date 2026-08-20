package kr.co.conwallet;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "gifticon_expiry";
    public static final String EXTRA_ID = "gifticon_id";
    public static final String EXTRA_OFFSET = "offset_days";

    private NotificationHelper() {}

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "디지털폐지수집 만료 알림", NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("디지털폐지수집에 저장한 기프티콘의 유효기간이 가까워지면 알려줍니다.");
            nm.createNotificationChannel(ch);
        }
    }

    public static void rescheduleAll(Context c) {
        for (Gifticon g : GifticonDb.get(c).all()) schedule(c, g);
    }

    public static void schedule(Context c, Gifticon g) {
        if (g == null) return;
        cancel(c, g.id);
        if (g.isUsed || g.deletedAt != null || !g.notificationsEnabled || g.expiryDate == null) return;
        for (Integer offset : NotificationPrefs.enabledOffsets(c)) {
            long when = notificationTime(g.expiryDate, offset);
            if (when <= System.currentTimeMillis()) continue;
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(c, NotificationReceiver.class)
                    .putExtra(EXTRA_ID, g.id)
                    .putExtra(EXTRA_OFFSET, offset);
            PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(g.id, offset), intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    public static void cancel(Context c, String id) {
        if (id == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int offset : new int[]{30, 7, 3, 1, 0}) {
            Intent intent = new Intent(c, NotificationReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, requestCode(id, offset), intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }

    private static long notificationTime(long expiryMillis, int offsetDays) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(expiryMillis);
        c.add(Calendar.DAY_OF_MONTH, -offsetDays);
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int requestCode(String id, int offset) {
        return (id + ":" + offset).hashCode() & 0x7fffffff;
    }
}
