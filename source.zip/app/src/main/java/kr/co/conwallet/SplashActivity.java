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

        // 첫 프레임이 그려지기 직전부터 바로 애니메이션을 시작해서
        // Android 기본 시작 아이콘 → 확대 모션 사이의 '멈칫'을 최대한 없앱니다.
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

        // 본화면의 빈 상태 이미지가 158dp이므로 마지막 크기도 그 크기에 맞춥니다.
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
        // 마지막에 살짝 '딱' 튕겼다가 제자리로 오는 느낌
        settle.setInterpolator(new OvershootInterpolator(1.15f));

        AnimatorSet sequence = new AnimatorSet();
        sequence.playSequentially(grow, settle);
        sequence.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                image.setLayerType(View.LAYER_TYPE_NONE, null);
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                // 본화면과 스플래시 사이를 페이드하지 않아 최종 이미지가 그대로 이어지는 느낌으로 전환
                overridePendingTransition(0, 0);
                finish();
            }
        });
        sequence.start();
    }
}
