package com.innovationai.myapplication.util;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.media3.datasource.RawResourceDataSource;

import com.innovationai.myapplication.model.Movie;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 电影媒体资源辅助工具
 * 支持本地资源引用与远程URL共存
 */
public final class MovieMediaUtil {
    public static final String DRAWABLE_PREFIX = "drawable://";
    public static final String RAW_PREFIX = "raw://";
    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:watch\\?v=|embed/|shorts/|live/))([A-Za-z0-9_-]{11})",
            Pattern.CASE_INSENSITIVE
    );

    private MovieMediaUtil() {
    }

    public static String drawableRef(String resourceName) {
        return DRAWABLE_PREFIX + resourceName;
    }

    public static String rawRef(String resourceName) {
        return RAW_PREFIX + resourceName;
    }

    public static boolean isYouTubeUrl(String url) {
        return extractYouTubeVideoId(url) != null;
    }

    public static String extractYouTubeVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        String trimmedUrl = url.trim();
        String videoIdFromUri = extractYouTubeVideoIdFromUri(trimmedUrl);
        if (videoIdFromUri != null) {
            return videoIdFromUri;
        }

        Matcher matcher = YOUTUBE_VIDEO_ID_PATTERN.matcher(trimmedUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String buildYouTubeEmbedUrl(String url) {
        String videoId = extractYouTubeVideoId(url);
        if (videoId == null) {
            return null;
        }
        return "https://www.youtube-nocookie.com/embed/" + videoId
                + "?autoplay=1&mute=1&playsinline=1&controls=1&rel=0"
                + "&modestbranding=1&iv_load_policy=3&fs=0&enablejsapi=1";
    }

    public static boolean isRemoteUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    public static boolean isYouTubeEmbedUrl(String url) {
        if (!isRemoteUrl(url)) {
            return false;
        }

        Uri uri = Uri.parse(url.trim());
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (normalizedHost.startsWith("www.")) {
            normalizedHost = normalizedHost.substring(4);
        }

        if (!"youtube.com".equals(normalizedHost) && !"youtube-nocookie.com".equals(normalizedHost)) {
            return false;
        }

        java.util.List<String> segments = uri.getPathSegments();
        return !segments.isEmpty() && "embed".equals(segments.get(0));
    }

    public static String copyPosterToAppStorage(Context context, Uri sourceUri) throws IOException {
        if (context == null || sourceUri == null) {
            throw new IOException("The poster file is invalid");
        }

        File posterDirectory = new File(context.getFilesDir(), "movie_posters");
        if (!posterDirectory.exists() && !posterDirectory.mkdirs()) {
            throw new IOException("Unable to create the poster directory");
        }

        String extension = resolveFileExtension(context, sourceUri);
        File outputFile = new File(
                posterDirectory,
                "poster_" + UUID.randomUUID().toString().replace("-", "") + extension
        );

        try (InputStream inputStream = openInputStream(context, sourceUri);
             FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {
            if (inputStream == null) {
                throw new IOException("Unable to read the selected poster file");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        return Uri.fromFile(outputFile).toString();
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

    private static String resolveFileExtension(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        String extension = mimeType == null
                ? null
                : MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (extension != null && !extension.trim().isEmpty()) {
            return "." + extension.toLowerCase(Locale.ROOT);
        }

        String path = uri.getPath();
        if (path != null) {
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < path.length() - 1) {
                return path.substring(dotIndex);
            }
        }
        return ".jpg";
    }

    private static InputStream openInputStream(Context context, Uri uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null) {
                throw new IOException("The file path is invalid");
            }
            return new FileInputStream(new File(path));
        }
        return context.getContentResolver().openInputStream(uri);
    }

    private static String extractYouTubeVideoIdFromUri(String url) {
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            if (host == null || host.trim().isEmpty()) {
                return null;
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("www.")) {
                normalizedHost = normalizedHost.substring(4);
            }

            if ("youtu.be".equals(normalizedHost)) {
                return normalizeYouTubeVideoId(uri.getLastPathSegment());
            }

            if ("youtube.com".equals(normalizedHost)
                    || "m.youtube.com".equals(normalizedHost)
                    || "youtube-nocookie.com".equals(normalizedHost)) {
                String queryVideoId = normalizeYouTubeVideoId(uri.getQueryParameter("v"));
                if (queryVideoId != null) {
                    return queryVideoId;
                }

                java.util.List<String> segments = uri.getPathSegments();
                if (!segments.isEmpty()) {
                    String firstSegment = segments.get(0);
                    if ("embed".equals(firstSegment)
                            || "shorts".equals(firstSegment)
                            || "live".equals(firstSegment)) {
                        return normalizeYouTubeVideoId(segments.get(segments.size() - 1));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static String normalizeYouTubeVideoId(String candidate) {
        if (candidate == null) {
            return null;
        }

        String trimmedCandidate = candidate.trim();
        return trimmedCandidate.matches("[A-Za-z0-9_-]{11}") ? trimmedCandidate : null;
    }
}
