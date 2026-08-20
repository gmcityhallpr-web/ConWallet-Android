package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class GifticonDetailActivity extends Activity {
    private String id;
    private LinearLayout root;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        id = getIntent().getStringExtra("id");
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        Gifticon g = id == null ? null : GifticonDb.get(this).getById(id);
        if (g == null) { finish(); return; }
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 28));
        root.setBackgroundColor(Ui.colorBg()); scroll.addView(root);

        TextView brand = Ui.text(this, g.brand == null || g.brand.isEmpty() ? "기프티콘" : g.brand, 14, Ui.colorSecondary());
        TextView title = Ui.text(this, g.title, 27, Ui.colorText()); title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(brand); root.addView(title);

        ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackground(Ui.rounded(Color.WHITE, 18, this));
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 330));
        imgLp.topMargin = Ui.dp(this, 14); root.addView(image, imgLp);
        if (g.imagePath != null && new File(g.imagePath).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize = 2;
            Bitmap b = BitmapFactory.decodeFile(g.imagePath, o); if (b != null) image.setImageBitmap(b);
        } else image.setImageResource(android.R.drawable.ic_menu_gallery);

        TextView expiry = Ui.text(this, g.expiryDate == null ? "유효기간 없음" : "유효기간  " + DateUtil.shortDate(g.expiryDate), 16, Ui.colorText());
        TextView status = Ui.text(this, statusLine(g), 14, g.isExpired() ? Color.rgb(190,45,45) : Ui.colorBrand());
        status.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); textLp.topMargin=Ui.dp(this,12);
        root.addView(expiry, textLp); root.addView(status);

        if (g.barcodePayload != null && !g.barcodePayload.isEmpty()) {
            TextView code = Ui.text(this, (g.barcodeSymbology == null ? "코드" : g.barcodeSymbology) + "  " + g.barcodePayload, 14, Ui.colorText());
            code.setTextIsSelectable(true); root.addView(code, textLp);
            Button copy = new Button(this); copy.setText("바코드 번호 복사");
            copy.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("기프티콘 코드", g.barcodePayload));
                Toast.makeText(this, "복사했습니다.", Toast.LENGTH_SHORT).show();
            }); root.addView(copy);
        }

        if (g.memo != null && !g.memo.trim().isEmpty()) {
            TextView memoTitle = Ui.text(this, "메모", 13, Ui.colorSecondary()); root.addView(memoTitle, textLp);
            TextView memo = Ui.text(this, g.memo, 15, Ui.colorText()); memo.setBackground(Ui.rounded(Color.WHITE, 10, this)); memo.setPadding(Ui.dp(this,12),Ui.dp(this,10),Ui.dp(this,12),Ui.dp(this,10)); root.addView(memo);
        }

        Button store = new Button(this); store.setText("매장에서 크게 보기 · 화면 밝기 최대");
        store.setEnabled(g.imagePath != null && new File(g.imagePath).exists());
        store.setOnClickListener(v -> startActivity(new Intent(this, StoreModeActivity.class).putExtra("id", g.id)));
        root.addView(store, textLp);

        Button used = new Button(this); used.setText(g.isUsed ? "사용 완료 취소" : "사용 완료로 표시");
        used.setOnClickListener(v -> toggleUsed(g)); root.addView(used);

        LinearLayout bottom = new LinearLayout(this); bottom.setGravity(Gravity.END);
        Button edit = new Button(this); edit.setText("수정"); edit.setOnClickListener(v -> startActivity(new Intent(this, AddEditGifticonActivity.class).putExtra("id", g.id)));
        Button delete = new Button(this); delete.setText("삭제"); delete.setOnClickListener(v -> confirmDelete(g));
        bottom.addView(edit); bottom.addView(delete); root.addView(bottom, textLp);
        setContentView(scroll);
    }

    private String statusLine(Gifticon g) {
        if (g.isUsed) return "사용 완료";
        if (g.isExpired()) return "기간 만료";
        Integer d = g.daysUntilExpiry();
        if (d == null) return "사용 가능";
        return d == 0 ? "오늘 만료" : "D-" + d + " · 사용 가능";
    }

    private void toggleUsed(Gifticon g) {
        g.isUsed = !g.isUsed;
        g.usedAt = g.isUsed ? System.currentTimeMillis() : null;
        g.updatedAt = System.currentTimeMillis();
        GifticonDb.get(this).save(g); NotificationHelper.schedule(this, g); render();
    }

    private void confirmDelete(Gifticon g) {
        new AlertDialog.Builder(this).setTitle("기프티콘 삭제")
                .setMessage("이 기프티콘과 저장된 이미지를 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d,w) -> {
                    NotificationHelper.cancel(this, g.id); ImageStore.delete(g.imagePath); GifticonDb.get(this).delete(g.id); finish();
                }).show();
    }
}
