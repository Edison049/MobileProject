package com.innovationai.myapplication.util;

import android.content.Context;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

/**
 * 图片加载工具类
 * 封装Glide图片加载功能，提供统一的图片加载接口
 */
public class ImageLoaderUtil {

    // 默认占位图资源ID
    private static final int DEFAULT_PLACEHOLDER = android.R.drawable.ic_menu_gallery;
    private static final int DEFAULT_ERROR = android.R.drawable.ic_menu_report_image;

    /**
     * 加载普通图片
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView 目标ImageView
     */
    public static void loadImage(Context context, String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(DEFAULT_PLACEHOLDER);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(DEFAULT_PLACEHOLDER)
                .error(DEFAULT_ERROR)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载圆形图片（头像等）
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView 目标ImageView
     */
    public static void loadCircularImage(Context context, String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(DEFAULT_PLACEHOLDER);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(DEFAULT_PLACEHOLDER)
                .error(DEFAULT_ERROR)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * 加载圆角图片
     * @param context 上下文
     * @param imageUrl 图片URL
     * @param imageView 目标ImageView
     * @param cornerRadius 圆角半径
     */
    public static void loadRoundedImage(Context context, String imageUrl, ImageView imageView, int cornerRadius) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(DEFAULT_PLACEHOLDER);
            return;
        }

        RequestOptions options = new RequestOptions()
                .placeholder(DEFAULT_PLACEHOLDER)
                .error(DEFAULT_ERROR)
                .transform(new CenterCrop(), new RoundedCorners(cornerRadius))
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .into(imageView);
    }

    /**
     * 预加载图片到缓存
     * @param context 上下文
     * @param imageUrl 图片URL
     */
    public static void preloadImage(Context context, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(context)
                .load(imageUrl)
                .apply(options)
                .preload();
    }

    /**
     * 清除图片缓存
     * @param context 上下文
     */
    public static void clearImageCache(Context context) {
        Glide.get(context).clearMemory();
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
    }

    /**
     * 暂停图片加载
     * @param context 上下文
     */
    public static void pauseRequests(Context context) {
        Glide.with(context).pauseRequests();
    }

    /**
     * 恢复图片加载
     * @param context 上下文
     */
    public static void resumeRequests(Context context) {
        Glide.with(context).resumeRequests();
    }
}