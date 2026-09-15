package com.gengroup.packager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    // Эта строка подставляется автоматически при сборке (см. workflow)
    private static final String TARGET_FILENAME = "TARGET_FILENAME_PLACEHOLDER";
    private static final String TARGET_SUBFOLDER = "GEN";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();

        if (!hasAllFilesAccess()) {
            requestAllFilesAccess();
            return;
        }
        showPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (hasAllFilesAccess() && webView == null) {
            showPlayer();
        }
    }

    private boolean hasAllFilesAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void requestAllFilesAccess() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivityForResult(intent, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllFilesAccess()) {
            showPlayer();
        }
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private File targetFile() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), TARGET_SUBFOLDER);
        return new File(dir, TARGET_FILENAME);
    }

    private void showPlayer() {
        File file = targetFile();
        if (!file.exists()) {
            showMissingFileScreen(file);
            return;
        }

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Чёрный фон вместо "пустого окна", пока грузится новый канал
        webView.setBackgroundColor(0xFF000000);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        setContentView(webView);
        webView.loadUrl(Uri.fromFile(file).toString());
    }

    private void showMissingFileScreen(File expected) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFF000000);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(60, 60, 60, 60);

        TextView text = new TextView(this);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(18);
        text.setGravity(Gravity.CENTER);
        text.setText("Файл не найден:\n" + expected.getAbsolutePath()
                + "\n\nСкопируйте файл в эту папку и нажмите кнопку ниже.");
        layout.addView(text);

        Button retry = new Button(this);
        retry.setText("Проверить ещё раз");
        retry.setOnClickListener(v -> showPlayer());
        layout.addView(retry);

        setContentView(layout);
    }
}
