package kr.co.conwallet;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import java.util.Calendar;

public final class NotificationHelper {
    public static final String CHANNEL_ID = "gifticon_expiry";
    // v2를 써서 예전에 만들어진 채널 설정과 섞이지 않고 HIGH 중요도로 새로 생성되게 합니다.
    public static final String URGENT_CHANNEL_ID = "gifticon_urgent_v2";
    public static final String EXTRA_ID = "gifticon_id";
    public static final String EXTRA_OFFSET = "offset_days";

    private NotificationHelper() {}

    public static void createChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel normal = new NotificationChannel(
                    CHANNEL_ID, "디지털폐지수집 만료 알림", NotificationManager.IMPORTANCE_DEFAULT);
            normal.setDescription("디지털폐지수집에 저장한 기프티콘의 유효기간이 가까워지면 알려줍니다.");
            nm.createNotificationChannel(normal);

            NotificationChannel urgent = new NotificationChannel(
                    URGENT_CHANNEL_ID, "디지털폐지수집 임박 즉시 알림", NotificationManager.IMPORTANCE_HIGH);
            urgent.setDescription("7일 이내에 만료되는 기프티콘을 새로 등록하면 즉시 알려줍니다.");
            urgent.enableVibration(true);
            urgent.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(urgent);
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

    public static void notifyIfUrgentNow(Context c, Gifticon g) {
        if (g == null || g.expiryDate == null || !g.notificationsEnabled || g.isUsed || g.deletedAt != null) return;
        Integer days = g.daysUntilExpiry();
        if (days == null || days < 0 || days > 7) return;

        String label = g.brand == null || g.brand.trim().isEmpty()
                ? g.title
                : g.brand.trim() + " · " + g.title;
        String body;
        if (days == 0) body = "오늘 만료돼요. 지금 확인해보세요.";
        else if (days == 1) body = "내일 만료돼요. 잊지 말고 사용하세요.";
        else body = "유효기간이 " + days + "일 남았어요. 잊기 전에 사용하세요.";

        // 앱 화면을 보고 있는 순간에도 즉시 반응을 확인할 수 있게 토스트를 함께 표시합니다.
        Toast.makeText(c, "⚠ " + label + " · " + body, Toast.LENGTH_LONG).show();

        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createChannel(c);

        Intent open = new Intent(c, GifticonDetailActivity.class)
                .putExtra("id", g.id)
                .setAction("kr.co.conwallet.URGENT_" + g.id);
        PendingIntent contentIntent = PendingIntent.getActivity(
                c,
                (g.id + ":urgent").hashCode() & 0x7fffffff,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            b = new Notification.Builder(c, URGENT_CHANNEL_ID);
        } else {
            b = new Notification.Builder(c)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
        }

        b.setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("기프티콘 만료 임박 · " + label)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((g.id + ":urgent:v2").hashCode(), b.build());
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
