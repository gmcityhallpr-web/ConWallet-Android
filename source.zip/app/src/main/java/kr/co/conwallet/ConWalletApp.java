package kr.co.conwallet;

import android.app.Application;

public class ConWalletApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
        NotificationPrefs.ensureDefaults(this);
    }
}
