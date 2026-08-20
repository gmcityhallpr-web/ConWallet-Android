package kr.co.conwallet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
            root.setPadding(Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12), Ui.dp(context, 12));
            root.setBackground(Ui.rounded(Color.WHITE, 16, context));

            ImageView image = new ImageView(context);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackground(Ui.rounded(Color.rgb(238, 240, 245), 12, context));
            root.addView(image, new LinearLayout.LayoutParams(Ui.dp(context, 76), Ui.dp(context, 76)));

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(Ui.dp(context, 12), 0, 0, 0);
            root.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView brand = Ui.text(context, "", 12, Ui.colorSecondary());
            TextView title = Ui.text(context, "", 16, Ui.colorText());
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            title.setMaxLines(2);
            TextView expiry = Ui.text(context, "", 13, Ui.colorSecondary());
            TextView status = Ui.text(context, "", 12, Ui.colorBrand());
            status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            textCol.addView(brand);
            textCol.addView(title);
            textCol.addView(expiry);
            textCol.addView(status);

            h = new Row(image, brand, title, expiry, status);
            root.setTag(h);
            convertView = root;
        } else {
            h = (Row) convertView.getTag();
        }

        Gifticon g = getItem(position);
        h.brand.setText(g.brand == null || g.brand.isEmpty() ? "기프티콘" : g.brand);
        h.title.setText(g.title);
        h.expiry.setText(g.expiryDate == null ? "유효기간 없음" : "유효기간  " + DateUtil.shortDate(g.expiryDate));
        Integer d = g.daysUntilExpiry();
        if (g.isUsed) h.status.setText("사용 완료");
        else if (g.isExpired()) h.status.setText("기간 만료");
        else if (d != null) h.status.setText(d == 0 ? "오늘 만료" : "D-" + d);
        else h.status.setText("사용 가능");

        h.image.setImageDrawable(null);
        if (g.imagePath != null && new File(g.imagePath).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 4;
            Bitmap b = BitmapFactory.decodeFile(g.imagePath, o);
            h.image.setImageBitmap(b);
        } else {
            h.image.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        return convertView;
    }

    private static class Row {
        final ImageView image; final TextView brand, title, expiry, status;
        Row(ImageView i, TextView b, TextView t, TextView e, TextView s) { image=i; brand=b; title=t; expiry=e; status=s; }
    }
}
