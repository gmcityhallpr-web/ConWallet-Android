package kr.co.conwallet;

import android.content.Context;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class BackupService {
    private BackupService() {}

    public static byte[] exportJson(Context c) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", 1);
        root.put("exportedAt", DateUtil.iso(System.currentTimeMillis()));
        JSONArray arr = new JSONArray();
        for (Gifticon g : GifticonDb.get(c).all()) {
            JSONObject o = new JSONObject();
            o.put("id", g.id);
            o.put("title", g.title);
            o.put("brand", g.brand);
            o.put("memo", g.memo);
            putNullable(o, "expiryDate", g.expiryDate == null ? null : DateUtil.iso(g.expiryDate));
            o.put("createdAt", DateUtil.iso(g.createdAt));
            o.put("updatedAt", DateUtil.iso(g.updatedAt));
            o.put("isUsed", g.isUsed);
            putNullable(o, "usedAt", g.usedAt == null ? null : DateUtil.iso(g.usedAt));
            o.put("notificationsEnabled", g.notificationsEnabled);
            putNullable(o, "barcodePayload", g.barcodePayload);
            putNullable(o, "barcodeSymbology", g.barcodeSymbology);
            String imageData = null;
            if (g.imagePath != null) {
                File f = new File(g.imagePath);
                if (f.exists()) imageData = Base64.encodeToString(readAll(f), Base64.NO_WRAP);
            }
            putNullable(o, "imageData", imageData);
            arr.put(o);
        }
        root.put("items", arr);
        return root.toString(2).getBytes(StandardCharsets.UTF_8);
    }

    public static int importJson(Context c, byte[] data) throws Exception {
        JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
        if (root.optInt("version", -1) != 1) throw new IOException("지원하지 않는 백업 버전입니다.");
        JSONArray arr = root.getJSONArray("items");
        GifticonDb db = GifticonDb.get(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Gifticon old = db.getById(o.getString("id"));
            Gifticon g = old == null ? new Gifticon() : old;
            g.id = o.getString("id");
            g.title = o.optString("title", "새 기프티콘");
            g.brand = o.optString("brand", "");
            g.memo = o.optString("memo", "");
            g.expiryDate = parseNullableDate(o, "expiryDate");
            Long created = DateUtil.parseIso(o.optString("createdAt", ""));
            Long updated = DateUtil.parseIso(o.optString("updatedAt", ""));
            g.createdAt = created == null ? System.currentTimeMillis() : created;
            g.updatedAt = updated == null ? g.createdAt : updated;
            g.isUsed = o.optBoolean("isUsed", false);
            g.usedAt = parseNullableDate(o, "usedAt");
            g.notificationsEnabled = o.optBoolean("notificationsEnabled", true);
            g.barcodePayload = nullableString(o, "barcodePayload");
            g.barcodeSymbology = nullableString(o, "barcodeSymbology");
            String b64 = nullableString(o, "imageData");
            if (b64 != null && !b64.isEmpty()) {
                byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
                if (old != null && old.imagePath != null) ImageStore.delete(old.imagePath);
                g.imagePath = ImageStore.saveBytes(c, bytes, g.id);
            }
            db.save(g);
            NotificationHelper.schedule(c, g);
        }
        return arr.length();
    }

    private static void putNullable(JSONObject o, String key, Object value) throws Exception {
        o.put(key, value == null ? JSONObject.NULL : value);
    }

    private static String nullableString(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) return null;
        String s = o.optString(key, null);
        return s == null || "null".equals(s) ? null : s;
    }

    private static Long parseNullableDate(JSONObject o, String key) {
        String s = nullableString(o, key);
        return s == null ? null : DateUtil.parseIso(s);
    }

    private static byte[] readAll(File f) throws IOException {
        long len = f.length();
        if (len > Integer.MAX_VALUE) throw new IOException("이미지가 너무 큽니다.");
        byte[] data = new byte[(int) len];
        try (FileInputStream in = new FileInputStream(f)) {
            int off = 0, n;
            while (off < data.length && (n = in.read(data, off, data.length - off)) > 0) off += n;
            if (off != data.length) throw new IOException("이미지를 끝까지 읽지 못했습니다.");
        }
        return data;
    }
}
