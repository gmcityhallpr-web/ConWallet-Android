package kr.co.conwallet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private GifticonAdapter adapter;
    private EditText search;
    private TextView statAvailable, statSoon, statTotal, emptyTitle, emptyBody, sortButton;
    private final TextView[] chips = new TextView[5];
    private int filterIndex = 0;
    private int sortIndex = 0;

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
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 24), Ui.dp(this, 18), Ui.dp(this, 8));
        root.setBackgroundColor(Ui.colorBg());

        // Apple의 inset grouped UI처럼 상단을 하나의 부드러운 카드로 묶고,
        // 텍스트 버튼 대신 아이콘 버튼을 사용해 시각적인 밀도를 낮춥니다.
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(Ui.dp(this, 13), Ui.dp(this, 12), Ui.dp(this, 11), Ui.dp(this, 12));
        header.setBackground(Ui.rounded(Color.WHITE, 24, this));
        header.setElevation(Ui.dp(this, 2));

        FrameLayout brandMark = new FrameLayout(this);
        brandMark.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 15, this));
        brandMark.setClipToOutline(true);
        ImageView brandImage = new ImageView(this);
        brandImage.setImageResource(R.drawable.wook_launcher);
        brandImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        brandMark.addView(brandImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
        ));
        header.addView(brandMark, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));

        TextView title = Ui.text(this, "디지털폐지수집", 23, Ui.colorText());
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleLp.leftMargin = Ui.dp(this, 11);
        titleLp.rightMargin = Ui.dp(this, 8);
        header.addView(title, titleLp);

        FrameLayout settings = topIconButton(R.drawable.ic_settings_sleek, false, "설정");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams settingsLp = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42));
        settingsLp.rightMargin = Ui.dp(this, 7);
        header.addView(settings, settingsLp);

        FrameLayout add = topIconButton(R.drawable.ic_add_sleek, true, "기프티콘 추가");
        add.setOnClickListener(v -> startActivity(new Intent(this, AddEditGifticonActivity.class)));
        header.addView(add, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));

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
        statsLp.topMargin = Ui.dp(this, 14);
        root.addView(stats, statsLp);

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
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
        searchRow.addView(search, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));

        sortButton = Ui.actionButton(this, "정렬", false);
        sortButton.setOnClickListener(v -> showSortDialog());
        LinearLayout.LayoutParams sortLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(this, 50));
        sortLp.leftMargin = Ui.dp(this, 8);
        searchRow.addView(sortButton, sortLp);
        LinearLayout.LayoutParams searchRowLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchRowLp.topMargin = Ui.dp(this, 14);
        root.addView(searchRow, searchRowLp);

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

        LinearLayout emptyWrap = new LinearLayout(this);
        emptyWrap.setOrientation(LinearLayout.VERTICAL);
        emptyWrap.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyWrap.setPadding(Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16));
        FrameLayout avatarBubble = new FrameLayout(this);
        avatarBubble.setBackground(Ui.rounded(Ui.colorBrandSoft(), 999, this));
        avatarBubble.setPadding(Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5), Ui.dp(this, 5));
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.wook_launcher);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarBubble.addView(avatar, new FrameLayout.LayoutParams(Ui.dp(this, 148), Ui.dp(this, 148), Gravity.CENTER));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(Ui.dp(this, 158), Ui.dp(this, 158));
        avatarLp.bottomMargin = Ui.dp(this, 8);
        emptyWrap.addView(avatarBubble, avatarLp);

        FrameLayout speech = new FrameLayout(this);
        View tail = new View(this);
        tail.setBackground(Ui.roundedStroke(Color.WHITE, Ui.colorDivider(), 5, this));
        tail.setRotation(45f);
        FrameLayout.LayoutParams tailLp = new FrameLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        tailLp.topMargin = Ui.dp(this, 2);
        speech.addView(tail, tailLp);
        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setGravity(Gravity.CENTER);
        bubble.setPadding(Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 22));
        bubble.setBackground(Ui.roundedStroke(Color.WHITE, Ui.colorDivider(), 22, this));
        emptyTitle = Ui.text(this, "기프티콘 쓸게 없는데 뭘 먹겠단건데?", 19, Ui.colorText());
        emptyTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyBody = Ui.text(this, "오른쪽 상단에 추가 버튼 눌러서 기프티콘 등록해", 13, Ui.colorSecondary());
        emptyBody.setGravity(Gravity.CENTER);
        emptyBody.setPadding(0, Ui.dp(this, 8), 0, 0);
        bubble.addView(emptyTitle);
        bubble.addView(emptyBody);
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        bubbleLp.topMargin = Ui.dp(this, 11);
        speech.addView(bubble, bubbleLp);
        emptyWrap.addView(speech, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams emptyLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        emptyLp.leftMargin = Ui.dp(this, 4);
        emptyLp.rightMargin = Ui.dp(this, 4);
        content.addView(emptyWrap, emptyLp);
        list.setEmptyView(emptyWrap);
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

    private FrameLayout topIconButton(int iconRes, boolean primary, String description) {
        FrameLayout button = new FrameLayout(this);
        button.setBackground(Ui.rounded(primary ? Ui.colorBrand() : Ui.colorNeutralSoft(), 999, this));
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription(description);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int size = Ui.dp(this, primary ? 20 : 19);
        button.addView(icon, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        return button;
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

    private void showSortDialog() {
        String[] choices = {"만료 임박순", "최근 등록순", "브랜드순"};
        new AlertDialog.Builder(this)
                .setTitle("정렬")
                .setSingleChoiceItems(choices, sortIndex, (d, which) -> {
                    sortIndex = which;
                    sortButton.setText(which == 0 ? "임박순" : which == 1 ? "최근순" : "브랜드순");
                    d.dismiss();
                    reload();
                })
                .show();
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
            chips[i].setBackground(selected ? Ui.rounded(Ui.colorBrand(), 999, this) : Ui.rounded(Ui.colorNeutralSoft(), 999, this));
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
        sortItems(shown);
        adapter.setItems(shown);
        statAvailable.setText(String.valueOf(available));
        statSoon.setText(String.valueOf(soon));
        statTotal.setText(String.valueOf(all.size()));
        updateEmptyMessage(q);
    }

    private void sortItems(List<Gifticon> items) {
        if (sortIndex == 1) {
            Collections.sort(items, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        } else if (sortIndex == 2) {
            Collections.sort(items, Comparator.comparing(g -> (g.brand == null ? "" : g.brand).toLowerCase(Locale.ROOT)));
        } else {
            Collections.sort(items, (a, b) -> {
                if (a.expiryDate == null && b.expiryDate == null) return Long.compare(b.createdAt, a.createdAt);
                if (a.expiryDate == null) return 1;
                if (b.expiryDate == null) return -1;
                int c = Long.compare(a.expiryDate, b.expiryDate);
                return c != 0 ? c : Long.compare(b.createdAt, a.createdAt);
            });
        }
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
                emptyTitle.setText("기프티콘 쓸게 없는데 뭘 먹겠단건데?");
                emptyBody.setText("오른쪽 상단에 추가 버튼 눌러서 기프티콘 등록해");
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
