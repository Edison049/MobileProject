package com.innovationai.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * 网络状态工具类
 * 用于检查网络连接状态和类型
 */
public class NetworkUtil {

    /**
     * 检查网络是否可用
     * @param context 上下文
     * @return true表示网络可用，false表示网络不可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * 获取网络类型
     * @param context 上下文
     * @return 网络类型字符串
     */
    public static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                switch (activeNetworkInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        return "WiFi";
                    case ConnectivityManager.TYPE_MOBILE:
                        return getMobileNetworkType(activeNetworkInfo.getSubtype());
                    default:
                        return "Unknown";
                }
            }
        }
        return "No Connection";
    }

    /**
     * 获取移动网络具体类型
     * @param networkType 网络类型代码
     * @return 具体的网络类型名称
     */
    private static String getMobileNetworkType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "Unknown";
        }
    }

    /**
     * 检查是否为WiFi连接
     * @param context 上下文
     * @return true表示WiFi连接，false表示其他连接或无连接
     */
    public static boolean isWiFiConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && 
                   activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                   activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * 检查是否为移动数据连接
     * @param context 上下文
     * @return true表示移动数据连接，false表示WiFi或其他连接
     */
    public static boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && 
                   activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE &&
                   activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * 显示网络状态提示
     * @param context 上下文
     */
    public static void showNetworkStatus(Context context) {
        if (isNetworkAvailable(context)) {
            String networkType = getNetworkType(context);
            Utils.showToast(context, "网络连接: " + networkType);
        } else {
            Utils.showToast(context, "网络不可用，请检查网络设置");
        }
    }
}