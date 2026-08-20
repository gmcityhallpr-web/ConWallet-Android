package kr.co.conwallet;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;

public class StoreModeActivity extends Activity {
    private Gifticon gifticon;
    private Button usedButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        String id = getIntent().getStringExtra("id");
        gifticon = id == null ? null : GifticonDb.get(this).getById(id);
        if (gifticon == null || gifticon.imagePath == null || !new File(gifticon.imagePath).exists()) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ZoomImageView image = new ZoomImageView(this);
        root.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 1;
        Bitmap b = BitmapFactory.decodeFile(gifticon.imagePath, o);
        image.setImageBitmap(b);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        topBar.setBackgroundColor(0xAA000000);

        TextView info = Ui.text(this, titleLine(gifticon), 14, Color.WHITE);
        info.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topBar.addView(info, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button close = new Button(this);
        close.setText("닫기");
        close.setOnClickListener(v -> finish());
        topBar.addView(close);

        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(topBar, topLp);

        usedButton = new Button(this);
        refreshUsedButton();
        usedButton.setOnClickListener(v -> toggleUsed());
        FrameLayout.LayoutParams usedLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 58),
                Gravity.BOTTOM);
        usedLp.setMargins(Ui.dp(this, 18), 0, Ui.dp(this, 18), Ui.dp(this, 22));
        root.addView(usedButton, usedLp);

        setContentView(root);
    }

    private String titleLine(Gifticon g) {
        String brand = g.brand == null ? "" : g.brand.trim();
        String title = g.title == null ? "기프티콘" : g.title;
        return brand.isEmpty() ? title : brand + " · " + title;
    }

    private void refreshUsedButton() {
        if (usedButton == null || gifticon == null) return;
        usedButton.setText(gifticon.isUsed ? "사용 완료 취소" : "✓ 사용 완료");
        if (gifticon.isUsed) Ui.styleSecondaryButton(usedButton, this);
        else Ui.stylePrimaryButton(usedButton, this);
    }

    private void toggleUsed() {
        gifticon.isUsed = !gifticon.isUsed;
        gifticon.usedAt = gifticon.isUsed ? System.currentTimeMillis() : null;
        gifticon.updatedAt = System.currentTimeMillis();
        GifticonDb.get(this).save(gifticon);
        NotificationHelper.schedule(this, gifticon);
        refreshUsedButton();
    }
}
