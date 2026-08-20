package kr.co.conwallet;

import android.content.Context;
import android.net.Uri;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class GifticonImageAnalyzer {
    public interface Callback { void onSuccess(Analysis a); void onError(Exception e); }
    public static class Analysis {
        public String recognizedText = "";
        public String inferredTitle = "새 기프티콘";
        public String inferredBrand = "";
        public Long inferredExpiryDate = null;
        public String barcodePayload = null;
        public String barcodeSymbology = null;
    }

    private GifticonImageAnalyzer() {}

    public static void analyze(Context c, Uri uri, Callback callback) {
        final InputImage image;
        try { image = InputImage.fromFilePath(c, uri); }
        catch (IOException e) { callback.onError(e); return; }

        Analysis out = new Analysis();
        AtomicInteger pending = new AtomicInteger(2);
        final Exception[] firstError = new Exception[1];
        Runnable finish = () -> {
            if (pending.decrementAndGet() == 0) {
                if (out.recognizedText.isEmpty() && firstError[0] != null) callback.onError(firstError[0]);
                else callback.onSuccess(out);
            }
        };

        TextRecognizer recognizer = TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        recognizer.process(image)
            .addOnSuccessListener(text -> {
                List<String> lines = new ArrayList<>();
                for (Text.TextBlock block : text.getTextBlocks()) for (Text.Line line : block.getLines()) lines.add(line.getText());
                out.recognizedText = text.getText();
                GifticonTextParser.Result parsed = GifticonTextParser.parse(lines);
                out.inferredTitle = parsed.title;
                out.inferredBrand = parsed.brand;
                out.inferredExpiryDate = parsed.expiryDate;
            })
            .addOnFailureListener(e -> { firstError[0] = e; })
            .addOnCompleteListener(t -> { recognizer.close(); finish.run(); });

        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(image)
            .addOnSuccessListener(list -> {
                if (!list.isEmpty()) {
                    Barcode b = list.get(0);
                    out.barcodePayload = b.getRawValue();
                    out.barcodeSymbology = formatName(b.getFormat());
                }
            })
            .addOnFailureListener(e -> { if (firstError[0] == null) firstError[0] = e; })
            .addOnCompleteListener(t -> { scanner.close(); finish.run(); });
    }

    private static String formatName(int f) {
        switch (f) {
            case Barcode.FORMAT_QR_CODE: return "QR";
            case Barcode.FORMAT_CODE_128: return "CODE 128";
            case Barcode.FORMAT_CODE_39: return "CODE 39";
            case Barcode.FORMAT_CODE_93: return "CODE 93";
            case Barcode.FORMAT_CODABAR: return "CODABAR";
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_AZTEC: return "AZTEC";
            case Barcode.FORMAT_DATA_MATRIX: return "DATA MATRIX";
            default: return "자동 감지";
        }
    }
}
