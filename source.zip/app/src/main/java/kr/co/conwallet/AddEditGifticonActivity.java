package kr.co.conwallet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;

public class AddEditGifticonActivity extends Activity {
    private static final int PICK_IMAGE = 101;
    private ImageView image;
    private EditText title, brand, memo;
    private Switch hasExpiry, notify;
    private Button dateButton, saveButton;
    private TextView analyzeStatus, barcodeInfo;
    private Uri selectedImageUri;
    private Long selectedExpiry;
    private Gifticon existing;
    private String analyzedBarcode, analyzedSymbology;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra("id");
        if (id != null) existing = GifticonDb.get(this).getById(id);
        setTitle(existing == null ? "기프티콘 추가" : "기프티콘 수정");
        setContentView(buildUi());
        if (existing != null) populate(existing);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 16), Ui.dp(this, 18), Ui.dp(this, 28));
        root.setBackgroundColor(Ui.colorBg());
        scroll.addView(root);

        TextView heading = Ui.text(this, existing == null ? "새 기프티콘" : "기프티콘 수정", 26, Ui.colorText());
        heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        root.addView(heading);

        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackground(Ui.rounded(Color.rgb(232,234,240), 18, this));
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 220));
        imageLp.topMargin = Ui.dp(this, 16);
        root.addView(image, imageLp);

        Button choose = new Button(this); choose.setText("사진에서 기프티콘 선택");
        choose.setOnClickListener(v -> pickImage());
        root.addView(choose);

        analyzeStatus = Ui.text(this, "사진을 선택하면 상품명·브랜드·유효기간·바코드를 자동으로 읽습니다.", 12, Ui.colorSecondary());
        root.addView(analyzeStatus);

        root.addView(label("상품명"));
        title = edit("예: 아이스 아메리카노 Tall"); root.addView(title);
        root.addView(label("브랜드"));
        brand = edit("예: 스타벅스"); root.addView(brand);

        hasExpiry = new Switch(this); hasExpiry.setText("유효기간 있음");
        root.addView(hasExpiry);
        dateButton = new Button(this); dateButton.setText("유효기간 선택");
        dateButton.setOnClickListener(v -> showDatePicker());
        root.addView(dateButton);
        notify = new Switch(this); notify.setText("만료 알림 사용"); notify.setChecked(true);
        root.addView(notify);
        hasExpiry.setOnCheckedChangeListener((b, checked) -> {
            dateButton.setEnabled(checked);
            notify.setEnabled(checked);
            if (checked && selectedExpiry == null) setDefaultExpiry();
        });
        dateButton.setEnabled(false); notify.setEnabled(false);

        barcodeInfo = Ui.text(this, "바코드/QR: 감지되지 않음", 12, Ui.colorSecondary());
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barLp.topMargin = Ui.dp(this, 10);
        root.addView(barcodeInfo, barLp);

        root.addView(label("메모"));
        memo = edit("필요한 메모를 적어두세요");
        memo.setMinLines(3); memo.setSingleLine(false); root.addView(memo);

        LinearLayout buttons = new LinearLayout(this); buttons.setGravity(Gravity.END);
        Button cancel = new Button(this); cancel.setText("취소"); cancel.setOnClickListener(v -> finish());
        saveButton = new Button(this); saveButton.setText("저장"); saveButton.setOnClickListener(v -> save());
        buttons.addView(cancel); buttons.addView(saveButton);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = Ui.dp(this, 16); root.addView(buttons, btnLp);
        return scroll;
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 13, Ui.colorSecondary());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14); t.setLayoutParams(lp); return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextSize(16); e.setBackground(Ui.rounded(Color.WHITE, 10, this));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10)); return e;
    }

    private void populate(Gifticon g) {
        title.setText(g.title); brand.setText(g.brand); memo.setText(g.memo);
        selectedExpiry = g.expiryDate;
        hasExpiry.setChecked(g.expiryDate != null);
        notify.setChecked(g.notificationsEnabled);
        analyzedBarcode = g.barcodePayload; analyzedSymbology = g.barcodeSymbology;
        updateDateButton(); updateBarcodeText();
        if (g.imagePath != null) loadImagePath(g.imagePath);
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selectedImageUri = data.getData();
        try { getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        loadImageUri(selectedImageUri);
        analyzeStatus.setText("이미지 분석 중…");
        saveButton.setEnabled(false);
        GifticonImageAnalyzer.analyze(this, selectedImageUri, new GifticonImageAnalyzer.Callback() {
            @Override public void onSuccess(GifticonImageAnalyzer.Analysis a) {
                runOnUiThread(() -> {
                    if (a.inferredTitle != null && !a.inferredTitle.trim().isEmpty()) title.setText(a.inferredTitle);
                    if (a.inferredBrand != null) brand.setText(a.inferredBrand);
                    if (a.inferredExpiryDate != null) {
                        selectedExpiry = a.inferredExpiryDate;
                        hasExpiry.setChecked(true);
                        updateDateButton();
                    }
                    analyzedBarcode = a.barcodePayload; analyzedSymbology = a.barcodeSymbology;
                    updateBarcodeText();
                    analyzeStatus.setText("자동 인식 완료. 잘못 읽은 값은 저장 전에 수정하세요.");
                    saveButton.setEnabled(true);
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> {
                    analyzeStatus.setText("자동 인식 실패: 직접 입력해 주세요.");
                    saveButton.setEnabled(true);
                });
            }
        });
    }

    private void setDefaultExpiry() {
        Calendar c = Calendar.getInstance(); c.add(Calendar.MONTH, 1);
        selectedExpiry = DateUtil.endOfDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        updateDateButton();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedExpiry != null) c.setTimeInMillis(selectedExpiry);
        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedExpiry = DateUtil.endOfDay(y, m, d); updateDateButton();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton() { dateButton.setText(selectedExpiry == null ? "유효기간 선택" : DateUtil.shortDate(selectedExpiry)); }
    private void updateBarcodeText() {
        barcodeInfo.setText(analyzedBarcode == null || analyzedBarcode.isEmpty() ? "바코드/QR: 감지되지 않음" :
                "바코드/QR: " + (analyzedSymbology == null ? "" : analyzedSymbology + " · ") + analyzedBarcode);
    }

    private void save() {
        String t = title.getText().toString().trim();
        if (t.isEmpty()) { Toast.makeText(this, "상품명을 입력해 주세요.", Toast.LENGTH_SHORT).show(); return; }
        Gifticon g = existing == null ? new Gifticon() : existing;
        String oldPath = g.imagePath;
        try {
            if (selectedImageUri != null) {
                String newPath = ImageStore.saveFromUri(this, selectedImageUri);
                g.imagePath = newPath;
                if (oldPath != null && !oldPath.equals(newPath)) ImageStore.delete(oldPath);
            }
            g.title = t;
            g.brand = brand.getText().toString().trim();
            g.memo = memo.getText().toString().trim();
            g.expiryDate = hasExpiry.isChecked() ? selectedExpiry : null;
            g.notificationsEnabled = hasExpiry.isChecked() && notify.isChecked();
            g.barcodePayload = analyzedBarcode;
            g.barcodeSymbology = analyzedSymbology;
            g.updatedAt = System.currentTimeMillis();
            GifticonDb.get(this).save(g);
            NotificationHelper.schedule(this, g);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadImagePath(String path) {
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize = 2;
        Bitmap b = BitmapFactory.decodeFile(path, o); if (b != null) image.setImageBitmap(b);
    }

    private void loadImageUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap b = BitmapFactory.decodeStream(in); image.setImageBitmap(b);
        } catch (Exception ignored) { image.setImageURI(uri); }
    }
}
