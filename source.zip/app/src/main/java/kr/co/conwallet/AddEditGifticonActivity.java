package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.Calendar;

public class AddEditGifticonActivity extends Activity {
    private static final int PICK_IMAGE = 101;
    private ImageView image;
    private TextView imageHint;
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
        Ui.prepareWindow(this);
        String id = getIntent().getStringExtra("id");
        if (id != null) existing = GifticonDb.get(this).getById(id);
        setTitle(existing == null ? "기프티콘 추가" : "기프티콘 수정");
        setContentView(buildUi());
        if (existing != null) populate(existing);
    }

    private View buildUi() {
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
        TextView heading = Ui.text(this, existing == null ? "새 기프티콘" : "기프티콘 수정", 27, Ui.colorText());
        heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        TextView desc = Ui.text(this, existing == null ? "사진을 고르면 정보를 자동으로 읽어드려요" : "사진을 누르면 새 이미지로 바꿀 수 있어요", 13, Ui.colorSecondary());
        titleCol.addView(heading);
        titleCol.addView(desc);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        // 사진 선택 영역 자체를 버튼처럼 사용합니다. 16:9 비율이라 배너 이미지는 1200x675 권장.
        FrameLayout imagePicker = new FrameLayout(this);
        imagePicker.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 20, this));
        imagePicker.setClipToOutline(true);
        imagePicker.setClickable(true);
        imagePicker.setFocusable(true);
        imagePicker.setContentDescription("사진에서 기프티콘 선택");
        imagePicker.setOnClickListener(v -> pickImage());

        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setClickable(false);
        imagePicker.addView(image, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        imageHint = Ui.text(this, "사진에서 기프티콘 선택\n눌러서 사진 고르기", 16, Ui.colorSecondary());
        imageHint.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        imageHint.setGravity(Gravity.CENTER);
        imageHint.setClickable(false);
        imageHint.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18));
        imagePicker.addView(imageHint, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        ));

        LinearLayout.LayoutParams imagePickerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, 210)
        );
        imagePickerLp.topMargin = Ui.dp(this, 16);
        root.addView(imagePicker, imagePickerLp);

        // 화면 폭에 맞춰 실제 표시 영역을 정확히 16:9로 맞춥니다.
        imagePicker.post(() -> {
            int width = imagePicker.getWidth();
            if (width <= 0) return;
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) imagePicker.getLayoutParams();
            int targetHeight = Math.round(width * 9f / 16f);
            if (lp.height != targetHeight) {
                lp.height = targetHeight;
                imagePicker.setLayoutParams(lp);
            }
        });

        analyzeStatus = Ui.text(this, "위 이미지를 누르면 상품명 · 브랜드 · 유효기간 · 바코드를 자동으로 읽어요.", 12, Ui.colorSecondary());
        analyzeStatus.setPadding(Ui.dp(this, 13), Ui.dp(this, 11), Ui.dp(this, 13), Ui.dp(this, 11));
        analyzeStatus.setBackground(Ui.rounded(Ui.colorBrandSoft(), 14, this));
        LinearLayout.LayoutParams analyzeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        analyzeLp.topMargin = Ui.dp(this, 10);
        root.addView(analyzeStatus, analyzeLp);

        LinearLayout infoCard = card("기본 정보");
        infoCard.addView(label("상품명"));
        title = edit("예: 아이스 아메리카노 Tall");
        infoCard.addView(title);
        infoCard.addView(label("브랜드"));
        brand = edit("예: 스타벅스");
        infoCard.addView(brand);
        addCard(root, infoCard);

        LinearLayout expiryCard = card("유효기간과 알림");
        hasExpiry = new Switch(this);
        hasExpiry.setText("유효기간 있음");
        hasExpiry.setTextColor(Ui.colorText());
        hasExpiry.setTextSize(15);
        expiryCard.addView(hasExpiry);
        dateButton = new Button(this);
        dateButton.setText("유효기간 선택");
        Ui.styleSecondaryButton(dateButton, this);
        dateButton.setOnClickListener(v -> showDatePicker());
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dateLp.topMargin = Ui.dp(this, 8);
        expiryCard.addView(dateButton, dateLp);
        notify = new Switch(this);
        notify.setText("만료 알림 사용");
        notify.setTextColor(Ui.colorText());
        notify.setTextSize(15);
        notify.setChecked(true);
        LinearLayout.LayoutParams notifyLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        notifyLp.topMargin = Ui.dp(this, 8);
        expiryCard.addView(notify, notifyLp);
        hasExpiry.setOnCheckedChangeListener((b, checked) -> {
            dateButton.setEnabled(checked);
            notify.setEnabled(checked);
            if (checked && selectedExpiry == null) setDefaultExpiry();
        });
        dateButton.setEnabled(false);
        notify.setEnabled(false);
        addCard(root, expiryCard);

        barcodeInfo = Ui.text(this, "바코드/QR · 감지되지 않음", 12, Ui.colorSecondary());
        barcodeInfo.setPadding(Ui.dp(this, 13), Ui.dp(this, 11), Ui.dp(this, 13), Ui.dp(this, 11));
        barcodeInfo.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 14, this));
        LinearLayout.LayoutParams barcodeLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        barcodeLp.topMargin = Ui.dp(this, 14);
        root.addView(barcodeInfo, barcodeLp);

        LinearLayout memoCard = card("메모");
        memo = edit("필요한 메모를 적어두세요");
        memo.setMinLines(3);
        memo.setSingleLine(false);
        memoCard.addView(memo);
        addCard(root, memoCard);

        LinearLayout buttons = new LinearLayout(this);
        Button cancel = new Button(this);
        cancel.setText("취소");
        Ui.styleSecondaryButton(cancel, this);
        cancel.setOnClickListener(v -> finish());
        saveButton = new Button(this);
        saveButton.setText("저장");
        Ui.stylePrimaryButton(saveButton, this);
        saveButton.setOnClickListener(v -> save());
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cancelLp.rightMargin = Ui.dp(this, 8);
        buttons.addView(cancel, cancelLp);
        buttons.addView(saveButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = Ui.dp(this, 16);
        root.addView(buttons, btnLp);
        return scroll;
    }

    private LinearLayout card(String heading) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        Ui.card(box, this);
        TextView t = Ui.text(this, heading, 17, Ui.colorText());
        t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        t.setPadding(0, 0, 0, Ui.dp(this, 8));
        box.addView(t);
        return box;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14);
        root.addView(card, lp);
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 12, Ui.colorSecondary());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 8);
        lp.bottomMargin = Ui.dp(this, 6);
        t.setLayoutParams(lp);
        return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Ui.colorSecondary());
        e.setTextColor(Ui.colorText());
        e.setTextSize(15);
        e.setBackground(Ui.roundedStroke(Ui.colorSurface(), Ui.colorDivider(), 13, this));
        e.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        return e;
    }

    private void populate(Gifticon g) {
        title.setText(g.title);
        brand.setText(g.brand);
        memo.setText(g.memo);
        selectedExpiry = g.expiryDate;
        hasExpiry.setChecked(g.expiryDate != null);
        notify.setChecked(g.notificationsEnabled);
        analyzedBarcode = g.barcodePayload;
        analyzedSymbology = g.barcodeSymbology;
        updateDateButton();
        updateBarcodeText();
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
        try {
            getContentResolver().takePersistableUriPermission(selectedImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
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
                    analyzedBarcode = a.barcodePayload;
                    analyzedSymbology = a.barcodeSymbology;
                    updateBarcodeText();
                    Gifticon duplicate = GifticonDb.get(AddEditGifticonActivity.this)
                            .findActiveByBarcode(analyzedBarcode, existing == null ? null : existing.id);
                    analyzeStatus.setText(duplicate == null
                            ? "자동 인식 완료 · 잘못 읽은 값은 저장 전에 수정하세요."
                            : "⚠ 같은 바코드가 이미 있어요 · " + duplicate.title);
                    saveButton.setEnabled(true);
                });
            }
            @Override public void onError(Exception e) {
                runOnUiThread(() -> {
                    analyzeStatus.setText("자동 인식에 실패했어요 · 필요한 정보만 직접 입력해 주세요.");
                    saveButton.setEnabled(true);
                });
            }
        });
    }

    private void setDefaultExpiry() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 1);
        selectedExpiry = DateUtil.endOfDay(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        updateDateButton();
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedExpiry != null) c.setTimeInMillis(selectedExpiry);
        new DatePickerDialog(this, (view, y, m, d) -> {
            selectedExpiry = DateUtil.endOfDay(y, m, d);
            updateDateButton();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton() {
        dateButton.setText(selectedExpiry == null ? "유효기간 선택" : DateUtil.shortDate(selectedExpiry));
    }

    private void updateBarcodeText() {
        barcodeInfo.setText(analyzedBarcode == null || analyzedBarcode.isEmpty()
                ? "바코드/QR · 감지되지 않음"
                : "바코드/QR · " + (analyzedSymbology == null ? "" : analyzedSymbology + " · ") + analyzedBarcode);
    }

    private void save() {
        String t = title.getText().toString().trim();
        if (t.isEmpty()) {
            Toast.makeText(this, "상품명을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        Gifticon duplicate = GifticonDb.get(this).findActiveByBarcode(analyzedBarcode, existing == null ? null : existing.id);
        if (duplicate != null) {
            new AlertDialog.Builder(this)
                    .setTitle("중복 기프티콘 확인")
                    .setMessage("같은 바코드의 ‘" + duplicate.title + "’이 이미 있어요. 그래도 저장할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("그래도 저장", (d, w) -> doSave(t))
                    .show();
        } else {
            doSave(t);
        }
    }

    private void doSave(String t) {
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
            g.deletedAt = null;
            GifticonDb.get(this).save(g);
            NotificationHelper.schedule(this, g);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadImagePath(String path) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 2;
        Bitmap b = BitmapFactory.decodeFile(path, o);
        if (b != null) {
            image.setImageBitmap(b);
            hideImageHint();
        }
    }

    private void loadImageUri(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap b = BitmapFactory.decodeStream(in);
            image.setImageBitmap(b);
        } catch (Exception ignored) {
            image.setImageURI(uri);
        }
        hideImageHint();
    }

    private void hideImageHint() {
        if (imageHint != null) imageHint.setVisibility(View.GONE);
    }
}
