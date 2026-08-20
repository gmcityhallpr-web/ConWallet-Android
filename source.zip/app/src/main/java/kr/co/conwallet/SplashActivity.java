package kr.co.conwallet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.wook_launcher);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);

        // Android 기본 시작 아이콘 크기에서 이어지는 느낌으로 작게 시작합니다.
        image.setScaleX(0.18f);
        image.setScaleY(0.18f);
        image.setAlpha(0.96f);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        );
        root.addView(image, lp);
        setContentView(root);

        // 작게 뜬 이미지를 비율 그대로 빠르고 부드럽게 화면 최대 크기까지 확대합니다.
        root.post(() -> image.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(460)
                .setInterpolator(new DecelerateInterpolator(1.7f))
                .withEndAction(() -> root.postDelayed(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 45))
                .start());
    }
}
