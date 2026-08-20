package kr.co.conwallet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GifticonAdapter extends BaseAdapter {
    private final Context context;
    private final List<Gifticon> items = new ArrayList<>();

    public GifticonAdapter(Context context) { this.context = context; }

    public void setItems(List<Gifticon> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Gifticon getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Row h;
        if (convertView == null) {
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 14), Ui.dp(context, 12));
            Ui.card(root, context);

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(Ui.rounded(Ui.colorNeutralSoft(), 14, context));
            image.setClipToOutline(true);
            root.addView(image, new LinearLayout.LayoutParams(Ui.dp(context, 82), Ui.dp(context, 82)));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(Ui.dp(context, 13), 0, 0, 0);
            root.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

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

            h = new Row(root, image, brand, title, expiry, status);
            root.setTag(h);
            convertView = root;
        } else {
            h = (Row) convertView.getTag();
        }

        Gifticon g = getItem(position);
        h.root.setAlpha(g.isUsed ? 0.62f : 1f);
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

    private void bindStatus(TextView view, String text, int textColor, int fillColor) {
        view.setText(text);
        view.setTextColor(textColor);
        view.setBackground(Ui.rounded(fillColor, 999, context));
    }

    private static class Row {
        final LinearLayout root;
        final ImageView image;
        final TextView brand, title, expiry, status;
        Row(LinearLayout r, ImageView i, TextView b, TextView t, TextView e, TextView s) {
            root = r; image = i; brand = b; title = t; expiry = e; status = s;
        }
    }
}
