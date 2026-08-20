package kr.co.conwallet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GifticonTextParser {
    public static class Result {
        public String title = "새 기프티콘";
        public String brand = "";
        public Long expiryDate = null;
    }

    private static final String[][] BRANDS = new String[][]{
        {"스타벅스","스타벅스","STARBUCKS"}, {"투썸플레이스","투썸플레이스","ATWOSOMEPLACE","TWOSOMEPLACE"},
        {"이디야커피","이디야","EDIYA"}, {"메가MGC커피","메가MGC","메가커피","MEGAMGC","MEGACOFFEE"},
        {"컴포즈커피","컴포즈","COMPOSECOFFEE"}, {"빽다방","빽다방","PAIK'SCOFFEE","PAIKSCOFFEE"},
        {"커피빈","커피빈","COFFEEBEAN"}, {"할리스","할리스","HOLLYS"}, {"폴바셋","폴바셋","PAULBASSETT"},
        {"파리바게뜨","파리바게뜨","PARISBAGUETTE"}, {"뚜레쥬르","뚜레쥬르","TOUSLESJOURS"},
        {"배스킨라빈스","배스킨라빈스","베스킨라빈스","BASKINROBBINS"}, {"던킨","던킨","DUNKIN"},
        {"교촌치킨","교촌","KYOCHON"}, {"BHC","BHC"}, {"BBQ","BBQ"}, {"굽네치킨","굽네","GOOBNE"},
        {"네네치킨","네네치킨","NENECHICKEN"}, {"맘스터치","맘스터치","MOM'STOUCH","MOMSTOUCH"},
        {"맥도날드","맥도날드","MCDONALD"}, {"버거킹","버거킹","BURGERKING"}, {"롯데리아","롯데리아","LOTTERIA"},
        {"서브웨이","서브웨이","SUBWAY"}, {"도미노피자","도미노피자","DOMINO'S","DOMINOS"}, {"피자헛","피자헛","PIZZAHUT"},
        {"CU","CU편의점","CU"}, {"GS25","GS25"}, {"세븐일레븐","세븐일레븐","7-ELEVEN","7ELEVEN"},
        {"이마트24","이마트24","EMART24"}, {"올리브영","올리브영","OLIVEYOUNG"},
        {"네이버페이","네이버페이","NAVERPAY"}, {"카카오페이","카카오페이","KAKAOPAY"},
        {"신세계상품권","신세계상품권","SHINSEGAE"}, {"문화상품권","문화상품권","CULTURELAND"}
    };

    private static final String[] META = {"유효기간","사용기간","교환기간","만료","쿠폰번호","바코드","사용방법","이용안내","주의사항","사용처","교환처","주문번호","발행일","결제","선물받기","gifticon","coupon","valid until"};
    private static final String[] PRODUCTS = {"아메리카노","라떼","커피","케이크","세트","치킨","피자","버거","상품권","금액권","교환권","음료","아이스크림","도넛","샌드위치"};

    private static final Pattern[] DATE_PATTERNS = new Pattern[]{
        Pattern.compile("(?<!\\d)(20\\d{2})\\s*[년./-]\\s*(1[0-2]|0?[1-9])\\s*[월./-]\\s*(3[01]|[12]\\d|0?[1-9])\\s*일?(?!\\d)"),
        Pattern.compile("(?<!\\d)(\\d{2})\\s*[./-]\\s*(0?[1-9]|1[0-2])\\s*[./-]\\s*(0?[1-9]|[12]\\d|3[01])(?!\\d)"),
        Pattern.compile("(?<!\\d)(20\\d{2})(0[1-9]|1[0-2])([0-2]\\d|3[01])(?!\\d)")
    };

    private GifticonTextParser() {}

    public static Result parse(List<String> lines) {
        Result r = new Result();
        String full = join(lines, "\n");
        r.brand = inferBrand(full);
        r.title = inferTitle(lines, r.brand);
        r.expiryDate = inferExpiry(full);
        return r;
    }

    private static String inferBrand(String text) {
        String normalized = text.toUpperCase(Locale.ROOT).replace(" ", "");
        for (String[] b : BRANDS) {
            for (int i=1;i<b.length;i++) {
                String alias = b[i].toUpperCase(Locale.ROOT).replace(" ", "");
                if (normalized.contains(alias)) return b[0];
            }
        }
        return "";
    }

    private static String inferTitle(List<String> lines, String brand) {
        String best = null; int bestScore = Integer.MIN_VALUE;
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.length() < 2 || line.length() > 60) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            boolean skip = false;
            for (String k : META) if (lower.contains(k.toLowerCase(Locale.ROOT))) { skip = true; break; }
            if (skip || looksLikeDate(line) || looksBarcodeNumber(line)) continue;
            int score = 0;
            if (line.length() >= 4 && line.length() <= 36) score += 4;
            for (String k : PRODUCTS) if (lower.contains(k.toLowerCase(Locale.ROOT))) { score += 5; break; }
            if (!brand.isEmpty() && lower.contains(brand.toLowerCase(Locale.ROOT))) score -= 3;
            if (Pattern.compile("[가-힣A-Za-z]").matcher(line).find()) score += 2;
            if (Pattern.compile("\\d{4,}").matcher(line).find()) score -= 2;
            if (lower.contains("http") || lower.contains("www.")) score -= 8;
            if (score > bestScore) { bestScore = score; best = line; }
        }
        if (best != null && bestScore > 0) return best;
        return brand.isEmpty() ? "새 기프티콘" : brand + " 기프티콘";
    }

    private static boolean looksLikeDate(String s) {
        return Pattern.compile("(?<!\\d)(?:20)?\\d{2}\\s*[년./-]\\s*\\d{1,2}\\s*[월./-]\\s*\\d{1,2}").matcher(s).find();
    }

    private static boolean looksBarcodeNumber(String s) {
        int digits=0, letters=0;
        for (char ch : s.toCharArray()) { if (Character.isDigit(ch)) digits++; if (Character.isLetter(ch)) letters++; }
        return digits >= 10 && letters <= 2;
    }

    private static Long inferExpiry(String text) {
        List<Candidate> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int pi=0; pi<DATE_PATTERNS.length; pi++) {
            Matcher m = DATE_PATTERNS[pi].matcher(text);
            while (m.find()) {
                try {
                    int year = Integer.parseInt(m.group(1)); if (pi == 1) year += 2000;
                    int month = Integer.parseInt(m.group(2)); int day = Integer.parseInt(m.group(3));
                    Calendar c = Calendar.getInstance(); c.setLenient(false); c.set(year, month-1, day, 23, 59, 59); c.set(Calendar.MILLISECOND, 999);
                    long date = c.getTimeInMillis();
                    int start = Math.max(0, m.start()-30), end = Math.min(text.length(), m.end()+30);
                    String context = text.substring(start, end).toLowerCase(Locale.ROOT);
                    int score = 0;
                    String[] kws = {"유효","사용","교환","만료","까지","valid","expiry","expire"};
                    for (String k : kws) if (context.contains(k)) { score += 12; break; }
                    long days = (date-now)/86400000L;
                    if (days >= -1) score += 5;
                    if (days >= 0 && days <= 1500) score += 3;
                    if (days < -365) score -= 10;
                    candidates.add(new Candidate(date, score));
                } catch (Exception ignored) {}
            }
        }
        if (candidates.isEmpty()) return null;
        Collections.sort(candidates, (a,b) -> a.score == b.score ? Long.compare(b.date,a.date) : Integer.compare(b.score,a.score));
        return candidates.get(0).date;
    }

    private static String join(List<String> items, String sep) {
        StringBuilder b = new StringBuilder();
        for (int i=0;i<items.size();i++) { if (i>0) b.append(sep); b.append(items.get(i)); }
        return b.toString();
    }

    private static class Candidate { long date; int score; Candidate(long d,int s){date=d;score=s;} }
}
