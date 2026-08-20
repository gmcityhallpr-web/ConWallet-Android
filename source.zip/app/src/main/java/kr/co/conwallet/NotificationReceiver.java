package kr.co.conwallet;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra(NotificationHelper.EXTRA_ID);
        int offset = intent.getIntExtra(NotificationHelper.EXTRA_OFFSET, -1);
        if (id == null) return;
        Gifticon g = GifticonDb.get(context).getById(id);
        if (g == null || g.deletedAt != null || g.isUsed || !g.notificationsEnabled) return;

        Intent open = new Intent(context, GifticonDetailActivity.class).putExtra("id", id);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id.hashCode() & 0x7fffffff, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String label = g.brand == null || g.brand.trim().isEmpty() ? g.title : g.brand + " · " + g.title;
        String body;
        if (offset == 0) body = "오늘 만료돼요. 잊지 말고 사용하세요.";
        else if (offset == 1) body = "내일 만료돼요. 사용 여부를 확인하세요.";
        else body = "유효기간이 " + offset + "일 남았어요.";

        android.app.Notification.Builder b;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            b = new android.app.Notification.Builder(context, NotificationHelper.CHANNEL_ID);
        } else {
            b = new android.app.Notification.Builder(context);
        }
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(label)
                .setContentText(body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id.hashCode() ^ offset, b.build());
    }
}
