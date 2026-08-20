package kr.co.conwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class TrashActivity extends Activity {
    private LinearLayout listBox;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.prepareWindow(this);
        setContentView(buildUi());
    }

    @Override protected void onResume() {
        super.onResume();
        renderList();
    }

    private ScrollView buildUi() {
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
        TextView title = Ui.text(this, "휴지통", 27, Ui.colorText());
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        TextView desc = Ui.text(this, "삭제한 기프티콘은 30일 동안 복구할 수 있어요", 13, Ui.colorSecondary());
        titleCol.addView(title);
        titleCol.addView(desc);
        header.addView(titleCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        listLp.topMargin = Ui.dp(this, 16);
        root.addView(listBox, listLp);
        return scroll;
    }

    private void renderList() {
        if (listBox == null) return;
        listBox.removeAllViews();
        List<Gifticon> items = GifticonDb.get(this).trash();
        if (items.isEmpty()) {
            TextView empty = Ui.text(this, "휴지통이 비어 있어요", 16, Ui.colorSecondary());
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 40), 0, Ui.dp(this, 40));
            listBox.addView(empty);
            return;
        }

        for (Gifticon g : items) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            Ui.card(card, this);

            TextView title = Ui.text(this, g.title, 17, Ui.colorText());
            title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
            card.addView(title);

            String brand = g.brand == null || g.brand.trim().isEmpty() ? "브랜드 없음" : g.brand;
            TextView meta = Ui.text(this, brand, 12, Ui.colorSecondary());
            meta.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
            card.addView(meta);

            LinearLayout buttons = new LinearLayout(this);
            Button restore = new Button(this);
            restore.setText("복구");
            Ui.stylePrimaryButton(restore, this);
            restore.setOnClickListener(v -> {
                GifticonDb.get(this).restore(g.id);
                Gifticon restored = GifticonDb.get(this).getById(g.id);
                NotificationHelper.schedule(this, restored);
                Toast.makeText(this, "복구했습니다.", Toast.LENGTH_SHORT).show();
                renderList();
            });

            Button delete = new Button(this);
            delete.setText("완전 삭제");
            Ui.styleDangerButton(delete, this);
            delete.setOnClickListener(v -> confirmPermanentDelete(g));

            LinearLayout.LayoutParams restoreLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            restoreLp.rightMargin = Ui.dp(this, 8);
            buttons.addView(restore, restoreLp);
            buttons.addView(delete, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            card.addView(buttons);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = Ui.dp(this, 10);
            listBox.addView(card, cardLp);
        }
    }

    private void confirmPermanentDelete(Gifticon g) {
        new AlertDialog.Builder(this)
                .setTitle("완전 삭제")
                .setMessage("이 기프티콘을 완전히 삭제할까요? 되돌릴 수 없습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("삭제", (d, w) -> {
                    ImageStore.delete(g.imagePath);
                    GifticonDb.get(this).delete(g.id);
                    renderList();
                })
                .show();
    }
}
