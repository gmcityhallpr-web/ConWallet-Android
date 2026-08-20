package kr.co.conwallet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;

import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        splashScreen.setOnExitAnimationListener(provider -> {
            provider.getView()
                    .animate()
                    .scaleX(1.16f)
                    .scaleY(1.16f)
                    .setDuration(220)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .withEndAction(() -> {
                        provider.remove();
                        startActivity(new Intent(this, MainActivity.class));
                        overridePendingTransition(0, 0);
                        finish();
                    })
                    .start();
        });
    }
}
