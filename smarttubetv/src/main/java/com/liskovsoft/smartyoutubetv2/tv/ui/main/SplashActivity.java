package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Intent;
import android.os.Bundle;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.BuildConfig;
import com.liskovsoft.smartyoutubetv2.tv.ui.bridge.BridgeActivity;

public class SplashActivity extends MotherActivity implements SplashView {
    private static final String TAG = SplashActivity.class.getSimpleName();
    private Intent mNewIntent;
    private SplashPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.IS_BRIDGE) {
            // Bridge APK: the legacy package only exists to migrate users to
            // SmartTube Ash. Hand off immediately without booting the full app.
            startActivity(new Intent(this, BridgeActivity.class));
            finish();
            return;
        }

        // Prewarm search/player connections in background while splash shows.
        // This triggers TLS handshake + PoToken init so Phase 2 is fast later.
        prewarmConnections();

        mNewIntent = getIntent();

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();

        //finish();
    }

    /**
     * Background prewarm: send minimal requests to /search and /player endpoints
     * to initialize TLS, connection pool, PoToken, and visitorData.
     * Runs on IO thread, result discarded. UI is not blocked.
     */
    private void prewarmConnections() {
        new Thread(() -> {
            try {
                long t0 = System.currentTimeMillis();

                // Step 1: Auth + PoToken + AppInfo (biggest cold start cost ~10-15s on TV)
                com.liskovsoft.youtubeapi.service.YouTubeSignInService.instance().checkAuth();
                long t1 = System.currentTimeMillis();
                android.util.Log.d("Prewarm", "checkAuth: " + (t1 - t0) + "ms");

                // Step 2: AppInfo (visitorData, playerUrl, clientData, PoToken)
                com.liskovsoft.youtubeapi.app.AppService.instance().refreshCacheIfNeeded();
                long t2 = System.currentTimeMillis();
                android.util.Log.d("Prewarm", "AppInfo: " + (t2 - t1) + "ms");

                // Step 3: TLS warmup for API endpoints
                okhttp3.OkHttpClient client = com.liskovsoft.googlecommon.common.helpers.RetrofitOkHttpHelper.getClient();
                okhttp3.Request playerReq = new okhttp3.Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/player?key="
                        + com.liskovsoft.youtubeapi.common.helpers.AppConstants.API_KEY + "&prettyPrint=false")
                    .post(okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260401.08.00\"}},\"videoId\":\"dQw4w9WgXcQ\"}"))
                    .build();
                okhttp3.Response r1 = client.newCall(playerReq).execute();
                r1.close();
                long t3 = System.currentTimeMillis();
                android.util.Log.d("Prewarm", "TLS player: " + (t3 - t2) + "ms");

                okhttp3.Request searchReq = new okhttp3.Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/search?key="
                        + com.liskovsoft.youtubeapi.common.helpers.AppConstants.API_KEY + "&prettyPrint=false")
                    .post(okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20260401.08.00\"}},\"query\":\"a\"}"))
                    .build();
                okhttp3.Response r2 = client.newCall(searchReq).execute();
                r2.close();
                long t4 = System.currentTimeMillis();

                android.util.Log.d("Prewarm", "Total: " + (t4 - t0) + "ms (auth=" + (t1-t0) + " appInfo=" + (t2-t1) + " tls=" + (t4-t2) + ")");
            } catch (Exception e) {
                android.util.Log.d("Prewarm", "failed: " + e.getMessage());
            }
        }, "ConnectionPrewarm").start();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;

        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        try {
            finish();
        } catch (NullPointerException e) {
            // NullPointerException: Attempt to invoke virtual method 'void com.android.server.wm.DisplayContent.moveStack(com.android.server.wm.TaskStack, boolean)'
            e.printStackTrace();
        }
    }
}
