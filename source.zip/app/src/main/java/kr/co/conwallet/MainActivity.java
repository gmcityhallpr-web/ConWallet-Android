package kr.co.conwallet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private GifticonAdapter adapter;
    private EditText search;
    private TextView statAvailable, statSoon, statTotal, emptyTitle, emptyBody;
    private final TextView[] chips = new TextView[5];
    private int filterIndex = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.prepareWindow(this);
        setContentView(buildUi());
        requestNotificationPermissionIfNeeded();
    }

    @Override protected void onResume() {
        super.onResume();
        reload();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 8));
        root.setBackgroundColor(Ui.colorBg());

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, "욱지갑", 30, Ui.colorText());
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView subtitle = Ui.text(this, "기프티콘, 잊기 전에 챙겨요", 13, Ui.colorSecondary());
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Ui.dp(this, 3);
        heading.addView(title);
        heading.addView(subtitle, subLp);
        header.addView(heading, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView settings = Ui.actionButton(this, "설정", false);
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        settingsLp.rightMargin = Ui.dp(this, 8);
        header.addView(settings, settingsLp);

        TextView add = Ui.actionButton(this, "＋ 추가", true);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddEditGifticonActivity.class)));
        header.addView(add);
        root.addView(header);

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
        Ui.card(stats, this);
        statAvailable = Ui.text(this, "0", 23, Ui.colorText());
        statSoon = Ui.text(this, "0", 23, Ui.colorBrand());
        statTotal = Ui.text(this, "0", 23, Ui.colorText());
        stats.addView(statCell("사용 가능", statAvailable), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statCell("7일 이내", statSoon), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        stats.addView(statCell("전체", statTotal), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statsLp.topMargin = Ui.dp(this, 18);
        root.addView(stats, statsLp);

        search = new EditText(this);
        search.setHint("상품명 · 브랜드 · 메모 검색");
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(Ui.colorText());
        search.setHintTextColor(Ui.colorSecondary());
        search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        search.setCompoundDrawablePadding(Ui.dp(this, 8));
        search.setBackground(Ui.roundedStroke(Ui.colorSurface(), Ui.colorDivider(), 15, this));
        search.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), 0);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50));
        searchLp.topMargin = Ui.dp(this, 14);
        root.addView(search, searchLp);

        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        filterScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels = {"사용 가능", "7일 이내", "기간 만료", "사용 완료", "전체"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            chips[i] = Ui.text(this, labels[i], 13, Ui.colorSecondary());
            chips[i].setGravity(Gravity.CENTER);
            chips[i].setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chips[i].setPadding(Ui.dp(this, 14), Ui.dp(this, 9), Ui.dp(this, 14), Ui.dp(this, 9));
            chips[i].setOnClickListener(v -> selectFilter(index));
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) chipLp.leftMargin = Ui.dp(this, 8);
            chipRow.addView(chips[i], chipLp);
        }
        filterScroll.addView(chipRow);
        LinearLayout.LayoutParams filterLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        filterLp.topMargin = Ui.dp(this, 12);
        filterLp.bottomMargin = Ui.dp(this, 8);
        root.addView(filterScroll, filterLp);
        updateChipStyles();

        FrameLayout content = new FrameLayout(this);
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(Ui.dp(this, 10));
        list.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 20));
        list.setClipToPadding(false);
        list.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        list.setBackgroundColor(Color.TRANSPARENT);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new GifticonAdapter(this);
        list.setAdapter(adapter);
        content.addView(list, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28), Ui.dp(this, 28));
        Ui.card(empty, this);
        emptyTitle = Ui.text(this, "아직 보관한 기프티콘이 없어요", 18, Ui.colorText());
        emptyTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyBody = Ui.text(this, "위의 ＋ 추가 버튼으로 첫 기프티콘을 등록해보세요.", 13, Ui.colorSecondary());
        emptyBody.setGravity(Gravity.CENTER);
        emptyBody.setPadding(0, Ui.dp(this, 8), 0, 0);
        empty.addView(emptyTitle);
        empty.addView(emptyBody);
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        emptyLp.leftMargin = Ui.dp(this, 4);
        emptyLp.rightMargin = Ui.dp(this, 4);
        content.addView(empty, emptyLp);
        list.setEmptyView(empty);
        root.addView(content, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { reload(); }
            public void afterTextChanged(Editable e) {}
        });
        list.setOnItemClickListener((p, v, pos, id) -> {
            Gifticon g = adapter.getItem(pos);
            startActivity(new Intent(this, GifticonDetailActivity.class).putExtra("id", g.id));
        });
        return root;
    }

    private LinearLayout statCell(String label, TextView value) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView caption = Ui.text(this, label, 12, Ui.colorSecondary());
        caption.setPadding(0, Ui.dp(this, 3), 0, 0);
        cell.addView(value);
        cell.addView(caption);
        return cell;
    }

    private void selectFilter(int index) {
        filterIndex = index;
        updateChipStyles();
        reload();
    }

    private void updateChipStyles() {
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            boolean selected = i == filterIndex;
            chips[i].setTextColor(selected ? Color.WHITE : Ui.colorSecondary());
            chips[i].setBackground(selected
                    ? Ui.rounded(Ui.colorBrand(), 999, this)
                    : Ui.rounded(Ui.colorNeutralSoft(), 999, this));
        }
    }

    private void reload() {
        if (adapter == null) return;
        List<Gifticon> all = GifticonDb.get(this).all();
        String q = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Gifticon> shown = new ArrayList<>();
        int available = 0, soon = 0;
        for (Gifticon g : all) {
            if (!g.isUsed && !g.isExpired()) {
                available++;
                Integer d = g.daysUntilExpiry();
                if (d != null && d >= 0 && d <= 7) soon++;
            }
            if (!q.isEmpty()) {
                String blob = ((g.title == null ? "" : g.title) + " " + (g.brand == null ? "" : g.brand) + " " + (g.memo == null ? "" : g.memo)).toLowerCase(Locale.ROOT);
                if (!blob.contains(q)) continue;
            }
            boolean match;
            switch (filterIndex) {
                case 0: match = !g.isUsed && !g.isExpired(); break;
                case 1:
                    Integer d = g.daysUntilExpiry();
                    match = !g.isUsed && !g.isExpired() && d != null && d >= 0 && d <= 7;
                    break;
                case 2: match = !g.isUsed && g.isExpired(); break;
                case 3: match = g.isUsed; break;
                default: match = true;
            }
            if (match) shown.add(g);
        }
        adapter.setItems(shown);
        statAvailable.setText(String.valueOf(available));
        statSoon.setText(String.valueOf(soon));
        statTotal.setText(String.valueOf(all.size()));
        updateEmptyMessage(q);
    }

    private void updateEmptyMessage(String q) {
        if (emptyTitle == null) return;
        if (!q.isEmpty()) {
            emptyTitle.setText("검색 결과가 없어요");
            emptyBody.setText("다른 상품명이나 브랜드로 검색해보세요.");
            return;
        }
        switch (filterIndex) {
            case 0:
                emptyTitle.setText("사용 가능한 기프티콘이 없어요");
                emptyBody.setText("＋ 추가 버튼으로 기프티콘을 등록해보세요.");
                break;
            case 1:
                emptyTitle.setText("7일 안에 만료되는 기프티콘이 없어요");
                emptyBody.setText("지금은 급하게 써야 할 기프티콘이 없네요.");
                break;
            case 2:
                emptyTitle.setText("만료된 기프티콘이 없어요");
                emptyBody.setText("잘 관리하고 있어요.");
                break;
            case 3:
                emptyTitle.setText("사용 완료한 기프티콘이 없어요");
                emptyBody.setText("사용한 기프티콘은 상세 화면에서 완료 처리할 수 있어요.");
                break;
            default:
                emptyTitle.setText("아직 보관한 기프티콘이 없어요");
                emptyBody.setText("＋ 추가 버튼으로 첫 기프티콘을 등록해보세요.");
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7001);
        }
    }
}
