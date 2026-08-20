package kr.co.conwallet;

import java.util.Calendar;
import java.util.UUID;

public class Gifticon {
    public String id = UUID.randomUUID().toString();
    public String title = "새 기프티콘";
    public String brand = "";
    public String memo = "";
    public Long expiryDate = null;
    public long createdAt = System.currentTimeMillis();
    public long updatedAt = System.currentTimeMillis();
    public boolean isUsed = false;
    public Long usedAt = null;
    public boolean notificationsEnabled = true;
    public String barcodePayload = null;
    public String barcodeSymbology = null;
    public String imagePath = null;

    public boolean isExpired() {
        if (expiryDate == null) return false;
        Calendar now = Calendar.getInstance();
        zeroTime(now);
        Calendar expiry = Calendar.getInstance();
        expiry.setTimeInMillis(expiryDate);
        zeroTime(expiry);
        return expiry.before(now);
    }

    public Integer daysUntilExpiry() {
        if (expiryDate == null) return null;
        Calendar now = Calendar.getInstance();
        zeroTime(now);
        Calendar exp = Calendar.getInstance();
        exp.setTimeInMillis(expiryDate);
        zeroTime(exp);
        long diff = exp.getTimeInMillis() - now.getTimeInMillis();
        return (int) Math.floor(diff / 86400000.0);
    }

    public String statusText() {
        if (isUsed) return "사용 완료";
        if (isExpired()) return "기간 만료";
        return "사용 가능";
    }

    private static void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }
}
