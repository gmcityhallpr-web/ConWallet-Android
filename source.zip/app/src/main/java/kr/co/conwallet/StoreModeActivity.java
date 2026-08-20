package kr.co.conwallet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import java.io.File;

public class StoreModeActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams lp = getWindow().getAttributes(); lp.screenBrightness = 1.0f; getWindow().setAttributes(lp);
        getWindow().setStatusBarColor(Color.BLACK); getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        String id = getIntent().getStringExtra("id"); Gifticon g = id == null ? null : GifticonDb.get(this).getById(id);
        if (g == null || g.imagePath == null || !new File(g.imagePath).exists()) { finish(); return; }
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        ZoomImageView image = new ZoomImageView(this); root.addView(image, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize=1; Bitmap b=BitmapFactory.decodeFile(g.imagePath,o); image.setImageBitmap(b);
        Button close = new Button(this); close.setText("닫기");
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.END); cp.setMargins(0,Ui.dp(this,18),Ui.dp(this,18),0); root.addView(close,cp); close.setOnClickListener(v->finish());
        setContentView(root);
    }
}
