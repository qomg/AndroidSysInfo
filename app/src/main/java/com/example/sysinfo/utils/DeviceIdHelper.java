package com.example.sysinfo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.lang.reflect.Method;
import java.util.UUID;

@SuppressWarnings("unused")
public class DeviceIdHelper {
    private static final String PREFS_FILE = "device_id_prefs";
    private static final String PREFS_DEVICE_ID = "device_id";

    public static String getDeviceId(Context context) {
        // 1. 尝试获取 ANDROID_ID
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        // 过滤已知无效值
        if (androidId != null &&
                !androidId.isEmpty() &&
                !"9774d56d682e549c".equals(androidId) &&
                !"0000000000000000".equals(androidId)) {
            return androidId;
        }

        // 2. 回退到本地生成的 UUID
        SharedPreferences prefs = context.getSharedPreferences(
                PREFS_FILE,
                Context.MODE_PRIVATE
        );
        String deviceId = prefs.getString(PREFS_DEVICE_ID, null);

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            prefs.edit().putString(PREFS_DEVICE_ID, deviceId).apply();
        }

        return deviceId;
    }

    public static String getSerialNo() {
        return get("dev.serialno", get("ro.serialno", ""));
    }

    @SuppressLint({"PrivateApi"})
    private static String get(String key, String def) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method method = clazz.getDeclaredMethod("get", String.class, String.class);
            method.setAccessible(true);
            return (String)method.invoke((Object)null, key, def);
        } catch (Exception e) {
            return def != null ? def : "";
        }
    }
}

/*
runCatching {
    val deviceId = DeviceIdHelper.getDeviceId(activity)
    println("deviceId: $deviceId")
}.onFailure {
    it.printStackTrace()
}
runCatching {
    val serialNo = DeviceIdHelper.getSerialNo()
    println("serialNo: $serialNo")
}.onFailure {
    it.printStackTrace()
}
*/