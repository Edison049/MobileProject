package com.innovationai.myapplication.util;

import android.content.Context;

import androidx.media3.datasource.RawResourceDataSource;

import com.innovationai.myapplication.model.Movie;

/**
 * 电影媒体资源辅助工具
 * 支持本地资源引用与远程URL共存
 */
public final class MovieMediaUtil {
    public static final String DRAWABLE_PREFIX = "drawable://";
    public static final String RAW_PREFIX = "raw://";

    private MovieMediaUtil() {
    }

    public static String drawableRef(String resourceName) {
        return DRAWABLE_PREFIX + resourceName;
    }

    public static String rawRef(String resourceName) {
        return RAW_PREFIX + resourceName;
    }

    public static void hydrateLocalResources(Context context, Movie movie) {
        if (movie == null) {
            return;
        }

        String posterUrl = movie.getPosterUrl();
        if (posterUrl != null && posterUrl.startsWith(DRAWABLE_PREFIX)) {
            String resourceName = posterUrl.substring(DRAWABLE_PREFIX.length());
            int resourceId = context.getResources()
                    .getIdentifier(resourceName, "drawable", context.getPackageName());
            if (resourceId != 0) {
                movie.setPosterResourceId(resourceId);
            }
        }

        String previewVideoUrl = movie.getPreviewVideoUrl();
        if (previewVideoUrl != null && previewVideoUrl.startsWith(RAW_PREFIX)) {
            String resourceName = previewVideoUrl.substring(RAW_PREFIX.length());
            int rawResId = context.getResources()
                    .getIdentifier(resourceName, "raw", context.getPackageName());
            if (rawResId != 0) {
                movie.setPreviewVideoUrl(
                        RawResourceDataSource.buildRawResourceUri(rawResId).toString()
                );
            }
        }
    }
}
