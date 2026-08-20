package kr.co.conwallet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView actionButton(Context c, String label, boolean primary) {
        TextView t = text(c, label, 14, primary ? Color.WHITE : colorText());
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 44));
        t.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        t.setBackground(primary
                ? rounded(colorBrand(), 14, c)
                : roundedStroke(colorSurface(), colorDivider(), 14, c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static void stylePrimaryButton(Button b, Context c) {
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(c, 48));
        b.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 10));
        b.setBackground(rounded(colorBrand(), 14, c));
    }

    public static void styleSecondaryButton(Button b, Context c) {
        b.setAllCaps(false);
        b.setTextColor(colorText());
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(c, 46));
        b.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        b.setBackground(roundedStroke(colorSurface(), colorDivider(), 14, c));
    }

    public static void styleDangerButton(Button b, Context c) {
        b.setAllCaps(false);
        b.setTextColor(colorDanger());
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(c, 46));
        b.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        b.setBackground(rounded(colorDangerSoft(), 14, c));
    }

    public static void card(View v, Context c) {
        v.setBackground(rounded(colorSurface(), 20, c));
        v.setElevation(dp(c, 1.5f));
    }

    public static void prepareWindow(Activity a) {
        a.getWindow().setStatusBarColor(colorBg());
        a.getWindow().setNavigationBarColor(colorSurface());
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    public static void margins(View view, int l, int t, int r, int b) {
        ViewGroup.LayoutParams raw = view.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) raw;
            p.setMargins(l, t, r, b);
            view.setLayoutParams(p);
        }
    }

    public static int colorBrand() { return Color.rgb(202, 20, 31); }
    public static int colorBrandDark() { return Color.rgb(154, 15, 24); }
    public static int colorBrandSoft() { return Color.rgb(255, 239, 241); }
    public static int colorBg() { return Color.rgb(247, 247, 249); }
    public static int colorSurface() { return Color.WHITE; }
    public static int colorText() { return Color.rgb(24, 24, 27); }
    public static int colorSecondary() { return Color.rgb(113, 113, 122); }
    public static int colorDivider() { return Color.rgb(228, 228, 231); }
    public static int colorNeutralSoft() { return Color.rgb(241, 241, 243); }
    public static int colorSuccess() { return Color.rgb(25, 128, 78); }
    public static int colorSuccessSoft() { return Color.rgb(234, 247, 240); }
    public static int colorWarning() { return Color.rgb(174, 99, 0); }
    public static int colorWarningSoft() { return Color.rgb(255, 244, 226); }
    public static int colorDanger() { return Color.rgb(190, 36, 44); }
    public static int colorDangerSoft() { return Color.rgb(255, 238, 239); }
}
