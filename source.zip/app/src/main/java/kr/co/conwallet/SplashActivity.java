package kr.co.conwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
        image.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        int baseSize = Ui.dp(this, 112);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(baseSize, baseSize, Gravity.CENTER);
        root.addView(image, lp);
        setContentView(root);

        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            private boolean started = false;

            @Override
            public boolean onPreDraw() {
                if (started) return true;
                started = true;
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                startLaunchAnimation(image, baseSize);
                return true;
            }
        });
    }

    private void startLaunchAnimation(ImageView image, int baseSize) {
        DisplayMetrics dm = getResources().getDisplayMetrics();

        float maxWidthPx = dm.widthPixels * 0.90f;
        float maxHeightPx = dm.heightPixels * 0.68f;
        float fullScale = Math.min(maxWidthPx / baseSize, maxHeightPx / baseSize);

        float targetScale = Ui.dp(this, 158) / (float) baseSize;
        float targetDown = Ui.dp(this, 42);

        ObjectAnimator growX = ObjectAnimator.ofFloat(image, View.SCALE_X, 1.0f, fullScale);
        ObjectAnimator growY = ObjectAnimator.ofFloat(image, View.SCALE_Y, 1.0f, fullScale);

        AnimatorSet grow = new AnimatorSet();
        grow.playTogether(growX, growY);
        grow.setDuration(330);
        grow.setInterpolator(new DecelerateInterpolator(1.45f));

        ObjectAnimator settleX = ObjectAnimator.ofFloat(image, View.SCALE_X, fullScale, targetScale);
        ObjectAnimator settleY = ObjectAnimator.ofFloat(image, View.SCALE_Y, fullScale, targetScale);
        ObjectAnimator settleDown = ObjectAnimator.ofFloat(image, View.TRANSLATION_Y, 0f, targetDown);

        AnimatorSet settle = new AnimatorSet();
        settle.playTogether(settleX, settleY, settleDown);
        settle.setDuration(260);
        settle.setInterpolator(new OvershootInterpolator(1.15f));

        AnimatorSet sequence = new AnimatorSet();
        sequence.playSequentially(grow, settle);
        sequence.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                image.setLayerType(View.LAYER_TYPE_NONE, null);
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });
        sequence.start();
    }
}
