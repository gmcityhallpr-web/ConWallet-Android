package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.prepareWindow(this);
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
        scroll.setBackgroundColor(Ui.colorBg());
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 30));
        scroll.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = Ui.actionButton(this, "‹", false);
        back.setTextSize(25);
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 44));
        backLp.rightMargin = Ui.dp(this, 12);
        header.addView(back, backLp);
        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView brand = Ui.text(this, g.brand == null || g.brand.isEmpty() ? "기프티콘" : g.brand, 13, Ui.colorSecondary());
        TextView title = Ui.text(this, g.title, 25, Ui.colorText());
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titleCol.addView(brand);
        titleCol.addView(title);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        ImageView image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        image.setBackground(Ui.rounded(Ui.colorSurface(), 20, this));
        image.setClipToOutline(true);
        image.setElevation(Ui.dp(this, 1.5f));
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 330));
        imgLp.topMargin = Ui.dp(this, 16);
        root.addView(image, imgLp);
        if (g.imagePath != null && new File(g.imagePath).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap b = BitmapFactory.decodeFile(g.imagePath, o);
            if (b != null) image.setImageBitmap(b);
        } else {
            image.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        Ui.card(info, this);
        TextView expiry = Ui.text(this,
                g.expiryDate == null ? "유효기간 없음" : "유효기간 · " + DateUtil.shortDate(g.expiryDate),
                15, Ui.colorText());
        info.addView(expiry);
        TextView status = Ui.text(this, statusLine(g), 12, Ui.colorSuccess());
        status.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        status.setGravity(Gravity.CENTER);
        status.setPadding(Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 5));
        styleStatus(status, g);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = Ui.dp(this, 9);
        info.addView(status, statusLp);
        addCard(root, info);

        if (g.barcodePayload != null && !g.barcodePayload.isEmpty()) {
            LinearLayout codeCard = sectionCard("바코드 / QR");
            TextView code = Ui.text(this,
                    (g.barcodeSymbology == null ? "코드" : g.barcodeSymbology) + " · " + g.barcodePayload,
                    14, Ui.colorText());
            code.setTextIsSelectable(true);
            code.setPadding(0, 0, 0, Ui.dp(this, 10));
            codeCard.addView(code);
            Button copy = new Button(this);
            copy.setText("바코드 번호 복사");
            Ui.styleSecondaryButton(copy, this);
            copy.setOnClickListener(v -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("기프티콘 코드", g.barcodePayload));
                Toast.makeText(this, "복사했습니다.", Toast.LENGTH_SHORT).show();
            });
            codeCard.addView(copy);
            addCard(root, codeCard);
        }

        if (g.memo != null && !g.memo.trim().isEmpty()) {
            LinearLayout memoCard = sectionCard("메모");
            TextView memo = Ui.text(this, g.memo, 15, Ui.colorText());
            memoCard.addView(memo);
            addCard(root, memoCard);
        }

        Button store = new Button(this);
        store.setText("매장에서 크게 보기");
        Ui.stylePrimaryButton(store, this);
        store.setEnabled(g.imagePath != null && new File(g.imagePath).exists());
        store.setOnClickListener(v -> startActivity(new Intent(this, StoreModeActivity.class).putExtra("id", g.id)));
        LinearLayout.LayoutParams storeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        storeLp.topMargin = Ui.dp(this, 16);
        root.addView(store, storeLp);

        Button used = new Button(this);
        used.setText(g.isUsed ? "사용 완료 취소" : "사용 완료로 표시");
        Ui.styleSecondaryButton(used, this);
        used.setOnClickListener(v -> toggleUsed(g));
        LinearLayout.LayoutParams usedLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        usedLp.topMargin = Ui.dp(this, 8);
        root.addView(used, usedLp);

        LinearLayout bottom = new LinearLayout(this);
        Button edit = new Button(this);
        edit.setText("수정");
        Ui.styleSecondaryButton(edit, this);
        edit.setOnClickListener(v -> startActivity(new Intent(this, AddEditGifticonActivity.class).putExtra("id", g.id)));
        Button delete = new Button(this);
        delete.setText("삭제");
        Ui.styleDangerButton(delete, this);
        delete.setOnClickListener(v -> confirmDelete(g));
        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        editLp.rightMargin = Ui.dp(this, 8);
        bottom.addView(edit, editLp);
        bottom.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomLp.topMargin = Ui.dp(this, 8);
        root.addView(bottom, bottomLp);
        setContentView(scroll);
    }

    private LinearLayout sectionCard(String heading) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        Ui.card(box, this);
        TextView t = Ui.text(this, heading, 13, Ui.colorSecondary());
        t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, Ui.dp(this, 8));
        box.addView(t);
        return box;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 12);
        root.addView(card, lp);
    }

    private void styleStatus(TextView status, Gifticon g) {
        Integer d = g.daysUntilExpiry();
        if (g.isUsed) {
            status.setTextColor(Ui.colorSecondary());
            status.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 999, this));
        } else if (g.isExpired()) {
            status.setTextColor(Ui.colorDanger());
            status.setBackground(Ui.rounded(Ui.colorDangerSoft(), 999, this));
        } else if (d != null && d <= 7) {
            status.setTextColor(Ui.colorWarning());
            status.setBackground(Ui.rounded(Ui.colorWarningSoft(), 999, this));
        } else {
            status.setTextColor(Ui.colorSuccess());
            status.setBackground(Ui.rounded(Ui.colorSuccessSoft(), 999, this));
        }
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
        GifticonDb.get(this).save(g);
        NotificationHelper.schedule(this, g);
        render();
    }

    private void confirmDelete(Gifticon g) {
        new AlertDialog.Builder(this)
                .setTitle("기프티콘 삭제")
                .setMessage("이 기프티콘과 저장된 이미지를 삭제할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d, w) -> {
                    NotificationHelper.cancel(this, g.id);
                    ImageStore.delete(g.imagePath);
                    GifticonDb.get(this).delete(g.id);
                    finish();
                }).show();
    }
}
