package kr.co.conwallet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREF_UI = "main_ui_prefs";
    private static final String KEY_SORT = "sort_index";
    private static final String KEY_MANUAL_ORDER = "manual_order_ids";

    private GifticonAdapter adapter;
    private ListView listView;
    private FrameLayout contentLayer;
    private EditText search;
    private TextView statAvailable, statSoon, statTotal, emptyTitle, emptyBody, sortButton;
    private final TextView[] chips = new TextView[5];
    private final List<Gifticon> displayedItems = new ArrayList<>();

    private int filterIndex = 0;
    private int sortIndex = 0;

    private boolean dragging = false;
    private int dragPosition = -1;
    private String draggingId;
    private ImageView dragGhost;
    private float dragFingerOffsetY;
    private float lastTouchX;
    private float lastTouchY;

    private int touchSlop;
    private boolean swiping = false;
    private float swipeDownX;
    private float swipeDownY;
    private float swipeStartTranslation;
    private int swipePosition = -1;
    private View swipeRow;
    private View swipeForeground;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.prepareWindow(this);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        sortIndex = getSharedPreferences(PREF_UI, MODE_PRIVATE).getInt(KEY_SORT, 0);
        if (sortIndex < 0 || sortIndex > 2) sortIndex = 0;
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

        sortButton = Ui.actionButton(this, sortLabel(sortIndex), false);
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

        contentLayer = new FrameLayout(this);
        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(Ui.dp(this, 10));
        listView.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 20));
        listView.setClipToPadding(false);
        listView.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        listView.setBackgroundColor(Color.TRANSPARENT);
        listView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        adapter = new GifticonAdapter(this);
        adapter.setDeleteListener(this::moveGifticonToTrash);
        listView.setAdapter(adapter);
        contentLayer.addView(listView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

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
        contentLayer.addView(emptyWrap, emptyLp);
        listView.setEmptyView(emptyWrap);
        root.addView(contentLayer, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { reload(); }
            public void afterTextChanged(Editable e) {}
        });

        listView.setOnItemClickListener((p, v, pos, id) -> {
            if (adapter.getOpenSwipeId() != null) {
                adapter.closeOpenSwipe();
                return;
            }
            Gifticon g = adapter.getItem(pos);
            startActivity(new Intent(this, GifticonDetailActivity.class).putExtra("id", g.id));
        });

        listView.setOnItemLongClickListener((p, v, pos, id) -> {
            if (sortIndex != 2 || pos < 0 || pos >= displayedItems.size()) return false;
            View foreground = adapter.getForeground(v);
            if (foreground != null) foreground.setTranslationX(0f);
            adapter.rememberOpenSwipeId(null);
            swiping = false;
            dragging = true;
            dragPosition = pos;
            draggingId = displayedItems.get(pos).id;
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            startDragGhost(v);
            if (listView.getParent() != null) listView.getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        });

        listView.setOnTouchListener((v, event) -> handleListTouch(event));
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

    private String sortLabel(int index) {
        if (index == 1) return "등록순";
        if (index == 2) return "자유";
        return "임박순";
    }

    private void showSortDialog() {
        String[] choices = {"기한 임박순", "등록순", "자유"};
        new AlertDialog.Builder(this)
                .setTitle("정렬 기준")
                .setSingleChoiceItems(choices, sortIndex, (d, which) -> {
                    if (which == 2 && sortIndex != 2) seedManualOrderFromCurrent();
                    sortIndex = which;
                    getSharedPreferences(PREF_UI, MODE_PRIVATE).edit().putInt(KEY_SORT, sortIndex).apply();
                    sortButton.setText(sortLabel(sortIndex));
                    d.dismiss();
                    reload();
                    if (sortIndex == 2) {
                        Toast.makeText(this, "항목을 길게 누른 채 위아래로 움직여 순서를 바꿔보세요.", Toast.LENGTH_SHORT).show();
                    }
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
        adapter.rememberOpenSwipeId(null);
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
        sortItems(shown, all);
        displayedItems.clear();
        displayedItems.addAll(shown);
        adapter.setItems(displayedItems);
        statAvailable.setText(String.valueOf(available));
        statSoon.setText(String.valueOf(soon));
        statTotal.setText(String.valueOf(all.size()));
        updateEmptyMessage(q);
    }

    private void sortItems(List<Gifticon> items, List<Gifticon> all) {
        if (sortIndex == 1) {
            Collections.sort(items, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        } else if (sortIndex == 2) {
            List<String> order = ensureManualOrder(all);
            Map<String, Integer> rank = new HashMap<>();
            for (int i = 0; i < order.size(); i++) rank.put(order.get(i), i);
            Collections.sort(items, (a, b) -> Integer.compare(
                    rank.containsKey(a.id) ? rank.get(a.id) : Integer.MAX_VALUE,
                    rank.containsKey(b.id) ? rank.get(b.id) : Integer.MAX_VALUE
            ));
        } else {
            sortByExpiry(items);
        }
    }

    private void sortByExpiry(List<Gifticon> items) {
        Collections.sort(items, (a, b) -> {
            if (a.expiryDate == null && b.expiryDate == null) return Long.compare(b.createdAt, a.createdAt);
            if (a.expiryDate == null) return 1;
            if (b.expiryDate == null) return -1;
            int c = Long.compare(a.expiryDate, b.expiryDate);
            return c != 0 ? c : Long.compare(b.createdAt, a.createdAt);
        });
    }

    private void seedManualOrderFromCurrent() {
        SharedPreferences p = getSharedPreferences(PREF_UI, MODE_PRIVATE);
        String saved = p.getString(KEY_MANUAL_ORDER, "");
        if (saved != null && !saved.trim().isEmpty()) {
            ensureManualOrder(GifticonDb.get(this).all());
            return;
        }

        List<Gifticon> all = GifticonDb.get(this).all();
        List<String> ids = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Gifticon g : displayedItems) {
            if (g != null && g.id != null && seen.add(g.id)) ids.add(g.id);
        }

        List<Gifticon> remaining = new ArrayList<>(all);
        if (sortIndex == 1) {
            Collections.sort(remaining, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        } else {
            sortByExpiry(remaining);
        }
        for (Gifticon g : remaining) {
            if (g != null && g.id != null && seen.add(g.id)) ids.add(g.id);
        }
        writeManualOrder(ids);
    }

    private List<String> ensureManualOrder(List<Gifticon> all) {
        List<String> saved = readManualOrder();
        Set<String> valid = new HashSet<>();
        for (Gifticon g : all) if (g != null && g.id != null) valid.add(g.id);

        List<String> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : saved) {
            if (valid.contains(id) && seen.add(id)) normalized.add(id);
        }

        if (normalized.isEmpty() && !all.isEmpty()) {
            List<Gifticon> seed = new ArrayList<>(all);
            sortByExpiry(seed);
            for (Gifticon g : seed) {
                if (g.id != null && seen.add(g.id)) normalized.add(g.id);
            }
        } else {
            for (Gifticon g : all) {
                if (g.id != null && seen.add(g.id)) normalized.add(g.id);
            }
        }

        if (!normalized.equals(saved)) writeManualOrder(normalized);
        return normalized;
    }

    private List<String> readManualOrder() {
        String raw = getSharedPreferences(PREF_UI, MODE_PRIVATE).getString(KEY_MANUAL_ORDER, "");
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;
        Set<String> seen = new HashSet<>();
        String[] parts = raw.split("\\n");
        for (String part : parts) {
            String id = part == null ? "" : part.trim();
            if (!id.isEmpty() && seen.add(id)) out.add(id);
        }
        return out;
    }

    private void writeManualOrder(List<String> ids) {
        StringBuilder b = new StringBuilder();
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isEmpty() || !seen.add(id)) continue;
            if (b.length() > 0) b.append('\n');
            b.append(id);
        }
        getSharedPreferences(PREF_UI, MODE_PRIVATE).edit().putString(KEY_MANUAL_ORDER, b.toString()).apply();
    }

    private boolean handleListTouch(MotionEvent event) {
        lastTouchX = event.getX();
        lastTouchY = event.getY();

        if (dragging) return handleDragTouch(event);

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            swiping = false;
            swipeDownX = event.getX();
            swipeDownY = event.getY();
            swipePosition = listView.pointToPosition((int) swipeDownX, (int) swipeDownY);
            swipeRow = null;
            swipeForeground = null;
            swipeStartTranslation = 0f;
            if (swipePosition >= 0 && swipePosition < adapter.getCount()) {
                int childIndex = swipePosition - listView.getFirstVisiblePosition();
                if (childIndex >= 0 && childIndex < listView.getChildCount()) {
                    swipeRow = listView.getChildAt(childIndex);
                    swipeForeground = adapter.getForeground(swipeRow);
                    if (swipeForeground != null) swipeStartTranslation = swipeForeground.getTranslationX();
                }
            }
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE && swipeForeground != null) {
            float dx = event.getX() - swipeDownX;
            float dy = event.getY() - swipeDownY;
            if (!swiping && Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.15f) {
                if (dx < 0 || swipeStartTranslation < 0f) {
                    swiping = true;
                    closeOtherVisibleSwipes(swipeRow);
                    if (listView.getParent() != null) listView.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            if (swiping) {
                float width = adapter.getDeleteWidthPx();
                float tx = swipeStartTranslation + dx;
                tx = Math.max(-width, Math.min(0f, tx));
                swipeForeground.setTranslationX(tx);
                return true;
            }
        }

        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) && swiping && swipeForeground != null) {
            float width = adapter.getDeleteWidthPx();
            boolean open = action == MotionEvent.ACTION_UP && swipeForeground.getTranslationX() <= -width * 0.42f;
            float target = open ? -width : 0f;
            String id = swipePosition >= 0 && swipePosition < adapter.getCount() ? adapter.getItem(swipePosition).id : null;
            swipeForeground.animate()
                    .translationX(target)
                    .setDuration(150)
                    .withEndAction(() -> adapter.rememberOpenSwipeId(open ? id : null))
                    .start();
            swiping = false;
            if (listView.getParent() != null) listView.getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }

        if (action == MotionEvent.ACTION_UP && adapter.getOpenSwipeId() != null && swipePosition >= 0 && swipePosition < adapter.getCount()) {
            Gifticon touched = adapter.getItem(swipePosition);
            if (touched == null || touched.id == null || !touched.id.equals(adapter.getOpenSwipeId())) {
                adapter.closeOpenSwipe();
                return true;
            }
        }
        return false;
    }

    private void closeOtherVisibleSwipes(View exceptRow) {
        if (listView == null || adapter == null) return;
        adapter.rememberOpenSwipeId(null);
        for (int i = 0; i < listView.getChildCount(); i++) {
            View row = listView.getChildAt(i);
            if (row == exceptRow) continue;
            View foreground = adapter.getForeground(row);
            if (foreground != null && foreground.getTranslationX() != 0f) {
                foreground.animate().translationX(0f).setDuration(120).start();
            }
        }
    }

    private void startDragGhost(View row) {
        if (row == null || row.getWidth() <= 0 || row.getHeight() <= 0 || contentLayer == null) return;
        try {
            Bitmap bitmap = Bitmap.createBitmap(row.getWidth(), row.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            row.draw(canvas);

            dragGhost = new ImageView(this);
            dragGhost.setImageBitmap(bitmap);
            dragGhost.setScaleType(ImageView.ScaleType.FIT_XY);
            dragGhost.setAlpha(0.96f);
            dragGhost.setElevation(Ui.dp(this, 16));
            dragGhost.setScaleX(1.015f);
            dragGhost.setScaleY(1.015f);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(row.getWidth(), row.getHeight());
            contentLayer.addView(dragGhost, lp);
            dragGhost.setX(listView.getLeft() + row.getLeft());
            dragGhost.setY(listView.getTop() + row.getTop());
            dragFingerOffsetY = lastTouchY - row.getTop();
            adapter.setDraggedId(draggingId);
        } catch (Exception ignored) {
            dragGhost = null;
        }
    }

    private boolean handleDragTouch(MotionEvent event) {
        if (!dragging || sortIndex != 2 || listView == null) return false;

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) {
            if (dragGhost != null) {
                dragGhost.setY(listView.getTop() + event.getY() - dragFingerOffsetY);
            }

            int edge = Ui.dp(this, 56);
            int step = Ui.dp(this, 40);
            if (event.getY() < edge) {
                listView.smoothScrollBy(-step, 60);
            } else if (event.getY() > listView.getHeight() - edge) {
                listView.smoothScrollBy(step, 60);
            }

            int target = listView.pointToPosition((int) event.getX(), (int) event.getY());
            if (target >= 0 && target < displayedItems.size() && target != dragPosition) {
                int first = listView.getFirstVisiblePosition();
                View firstView = listView.getChildAt(0);
                int top = firstView == null ? 0 : firstView.getTop();

                Gifticon moving = displayedItems.remove(dragPosition);
                displayedItems.add(target, moving);
                dragPosition = target;
                adapter.setItems(displayedItems);
                listView.setSelectionFromTop(first, top);
                listView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                persistManualOrderFromDisplayed();
            }
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            persistManualOrderFromDisplayed();
            finishDragGhost();
            dragging = false;
            dragPosition = -1;
            draggingId = null;
            if (listView.getParent() != null) listView.getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return true;
    }

    private void finishDragGhost() {
        if (dragGhost != null && contentLayer != null) {
            contentLayer.removeView(dragGhost);
            dragGhost.setImageDrawable(null);
            dragGhost = null;
        }
        if (adapter != null) adapter.setDraggedId(null);
    }

    private void persistManualOrderFromDisplayed() {
        List<Gifticon> all = GifticonDb.get(this).all();
        List<String> base = ensureManualOrder(all);
        List<String> visibleOrder = new ArrayList<>();
        Set<String> visibleSet = new HashSet<>();
        for (Gifticon g : displayedItems) {
            if (g != null && g.id != null && visibleSet.add(g.id)) visibleOrder.add(g.id);
        }
        if (visibleOrder.isEmpty()) return;

        List<String> result = new ArrayList<>();
        int visibleIndex = 0;
        for (String id : base) {
            if (visibleSet.contains(id)) {
                if (visibleIndex < visibleOrder.size()) result.add(visibleOrder.get(visibleIndex++));
            } else {
                result.add(id);
            }
        }
        while (visibleIndex < visibleOrder.size()) result.add(visibleOrder.get(visibleIndex++));
        writeManualOrder(result);
    }

    private void moveGifticonToTrash(Gifticon g) {
        if (g == null || g.id == null) return;
        if (dragging) return;
        adapter.rememberOpenSwipeId(null);
        NotificationHelper.cancel(this, g.id);
        GifticonDb.get(this).moveToTrash(g.id);
        Toast.makeText(this, "휴지통으로 이동했어요.", Toast.LENGTH_SHORT).show();
        reload();
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
