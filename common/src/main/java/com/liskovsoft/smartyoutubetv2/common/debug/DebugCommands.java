package com.liskovsoft.smartyoutubetv2.common.debug;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;

/**
 * Debug broadcast receiver for testing via adb.
 *
 * Usage:
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd refresh_home
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd set_mode --es mode unified
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd set_mode --es mode rotating
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd set_mode --es mode all_expanded
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd toggle_pool --ei pool 0 --ez enabled true
 *   adb shell am broadcast -a com.smarttube.DEBUG --es cmd get_status
 */
public class DebugCommands extends BroadcastReceiver {
    private static final String TAG = "DebugCommands";
    public static final String ACTION = "com.smarttube.DEBUG";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String cmd = intent.getStringExtra("cmd");
        if (cmd == null) return;

        Log.d(TAG, "Command: " + cmd);
        GeneralData gd = GeneralData.instance(context);

        switch (cmd) {
            case "refresh_home":
                com.liskovsoft.youtubeapi.browse.v2.BrowseService2.setLastFullRefreshMs(0);
                BrowsePresenter bp = BrowsePresenter.instance(context);
                if (bp != null) bp.refresh();
                Log.d(TAG, "Home refresh triggered (REFRESH_HARD)");
                break;

            case "set_mode":
                String mode = intent.getStringExtra("mode");
                if ("unified".equals(mode)) {
                    gd.setDiscoveryMode(GeneralData.DISCOVERY_UNIFIED);
                } else if ("rotating".equals(mode)) {
                    gd.setDiscoveryMode(GeneralData.DISCOVERY_ROTATING);
                } else if ("all_expanded".equals(mode)) {
                    gd.setDiscoveryMode(GeneralData.DISCOVERY_ALL_EXPANDED);
                }
                Log.d(TAG, "Discovery mode set to: " + mode + " (" + gd.getDiscoveryMode() + ")");
                break;

            case "toggle_pool":
                int pool = intent.getIntExtra("pool", -1);
                boolean enabled = intent.getBooleanExtra("enabled", true);
                if (pool >= 0 && pool <= 5) {
                    gd.setPoolEnabled(pool, enabled);
                    Log.d(TAG, "Pool " + pool + " enabled=" + enabled);
                }
                break;

            case "get_status":
                int currentMode = gd.getDiscoveryMode();
                String modeName = currentMode == 0 ? "unified" : currentMode == 1 ? "rotating" : "all_expanded";
                StringBuilder pools = new StringBuilder();
                String[] poolNames = {"A-Music", "B-Lifestyle", "C-Mixed", "D-Movies", "E-Anime", "Language"};
                for (int i = 0; i < 6; i++) {
                    pools.append(poolNames[i]).append("=").append(gd.isPoolEnabled(i) ? "ON" : "OFF").append(" ");
                }
                int refreshCounter = com.liskovsoft.youtubeapi.browse.v2.BrowseService2.getRefreshCounter().get();
                Log.d(TAG, "STATUS: mode=" + modeName +
                        " refreshCounter=" + refreshCounter +
                        " pools=[" + pools.toString().trim() + "]" +
                        " unifiedShelf=" + com.liskovsoft.youtubeapi.browse.v2.BrowseService2.getUnifiedShelf() +
                        " allPoolsAtOnce=" + com.liskovsoft.youtubeapi.browse.v2.BrowseService2.getAllPoolsAtOnce() +
                        " rotatingPoolCount=" + com.liskovsoft.youtubeapi.browse.v2.BrowseService2.getRotatingPoolCount());
                break;
        }
    }
}
