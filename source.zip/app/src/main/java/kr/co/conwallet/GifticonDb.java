package kr.co.conwallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class GifticonDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "conwallet.db";
    private static final int DB_VERSION = 1;
    private static GifticonDb instance;

    public static synchronized GifticonDb get(Context c) {
        if (instance == null) instance = new GifticonDb(c.getApplicationContext());
        return instance;
    }

    private GifticonDb(Context c) { super(c, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE gifticons (" +
                "id TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "brand TEXT NOT NULL DEFAULT ''," +
                "memo TEXT NOT NULL DEFAULT ''," +
                "expiry INTEGER," +
                "created_at INTEGER NOT NULL," +
                "updated_at INTEGER NOT NULL," +
                "is_used INTEGER NOT NULL DEFAULT 0," +
                "used_at INTEGER," +
                "notifications_enabled INTEGER NOT NULL DEFAULT 1," +
                "barcode_payload TEXT," +
                "barcode_symbology TEXT," +
                "image_path TEXT" +
                ")");
        db.execSQL("CREATE INDEX idx_gifticons_created ON gifticons(created_at DESC)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public void save(Gifticon g) {
        ContentValues v = values(g);
        getWritableDatabase().insertWithOnConflict("gifticons", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Gifticon getById(String id) {
        Cursor c = getReadableDatabase().query("gifticons", null, "id=?", new String[]{id}, null, null, null);
        try { return c.moveToFirst() ? from(c) : null; }
        finally { c.close(); }
    }

    public List<Gifticon> all() {
        List<Gifticon> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("gifticons", null, null, null, null, null, "created_at DESC");
        try { while (c.moveToNext()) list.add(from(c)); }
        finally { c.close(); }
        return list;
    }

    public void delete(String id) {
        getWritableDatabase().delete("gifticons", "id=?", new String[]{id});
    }

    public void deleteAll() { getWritableDatabase().delete("gifticons", null, null); }

    private static ContentValues values(Gifticon g) {
        ContentValues v = new ContentValues();
        v.put("id", g.id); v.put("title", g.title); v.put("brand", g.brand); v.put("memo", g.memo);
        if (g.expiryDate == null) v.putNull("expiry"); else v.put("expiry", g.expiryDate);
        v.put("created_at", g.createdAt); v.put("updated_at", g.updatedAt); v.put("is_used", g.isUsed ? 1 : 0);
        if (g.usedAt == null) v.putNull("used_at"); else v.put("used_at", g.usedAt);
        v.put("notifications_enabled", g.notificationsEnabled ? 1 : 0);
        if (g.barcodePayload == null) v.putNull("barcode_payload"); else v.put("barcode_payload", g.barcodePayload);
        if (g.barcodeSymbology == null) v.putNull("barcode_symbology"); else v.put("barcode_symbology", g.barcodeSymbology);
        if (g.imagePath == null) v.putNull("image_path"); else v.put("image_path", g.imagePath);
        return v;
    }

    private static Gifticon from(Cursor c) {
        Gifticon g = new Gifticon();
        g.id = c.getString(c.getColumnIndexOrThrow("id"));
        g.title = c.getString(c.getColumnIndexOrThrow("title"));
        g.brand = c.getString(c.getColumnIndexOrThrow("brand"));
        g.memo = c.getString(c.getColumnIndexOrThrow("memo"));
        int i = c.getColumnIndexOrThrow("expiry"); g.expiryDate = c.isNull(i) ? null : c.getLong(i);
        g.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        g.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        g.isUsed = c.getInt(c.getColumnIndexOrThrow("is_used")) != 0;
        i = c.getColumnIndexOrThrow("used_at"); g.usedAt = c.isNull(i) ? null : c.getLong(i);
        g.notificationsEnabled = c.getInt(c.getColumnIndexOrThrow("notifications_enabled")) != 0;
        i = c.getColumnIndexOrThrow("barcode_payload"); g.barcodePayload = c.isNull(i) ? null : c.getString(i);
        i = c.getColumnIndexOrThrow("barcode_symbology"); g.barcodeSymbology = c.isNull(i) ? null : c.getString(i);
        i = c.getColumnIndexOrThrow("image_path"); g.imagePath = c.isNull(i) ? null : c.getString(i);
        return g;
    }
}
