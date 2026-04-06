package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

/**
 * Detects weak devices and applies playback constraints.
 * Weak device: armeabi-v7a + Android ≤ 7.1 + RAM ≤ 2GB
 */
public class DeviceCapabilityHelper {

    public static boolean isLowEndDevice(Context context) {
        // Check ABI
        boolean isArm32 = false;
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("armeabi-v7a".equals(abi)) { isArm32 = true; break; }
        }
        if (!isArm32) return false;

        // Check Android version
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) return false; // > Android 7.1

        // Check RAM
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long totalRamMB = mi.totalMem / (1024 * 1024);
            return totalRamMB <= 2048;
        } catch (Exception e) {
            return true; // assume low-end if can't detect
        }
    }
}
