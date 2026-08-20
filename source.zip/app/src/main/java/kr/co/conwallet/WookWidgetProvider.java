package kr.co.conwallet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class WookWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        refreshAll(context);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, WookWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(component);
        if (ids == null || ids.length == 0) return;
        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(Context context, AppWidgetManager manager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wookwallet);

        PendingIntent openApp = PendingIntent.getActivity(
                context,
                appWidgetId,
                new Intent(context, MainActivity.class)
                        .setAction("kr.co.conwallet.WIDGET_OPEN_APP_" + appWidgetId),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, openApp);
        views.setOnClickPendingIntent(R.id.widget_header, openApp);

        List<Gifticon> items = widgetItems(context);
        views.setTextViewText(
                R.id.widget_subtitle,
                items.isEmpty()
                        ? "사용 가능한 기프티콘이 없어요"
                        : "사용 가능 " + items.size() + "개 · 만료 임박 순"
        );

        int[] rowIds = {R.id.widget_row_1, R.id.widget_row_2, R.id.widget_row_3};
        int[] titleIds = {R.id.widget_item_title_1, R.id.widget_item_title_2, R.id.widget_item_title_3};
        int[] dayIds = {R.id.widget_item_day_1, R.id.widget_item_day_2, R.id.widget_item_day_3};

        if (items.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE);
            for (int rowId : rowIds) views.setViewVisibility(rowId, View.GONE);
            views.setOnClickPendingIntent(R.id.widget_empty, openApp);
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE);
            for (int i = 0; i < rowIds.length; i++) {
                if (i < items.size()) {
                    Gifticon g = items.get(i);
                    views.setViewVisibility(rowIds[i], View.VISIBLE);
                    views.setTextViewText(titleIds[i], displayName(g));
                    views.setTextViewText(dayIds[i], deadlineText(g));

                    Intent detail = new Intent(context, GifticonDetailActivity.class)
                            .putExtra("id", g.id)
                            .setAction("kr.co.conwallet.WIDGET_OPEN_" + g.id + "_" + i);
                    PendingIntent openDetail = PendingIntent.getActivity(
                            context,
                            (g.id + ":widget:" + i).hashCode() & 0x7fffffff,
                            detail,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    views.setOnClickPendingIntent(rowIds[i], openDetail);
                } else {
                    views.setViewVisibility(rowIds[i], View.GONE);
                }
            }
        }

        manager.updateAppWidget(appWidgetId, views);
    }

    private static List<Gifticon> widgetItems(Context context) {
        List<Gifticon> out = new ArrayList<>();
        for (Gifticon g : GifticonDb.get(context).all()) {
            if (!g.isUsed && !g.isExpired()) out.add(g);
        }

        Collections.sort(out, new Comparator<Gifticon>() {
            @Override
            public int compare(Gifticon a, Gifticon b) {
                if (a.expiryDate == null && b.expiryDate == null) {
                    return Long.compare(b.createdAt, a.createdAt);
                }
                if (a.expiryDate == null) return 1;
                if (b.expiryDate == null) return -1;
                int expiryCompare = Long.compare(a.expiryDate, b.expiryDate);
                if (expiryCompare != 0) return expiryCompare;
                return Long.compare(b.createdAt, a.createdAt);
            }
        });

        if (out.size() > 3) return new ArrayList<>(out.subList(0, 3));
        return out;
    }

    private static String displayName(Gifticon g) {
        String title = g.title == null || g.title.trim().isEmpty() ? "기프티콘" : g.title.trim();
        String brand = g.brand == null ? "" : g.brand.trim();
        return brand.isEmpty() ? title : brand + " · " + title;
    }

    private static String deadlineText(Gifticon g) {
        Integer days = g.daysUntilExpiry();
        if (days == null) return "기한 없음";
        if (days == 0) return "오늘";
        return "D-" + days;
    }
}
