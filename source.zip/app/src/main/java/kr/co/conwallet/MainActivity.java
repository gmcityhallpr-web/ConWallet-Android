package kr.co.conwallet;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private GifticonAdapter adapter;
    private EditText search;
    private Spinner filter;
    private TextView summary;
    private ListView list;
    private int filterIndex = 0;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 8));
        root.setBackgroundColor(Ui.colorBg());

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "콘지갑", 28, Ui.colorText());
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button settings = new Button(this); settings.setText("설정");
        Button add = new Button(this); add.setText("＋ 추가");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        add.setOnClickListener(v -> startActivity(new Intent(this, AddEditGifticonActivity.class)));
        top.addView(settings); top.addView(add);
        root.addView(top);

        search = new EditText(this);
        search.setHint("상품명, 브랜드, 메모 검색");
        search.setSingleLine(true);
        search.setBackground(Ui.rounded(Color.WHITE, 12, this));
        search.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        searchLp.topMargin = Ui.dp(this, 10);
        root.addView(search, searchLp);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        filter = new Spinner(this);
        String[] filters = {"사용 가능", "7일 이내", "기간 만료", "사용 완료", "전체"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filter.setAdapter(spinnerAdapter);
        summary = Ui.text(this, "", 12, Ui.colorSecondary());
        filterRow.addView(filter, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        filterRow.addView(summary);
        root.addView(filterRow);

        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(Ui.dp(this, 8));
        list.setPadding(0, Ui.dp(this, 4), 0, 0);
        list.setClipToPadding(false);
        list.setBackgroundColor(Color.TRANSPARENT);
        adapter = new GifticonAdapter(this);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { reload(); }
            public void afterTextChanged(Editable e) {}
        });
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { filterIndex = pos; reload(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        list.setOnItemClickListener((p, v, pos, id) -> {
            Gifticon g = adapter.getItem(pos);
            startActivity(new Intent(this, GifticonDetailActivity.class).putExtra("id", g.id));
        });
        return root;
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
                case 1: Integer d = g.daysUntilExpiry(); match = !g.isUsed && !g.isExpired() && d != null && d >= 0 && d <= 7; break;
                case 2: match = !g.isUsed && g.isExpired(); break;
                case 3: match = g.isUsed; break;
                default: match = true;
            }
            if (match) shown.add(g);
        }
        adapter.setItems(shown);
        summary.setText("사용 가능 " + available + "  ·  7일 이내 " + soon + "  ·  전체 " + all.size());
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7001);
        }
    }
}
