package kr.co.conwallet;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GifticonAdapter extends BaseAdapter {
    public interface DeleteListener {
        void onDelete(Gifticon gifticon);
    }

    private static final String PREF_UI = "main_ui_prefs";
    private static final String KEY_MANUAL_ORDER = "manual_order_ids";

    private final Context context;
    private final List<Gifticon> items = new ArrayList<>();
    private DeleteListener deleteListener;
    private String openSwipeId;
    private String draggedId;
    private boolean reorderMode = false;

    public GifticonAdapter(Context context) { this.context = context; }

    public void setItems(List<Gifticon> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setDeleteListener(DeleteListener listener) {
        deleteListener = listener;
    }

    public void setReorderMode(boolean enabled) {
        if (reorderMode == enabled) return;
        reorderMode = enabled;
        if (!enabled) draggedId = null;
        notifyDataSetChanged();
    }

    public int getDeleteWidthPx() {
        return Ui.dp(context, 84);
    }

    public View getForeground(View row) {
        Object tag = row == null ? null : row.getTag();
        return tag instanceof Row ? ((Row) tag).foreground : row;
    }

    public String getOpenSwipeId() {
        return openSwipeId;
    }

    public void rememberOpenSwipeId(String id) {
        openSwipeId = id;
    }

    public void closeOpenSwipe() {
        if (openSwipeId == null) return;
        openSwipeId = null;
        notifyDataSetChanged();
    }

    public void setDraggedId(String id) {
        draggedId = id;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Gifticon getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Row h;
        if (convertView == null) {
            FrameLayout root = new FrameLayout(context);

            TextView delete = Ui.text(context, "삭제", 14, Color.WHITE);
            delete.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            delete.setGravity(Gravity.CENTER);
            delete.setClickable(true);
            delete.setFocusable(true);
            delete.setBackground(Ui.rounded(Ui.colorDanger(), 18, context));
            FrameLayout.LayoutParams deleteLp = new FrameLayout.LayoutParams(
                    getDeleteWidthPx(),
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.END
            );
            root.addView(delete, deleteLp);

            LinearLayout foreground = new LinearLayout(context);
            foreground.setOrientation(LinearLayout.HORIZONTAL);
            foreground.setGravity(Gravity.CENTER_VERTICAL);
            foreground.setPadding(Ui.dp(context, 9), Ui.dp(context, 12), Ui.dp(context, 14), Ui.dp(context, 12));
            Ui.card(foreground, context);
            root.addView(foreground, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            ));

            LinearLayout dragArea = new LinearLayout(context);
            dragArea.setGravity(Gravity.CENTER);
            dragArea.setBackground(Ui.rounded(0x22D1D1D6, 11, context));
            dragArea.setClickable(true);
            dragArea.setLongClickable(true);
            dragArea.setContentDescription("순서 이동");

            ImageView dragHandle = new ImageView(context);
            dragHandle.setImageResource(R.drawable.ic_reorder_handle);
            dragHandle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            dragHandle.setAlpha(0.72f);
            dragArea.addView(dragHandle, new LinearLayout.LayoutParams(Ui.dp(context, 19), Ui.dp(context, 19)));
            LinearLayout.LayoutParams dragAreaLp = new LinearLayout.LayoutParams(Ui.dp(context, 34), Ui.dp(context, 82));
            dragAreaLp.rightMargin = Ui.dp(context, 9);
            foreground.addView(dragArea, dragAreaLp);

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 14, context));
            image.setClipToOutline(true);
            foreground.addView(image, new LinearLayout.LayoutParams(Ui.dp(context, 82), Ui.dp(context, 82)));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(Ui.dp(context, 13), 0, 0, 0);
            foreground.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            LinearLayout meta = new LinearLayout(context);
            meta.setGravity(Gravity.CENTER_VERTICAL);
            TextView brand = Ui.text(context, "", 12, Ui.colorSecondary());
            brand.setMaxLines(1);
            TextView status = Ui.text(context, "", 11, Ui.colorSuccess());
            status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            status.setGravity(Gravity.CENTER);
            status.setPadding(Ui.dp(context, 9), Ui.dp(context, 4), Ui.dp(context, 9), Ui.dp(context, 4));
            meta.addView(brand, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            meta.addView(status);

            TextView title = Ui.text(context, "", 16, Ui.colorText());
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setMaxLines(2);
            title.setPadding(0, Ui.dp(context, 5), 0, 0);
            TextView expiry = Ui.text(context, "", 13, Ui.colorSecondary());
            expiry.setPadding(0, Ui.dp(context, 7), 0, 0);

            textCol.addView(meta);
            textCol.addView(title);
            textCol.addView(expiry);

            h = new Row(root, foreground, delete, dragArea, dragHandle, image, brand, title, expiry, status);
            root.setTag(h);
            convertView = root;
        } else {
            h = (Row) convertView.getTag();
        }

        Gifticon g = getItem(position);
        final Row row = h;
        final Gifticon boundGifticon = g;

        boolean isDragged = g.id != null && g.id.equals(draggedId);
        h.dragArea.setVisibility(reorderMode ? View.VISIBLE : View.GONE);
        h.foreground.setAlpha(isDragged ? 0.14f : (g.isUsed ? 0.62f : 1f));
        h.delete.setAlpha(isDragged ? 0f : 1f);
        h.dragArea.setAlpha(isDragged ? 0.18f : 1f);
        h.foreground.setTranslationX(g.id != null && g.id.equals(openSwipeId) ? -getDeleteWidthPx() : 0f);

        h.delete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(boundGifticon);
        });

        // 자유 정렬에서는 왼쪽 핸들을 길게 누르면 Android 기본 드래그 섀도가
        // 손가락을 그대로 따라갑니다. 압력 감지/3D Touch와 무관하게 동작합니다.
        h.dragArea.setOnLongClickListener(v -> {
            if (!reorderMode || boundGifticon.id == null) return false;
            openSwipeId = null;
            draggedId = boundGifticon.id;
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            notifyDataSetChanged();

            ClipData clip = ClipData.newPlainText("gifticon_id", boundGifticon.id);
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(row.foreground);
            boolean started;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                started = row.root.startDragAndDrop(clip, shadow, boundGifticon.id, 0);
            } else {
                started = row.root.startDrag(clip, shadow, boundGifticon.id, 0);
            }
            if (!started) {
                draggedId = null;
                notifyDataSetChanged();
            }
            return started;
        });

        h.root.setOnDragListener((v, event) -> {
            if (!reorderMode) return false;
            Object local = event.getLocalState();
            if (!(local instanceof String)) return false;
            String movingId = (String) local;

            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    if (boundGifticon.id != null && !boundGifticon.id.equals(movingId)) {
                        if (moveItemBefore(movingId, boundGifticon.id)) {
                            row.root.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                            persistManualOrder();
                        }
                    }
                    return true;
                case DragEvent.ACTION_DROP:
                    persistManualOrder();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    draggedId = null;
                    persistManualOrder();
                    notifyDataSetChanged();
                    return true;
                default:
                    return true;
            }
        });

        h.brand.setText(g.brand == null || g.brand.isEmpty() ? "기프티콘" : g.brand);
        h.title.setText(g.title == null || g.title.trim().isEmpty() ? "이름 없는 기프티콘" : g.title);
        h.expiry.setText(g.expiryDate == null ? "유효기간 없음" : "유효기간 · " + DateUtil.shortDate(g.expiryDate));

        Integer d = g.daysUntilExpiry();
        if (g.isUsed) {
            bindStatus(h.status, "사용 완료", Ui.colorSecondary(), Ui.colorNeutralSoft());
        } else if (g.isExpired()) {
            bindStatus(h.status, "기간 만료", Ui.colorDanger(), Ui.colorDangerSoft());
        } else if (d != null && d <= 7) {
            bindStatus(h.status, d == 0 ? "오늘 만료" : "D-" + d, Ui.colorWarning(), Ui.colorWarningSoft());
        } else if (d != null) {
            bindStatus(h.status, "D-" + d, Ui.colorSuccess(), Ui.colorSuccessSoft());
        } else {
            bindStatus(h.status, "사용 가능", Ui.colorSuccess(), Ui.colorSuccessSoft());
        }

        h.image.setImageDrawable(null);
        if (g.imagePath != null && new File(g.imagePath).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            Bitmap b = BitmapFactory.decodeFile(g.imagePath, o);
            if (b != null) h.image.setImageBitmap(b);
            else h.image.setImageResource(android.R.drawable.ic_menu_gallery);
        } else {
            h.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        return convertView;
    }

    private boolean moveItemBefore(String movingId, String targetId) {
        int from = indexOfId(movingId);
        int to = indexOfId(targetId);
        if (from < 0 || to < 0 || from == to) return false;

        Gifticon moving = items.remove(from);
        if (from < to) to--;
        items.add(Math.max(0, Math.min(to, items.size())), moving);
        notifyDataSetChanged();
        return true;
    }

    private int indexOfId(String id) {
        if (id == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            Gifticon g = items.get(i);
            if (g != null && id.equals(g.id)) return i;
        }
        return -1;
    }

    private void persistManualOrder() {
        if (!reorderMode) return;
        StringBuilder b = new StringBuilder();
        Set<String> seen = new HashSet<>();
        for (Gifticon g : items) {
            if (g == null || g.id == null || !seen.add(g.id)) continue;
            if (b.length() > 0) b.append('\n');
            b.append(g.id);
        }
        SharedPreferences p = context.getSharedPreferences(PREF_UI, Context.MODE_PRIVATE);
        p.edit().putString(KEY_MANUAL_ORDER, b.toString()).apply();
    }

    private void bindStatus(TextView view, String text, int textColor, int fillColor) {
        view.setText(text);
        view.setTextColor(textColor);
        view.setBackground(Ui.rounded(fillColor, 999, context));
    }

    private static class Row {
        final FrameLayout root;
        final LinearLayout foreground;
        final TextView delete;
        final LinearLayout dragArea;
        final ImageView dragHandle;
        final ImageView image;
        final TextView brand, title, expiry, status;

        Row(FrameLayout r, LinearLayout f, TextView d, LinearLayout da, ImageView dh,
            ImageView i, TextView b, TextView t, TextView e, TextView s) {
            root = r;
            foreground = f;
            delete = d;
            dragArea = da;
            dragHandle = dh;
            image = i;
            brand = b;
            title = t;
            expiry = e;
            status = s;
        }
    }
}
