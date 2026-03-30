package com.innovationai.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.text.TextUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firebase工具类
 * 当前主要负责判断是否已完成Firebase配置并返回Firestore实例
 */
public final class FirebaseUtil {
    private static FirebaseFirestore firestore;

    private FirebaseUtil() {
    }

    public static boolean isFirebaseConfigured(Context context) {
        int appIdRes = context.getResources()
                .getIdentifier("google_app_id", "string", context.getPackageName());
        if (appIdRes == 0) {
            return false;
        }

        String googleAppId = context.getString(appIdRes);
        return !TextUtils.isEmpty(googleAppId);
    }

    public static FirebaseFirestore getFirestore(Context context) {
        if (!isFirebaseConfigured(context)) {
            return null;
        }

        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context);
        }

        if (firestore == null) {
            firestore = FirebaseFirestore.getInstance();
        }
        return firestore;
    }

    public static boolean hasUsableNetwork(Context context) {
        ConnectivityManager connectivityManager = context.getSystemService(ConnectivityManager.class);
        if (connectivityManager == null) {
            return false;
        }

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
