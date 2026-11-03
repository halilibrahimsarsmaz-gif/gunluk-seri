package com.gunlukseri.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.window.OnBackInvokedDispatcher;

public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "App");
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }
    }

    private void handleBack() {
        web.evaluateJavascript("window.onBack && window.onBack()", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.onNativeResume && window.onNativeResume()", null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (web != null) web.evaluateJavascript("window.onNativeResume && window.onNativeResume()", null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    class Bridge {
        @JavascriptInterface
        public void setBars(final String color, final boolean light) {
            runOnUiThread(() -> {
                try {
                    int c = Color.parseColor(color.trim());
                    Window w = getWindow();
                    w.setStatusBarColor(c);
                    w.setNavigationBarColor(c);
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        WindowInsetsController ic = w.getInsetsController();
                        if (ic != null) {
                            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                     | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                            ic.setSystemBarsAppearance(light ? mask : 0, mask);
                        }
                    }
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface
        public void sync(String remindersJson, String date, String doneIds) {
            Reminders.sync(MainActivity.this, remindersJson, date, doneIds);
        }

        @JavascriptInterface
        public String takeDone() {
            return Reminders.takePending(MainActivity.this);
        }

        @JavascriptInterface
        public boolean notifOk() {
            if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
            android.app.NotificationManager nm = getSystemService(android.app.NotificationManager.class);
            return nm.areNotificationsEnabled();
        }

        @JavascriptInterface
        public void askNotif() {
            runOnUiThread(() -> {
                if (android.os.Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 7);
                }
            });
        }

        @JavascriptInterface
        public void openNotifSettings() {
            runOnUiThread(() -> {
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                try { startActivity(i); }
                catch (Exception e) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
            });
        }

        @JavascriptInterface
        public void copy(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Günlük Seri yedeği", text));
        }
    }
}
