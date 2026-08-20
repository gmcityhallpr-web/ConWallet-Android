package kr.co.conwallet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public final class ImageStore {
    private ImageStore() {}

    public static String saveFromUri(Context c, Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, bounds); }
        int max = Math.max(bounds.outWidth, bounds.outHeight);
        int sample = 1;
        while (max / sample > 3000) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(in, null, opts); }
        if (bitmap == null) throw new IOException("이미지를 읽을 수 없습니다.");
        File dir = new File(c.getFilesDir(), "gifticon_images");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("이미지 폴더를 만들 수 없습니다.");
        File out = new File(dir, UUID.randomUUID() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos); }
        bitmap.recycle();
        return out.getAbsolutePath();
    }

    public static String saveBytes(Context c, byte[] bytes, String id) throws IOException {
        File dir = new File(c.getFilesDir(), "gifticon_images");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("이미지 폴더를 만들 수 없습니다.");
        File out = new File(dir, id + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(bytes); }
        return out.getAbsolutePath();
    }

    public static void delete(String path) {
        if (path != null) { try { new File(path).delete(); } catch (Exception ignored) {} }
    }
}
