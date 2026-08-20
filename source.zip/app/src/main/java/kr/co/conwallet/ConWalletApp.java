package kr.co.conwallet;

import android.app.Application;

public class ConWalletApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        NotificationPrefs.ensureDefaults(this);
        long thirtyDays = 30L * 24L * 60L * 60L * 1000L;
        GifticonDb.get(this).purgeTrashOlderThan(System.currentTimeMillis() - thirtyDays);
    }
}
