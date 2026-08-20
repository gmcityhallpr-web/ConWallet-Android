package kr.co.conwallet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundedStroke(int fill, int stroke, float radiusDp, Context c) {
        GradientDrawable d = rounded(fill, radiusDp, c);
        d.setStroke(dp(c, 1), stroke);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static void margins(View view, int l, int t, int r, int b) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) raw;
            p.setMargins(l, t, r, b);
            view.setLayoutParams(p);
        }
    }

    public static int colorBrand() { return Color.rgb(51, 92, 255); }
    public static int colorBg() { return Color.rgb(246, 247, 251); }
    public static int colorText() { return Color.rgb(23, 24, 28); }
    public static int colorSecondary() { return Color.rgb(102, 106, 115); }
    public static int colorDivider() { return Color.rgb(226, 228, 235); }
}
