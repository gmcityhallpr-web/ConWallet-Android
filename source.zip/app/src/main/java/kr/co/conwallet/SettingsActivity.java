package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int EXPORT_JSON = 201;
    private static final int IMPORT_JSON = 202;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.prepareWindow(this);
        setTitle("욱지갑 설정");
        setContentView(buildUi());
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
        TextView title = Ui.text(this, "욱지갑 설정", 27, Ui.colorText());
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        TextView intro = Ui.text(this, "알림과 백업을 한곳에서 관리해요", 13, Ui.colorSecondary());
        titleCol.addView(title);
        titleCol.addView(intro);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        LinearLayout notification = card("만료 알림", "사용하기 전에 미리 알려드려요.");
        notification.addView(toggle("D-30 오전 9시", NotificationPrefs.D30));
        notification.addView(toggle("D-7 오전 9시", NotificationPrefs.D7));
        notification.addView(toggle("D-1 오전 9시", NotificationPrefs.D1));
        TextView notice = Ui.text(this, "배터리 최적화 상태에 따라 알림이 몇 분 늦을 수 있어요.", 12, Ui.colorSecondary());
        notice.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 10));
        notification.addView(notice);
        Button reschedule = new Button(this);
        reschedule.setText("알림 다시 예약");
        Ui.styleSecondaryButton(reschedule, this);
        reschedule.setOnClickListener(v -> {
            NotificationHelper.rescheduleAll(this);
            Toast.makeText(this, "알림을 다시 예약했습니다.", Toast.LENGTH_SHORT).show();
        });
        notification.addView(reschedule);
        addCard(root, notification);

        LinearLayout backup = card("백업 · 복원", "기프티콘 이미지까지 JSON 파일 하나에 담아요.");
        TextView compatible = Ui.text(this, "iPhone 욱지갑과 같은 백업 형식을 사용합니다.", 13, Ui.colorSecondary());
        compatible.setPadding(0, 0, 0, Ui.dp(this, 12));
        backup.addView(compatible);
        LinearLayout backupButtons = new LinearLayout(this);
        Button export = new Button(this);
        export.setText("백업 내보내기");
        Ui.stylePrimaryButton(export, this);
        export.setOnClickListener(v -> exportBackup());
        Button restore = new Button(this);
        restore.setText("백업 불러오기");
        Ui.styleSecondaryButton(restore, this);
        restore.setOnClickListener(v -> importBackup());
        LinearLayout.LayoutParams exportLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        exportLp.rightMargin = Ui.dp(this, 8);
        backupButtons.addView(export, exportLp);
        backupButtons.addView(restore, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        backup.addView(backupButtons);
        addCard(root, backup);

        LinearLayout data = card("데이터 관리", "필요할 때만 사용하세요. 삭제한 데이터는 되돌릴 수 없습니다.");
        Button deleteAll = new Button(this);
        deleteAll.setText("모든 기프티콘 삭제");
        Ui.styleDangerButton(deleteAll, this);
        deleteAll.setOnClickListener(v -> confirmDeleteAll());
        data.addView(deleteAll);
        addCard(root, data);
        return scroll;
    }

    private LinearLayout card(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        Ui.card(box, this);
        TextView heading = Ui.text(this, title, 18, Ui.colorText());
        heading.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        box.addView(heading);
        TextView desc = Ui.text(this, subtitle, 12, Ui.colorSecondary());
        desc.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        box.addView(desc);
        return box;
    }

    private void addCard(LinearLayout root, View card) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14);
        root.addView(card, lp);
    }

    private Switch toggle(String label, String key) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextColor(Ui.colorText());
        sw.setTextSize(15);
        sw.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        sw.setChecked(NotificationPrefs.get(this, key));
        sw.setOnCheckedChangeListener((button, checked) -> {
            NotificationPrefs.set(this, key, checked);
            NotificationHelper.rescheduleAll(this);
        });
        return sw;
    }

    private void exportBackup() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.KOREA).format(new Date());
        i.putExtra(Intent.EXTRA_TITLE, "wookwallet-backup-" + stamp + ".json");
        startActivityForResult(i, EXPORT_JSON);
    }

    private void importBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        startActivityForResult(i, IMPORT_JSON);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == EXPORT_JSON) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new Exception("파일을 열 수 없습니다.");
                out.write(BackupService.exportJson(this));
                Toast.makeText(this, "백업을 저장했습니다.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "백업 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == IMPORT_JSON) {
            new AlertDialog.Builder(this)
                    .setTitle("백업 불러오기")
                    .setMessage("같은 ID의 기프티콘은 백업 내용으로 업데이트됩니다. 계속할까요?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("불러오기", (d, w) -> {
                        try (InputStream in = getContentResolver().openInputStream(uri)) {
                            if (in == null) throw new Exception("파일을 열 수 없습니다.");
                            int count = BackupService.importJson(this, readAll(in));
                            Toast.makeText(this, count + "개 기프티콘을 불러왔습니다.", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "복원 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }).show();
        }
    }

    private byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("모든 데이터 삭제")
                .setMessage("저장된 모든 기프티콘과 이미지를 삭제합니다. 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("모두 삭제", (d, w) -> {
                    for (Gifticon g : GifticonDb.get(this).all()) {
                        NotificationHelper.cancel(this, g.id);
                        ImageStore.delete(g.imagePath);
                    }
                    GifticonDb.get(this).deleteAll();
                    Toast.makeText(this, "모두 삭제했습니다.", Toast.LENGTH_SHORT).show();
                }).show();
    }
}
