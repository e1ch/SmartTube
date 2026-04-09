package com.liskovsoft.smartyoutubetv2.tv.ui.bridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * One-way migration screen shown by the bridge APK (flavor: stbridge).
 *
 * Bridge APK keeps the legacy package name ({@code org.smarttube.beta}) so it
 * can install as an in-place update over existing installs. Its only purpose
 * is to tell users that the fork has moved to {@code SmartTube Ash} and hand
 * them off to the GitHub release page where the new APK lives.
 */
public class BridgeActivity extends Activity {
    private static final String RELEASE_URL =
            "https://github.com/e1ch/SmartTube/releases/latest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new AlertDialog.Builder(this)
                .setTitle("SmartTube 已更名為 SmartTube Ash")
                .setMessage(
                        "此舊版本將不再更新。\n\n" +
                        "請至以下網址下載並安裝新版 SmartTube Ash：\n" +
                        RELEASE_URL + "\n\n" +
                        "安裝完成後，您可以手動解除安裝此舊版 app。")
                .setCancelable(false)
                .setPositiveButton("開啟下載頁", (dialog, which) -> {
                    openReleasePage();
                    finish();
                })
                .setNegativeButton("離開", (dialog, which) -> finish())
                .show();
    }

    private void openReleasePage() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_URL));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
