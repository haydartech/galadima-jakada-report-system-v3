package com.galadima.gja;

import android.Manifest;
import androidx.activity.ComponentActivity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.FileChooserParams;
import java.io.InputStream;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;
import java.io.ByteArrayOutputStream;
import android.util.Base64;
import java.util.EnumMap;
import java.util.Map;

public class MainActivity extends ComponentActivity {
    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private static final int FILE_PICK_REQUEST = 45;
    private final ActivityResultLauncher<ScanOptions> scanner = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) sendScan(result.getContents());
    });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Choose report image"), FILE_PICK_REQUEST);
                return true;
            }
        });
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/www/index.html");
    }
    private class Bridge {
        @JavascriptInterface public void printReport(String jobName) {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager)getSystemService(PRINT_SERVICE);
                String clean = jobName == null ? "GJA Student Report" : jobName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\.pdf$", "");
                pm.print(clean, webView.createPrintDocumentAdapter(clean), new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(new PrintAttributes.Resolution("gja", "GJA Print", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
            });
        }
        @JavascriptInterface public void generateQr(String contents, String callback) {
            try {
                Map<EncodeHintType,Object> qrHints = new EnumMap<>(EncodeHintType.class);
                qrHints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                qrHints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                BitMatrix matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 360, 360, qrHints);
                Bitmap bmp = Bitmap.createBitmap(360,360,Bitmap.Config.ARGB_8888);
                for(int x=0;x<360;x++) for(int y=0;y<360;y++) bmp.setPixel(x,y,matrix.get(x,y)?Color.BLACK:Color.WHITE);
                ByteArrayOutputStream out = new ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.PNG,100,out);
                String encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
                runOnUiThread(() -> webView.evaluateJavascript("window."+callback+"('"+encoded+"')", null));
            } catch(Exception e) { String message = e.getMessage() == null ? "QR generation failed" : e.getMessage(); String safe = message.replace("\\", "\\\\").replace("'", "\\'"); runOnUiThread(() -> { webView.evaluateJavascript("window.onQrError('"+safe+"')", null); Toast.makeText(MainActivity.this,"QR generation failed: "+message,Toast.LENGTH_LONG).show(); }); }
        }
        @JavascriptInterface public void openScanner() {
            runOnUiThread(() -> {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},44); return;
                }
                ScanOptions options = new ScanOptions(); options.setDesiredBarcodeFormats(ScanOptions.QR_CODE); options.setPrompt("Scan GJA report QR code"); options.setBeepEnabled(false); options.setOrientationLocked(false); scanner.launch(options);
            });
        }
        @JavascriptInterface public void scanGallery(String base64) {
            try {
                byte[] bytes=Base64.decode(base64, Base64.DEFAULT); Bitmap bmp=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                int w=bmp.getWidth(),h=bmp.getHeight(); int[] pixels=new int[w*h]; bmp.getPixels(pixels,0,w,0,0,w,h);
                RGBLuminanceSource source=new RGBLuminanceSource(w,h,pixels); BinaryBitmap binary=new BinaryBitmap(new HybridBinarizer(source));
                Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class); hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
                String result=new MultiFormatReader().decode(binary,hints).getText(); sendScan(result);
            } catch(Exception e) { runOnUiThread(() -> Toast.makeText(MainActivity.this,"No readable QR code found in this image",Toast.LENGTH_LONG).show()); }
        }
    }
    private void sendScan(String value) {
        String safe = value.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
        runOnUiThread(() -> webView.evaluateJavascript("window.onQrScanResult('"+safe+"')", null));
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==44 && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED) {
            ScanOptions options=new ScanOptions(); options.setDesiredBarcodeFormats(ScanOptions.QR_CODE); options.setPrompt("Scan GJA report QR code"); options.setBeepEnabled(false); options.setOrientationLocked(false); scanner.launch(options);
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICK_REQUEST) {
            Uri uri = resultCode == RESULT_OK && data != null ? data.getData() : null;
            if (fileChooserCallback != null) { fileChooserCallback.onReceiveValue(uri == null ? null : new Uri[]{uri}); fileChooserCallback = null; }
        }
    }
    @Override public void onBackPressed() { if(webView!=null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
