package com.innovationai.myapplication.activity;

import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.ui.PlayerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.google.android.material.button.MaterialButton;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 电影详情Activity
 * 展示电影详细信息，支持视频预览和购买功能
 */
public class MovieDetailActivity extends AppCompatActivity {
    private static final String TAG = "MovieDetailActivity";
    private static final long SEEK_INTERVAL_MS = 10_000L;
    private static final long CONTROLS_HIDE_DELAY_MS = 3_000L;
    private static final String YOUTUBE_EMBED_ORIGIN = "https://appassets.androidplatform.net";
    private final AppRepository repository = AppRepository.getInstance();

    // UI 组件
    private ImageButton backButton;
    private ImageView moviePosterLarge;
    private TextView movieTitleDetail;
    private TextView movieRatingDetail;
    private TextView movieGenreDetail;
    private TextView movieDurationDetail;
    private TextView moviePriceDetail;
    private FrameLayout videoPreviewPlayer;
    private PlayerView videoView;
    private WebView youtubeWebView;
    private View gradientOverlay;
    private SeekBar seekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private ImageButton playPauseButton;
    private ImageButton rewindButton;
    private ImageButton fastForwardButton;
    private ImageView centerPlayIndicator;
    private View controlPanel;
    private TextView movieDirectorDetail;
    private TextView movieCastDetail;
    private TextView movieDescriptionDetail;
    private MaterialButton addToCartButton;
    private MaterialButton buyNowButton;

    // 播放器相关
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private Runnable hideRunnable;
    private ExoPlayer player;
    private boolean isPlaying = false;
    private boolean isPrepared = false;
    private boolean isUserSeeking = false;
    private boolean isEmbeddedWebPlayer = false;
    private long videoDurationMs = 0L;
    private long pendingSeekPositionMs = C.TIME_UNSET;

    // 数据
    private String movieId;
    private Movie currentMovie;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        movieId = getIntent().getStringExtra(Constants.EXTRA_MOVIE_ID);
        if (movieId == null) {
            Utils.showToast(this, "Movie information is invalid");
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadMovieDetails();
        loadUserInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null && player.isPlaying()) {
            player.pause();
        }
        if (youtubeWebView != null) {
            youtubeWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (youtubeWebView != null) {
            youtubeWebView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        hideHandler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (youtubeWebView != null) {
            youtubeWebView.destroy();
            youtubeWebView = null;
        }
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        moviePosterLarge = findViewById(R.id.movie_poster_large);
        movieTitleDetail = findViewById(R.id.movie_title_detail);
        movieRatingDetail = findViewById(R.id.movie_rating_detail);
        movieGenreDetail = findViewById(R.id.movie_genre_detail);
        movieDurationDetail = findViewById(R.id.movie_duration_detail);
        moviePriceDetail = findViewById(R.id.movie_price_detail);
        videoPreviewPlayer = findViewById(R.id.video_preview_player);

        if (videoPreviewPlayer != null) {
            videoView = videoPreviewPlayer.findViewById(R.id.video_view);
            youtubeWebView = videoPreviewPlayer.findViewById(R.id.youtube_web_view);
            gradientOverlay = videoPreviewPlayer.findViewById(R.id.player_gradient_overlay);
            seekBar = videoPreviewPlayer.findViewById(R.id.seek_bar);
            currentTimeText = videoPreviewPlayer.findViewById(R.id.current_time_text);
            totalTimeText = videoPreviewPlayer.findViewById(R.id.total_time_text);
            playPauseButton = videoPreviewPlayer.findViewById(R.id.play_pause_button);
            rewindButton = videoPreviewPlayer.findViewById(R.id.rewind_button);
            fastForwardButton = videoPreviewPlayer.findViewById(R.id.fast_forward_button);
            centerPlayIndicator = videoPreviewPlayer.findViewById(R.id.center_play_indicator);
            controlPanel = videoPreviewPlayer.findViewById(R.id.control_panel);
        }

        movieDirectorDetail = findViewById(R.id.movie_director_detail);
        movieCastDetail = findViewById(R.id.movie_cast_detail);
        movieDescriptionDetail = findViewById(R.id.movie_description_detail);
        addToCartButton = findViewById(R.id.add_to_cart_button);
        buyNowButton = findViewById(R.id.buy_now_button);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        addToCartButton.setOnClickListener(v -> addToCart());
        buyNowButton.setOnClickListener(v -> buyNow());
    }

    /**
     * 加载电影详情
     */
    private void loadMovieDetails() {
        repository.loadMovieById(this, movieId, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(Movie data) {
                currentMovie = data;
                displayMovieDetails();
                setupVideoPlayer();
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(MovieDetailActivity.this, errorMessage);
                finish();
            }
        });
    }

    /**
     * 显示电影详情
     */
    private void displayMovieDetails() {
        if (currentMovie == null) {
            return;
        }

        movieTitleDetail.setText(currentMovie.getTitle());
        movieRatingDetail.setText(String.format(Locale.getDefault(), "%.1f★", currentMovie.getRating()));
        movieGenreDetail.setText(currentMovie.getGenre());
        movieDurationDetail.setText("120 min");
        moviePriceDetail.setText("Price: " + currentMovie.getPrice() + " credits");
        movieDirectorDetail.setText(currentMovie.getDirector());
        movieCastDetail.setText(currentMovie.getCast());
        String description = currentMovie.getDescription();
        movieDescriptionDetail.setText(description == null || description.trim().isEmpty()
                ? "No synopsis available"
                : description);

        if (currentMovie.isLocalImage()) {
            Glide.with(this)
                    .load(currentMovie.getPosterResourceId())
                    .transform(new CenterCrop())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(moviePosterLarge);
        } else {
            Glide.with(this)
                    .load(currentMovie.getPosterUrl())
                    .transform(new CenterCrop())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(moviePosterLarge);
        }
    }

    /**
     * 设置视频播放器
     */
    private void setupVideoPlayer() {
        if (currentMovie == null || currentMovie.getPreviewVideoUrl() == null
                || currentMovie.getPreviewVideoUrl().trim().isEmpty()) {
            Utils.showToast(this, "Video URL is empty");
            return;
        }

        if (videoView == null || seekBar == null || currentTimeText == null
                || totalTimeText == null || playPauseButton == null
                || rewindButton == null || fastForwardButton == null
                || controlPanel == null) {
            Utils.showToast(this, "Failed to initialize the player");
            return;
        }

        releasePlayer();
        resetPlayerState();
        if (MovieMediaUtil.isYouTubeUrl(currentMovie.getPreviewVideoUrl())) {
            setupYouTubePlayer(currentMovie.getPreviewVideoUrl().trim());
            return;
        }

        showNativePlayerUi();
        configureSeekBar();
        configurePlayerControls();

        try {
            Uri sourceVideoUri = Uri.parse(currentMovie.getPreviewVideoUrl().trim());
            Uri videoUri = buildPlayableVideoUri(sourceVideoUri);
            long resolvedDurationMs = resolveDurationFromUri(sourceVideoUri);
            if (resolvedDurationMs > 0L) {
                updateDurationUi(resolvedDurationMs);
            }

            player = new ExoPlayer.Builder(this).build();
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
            player.setSeekParameters(SeekParameters.EXACT);
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (player == null) {
                        return;
                    }

                    if (playbackState == Player.STATE_READY) {
                        isPrepared = true;
                        maybeUpdateDurationFromPlayer();
                        if (pendingSeekPositionMs != C.TIME_UNSET) {
                            seekBar.setProgress((int) Math.min(pendingSeekPositionMs, Integer.MAX_VALUE));
                            currentTimeText.setText(formatTime(pendingSeekPositionMs));
                            pendingSeekPositionMs = C.TIME_UNSET;
                        }
                        syncProgressWithPlayer();
                    } else if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false;
                        updatePlayPauseButton();
                        stopProgressUpdates();
                        cancelHideControls();
                        syncProgressWithPlayer();
                        controlPanel.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlayingNow) {
                    isPlaying = isPlayingNow;
                    updatePlayPauseButton();
                    maybeUpdateDurationFromPlayer();

                    if (isPlayingNow) {
                        startProgressUpdates();
                        hideControlsDelayed(CONTROLS_HIDE_DELAY_MS);
                    } else {
                        stopProgressUpdates();
                        cancelHideControls();
                        if (controlPanel != null) {
                            controlPanel.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Video playback failed", error);
                    resetPlayerState();
                    Utils.showToast(MovieDetailActivity.this, buildPlaybackErrorMessage(error));
                }

                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition,
                                                    Player.PositionInfo newPosition,
                                                    int reason) {
                    pendingSeekPositionMs = C.TIME_UNSET;
                    syncProgressWithPlayer();
                }
            });

            videoView.setPlayer(player);
            player.setMediaItem(MediaItem.fromUri(videoUri));
            player.prepare();
            player.play();
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up player", e);
            resetPlayerState();
            Utils.showToast(this, "Failed to initialize the player: " + e.getMessage());
        }
    }

    private void configurePlayerControls() {
        playPauseButton.setOnClickListener(v -> {
            if (player == null) {
                return;
            }

            if (isPlaying) {
                player.pause();
                showCenterIndicator(false);
            } else {
                player.play();
                showCenterIndicator(true);
            }
        });

        rewindButton.setOnClickListener(v -> {
            if (!canSeek()) {
                return;
            }

            long newPosition = Math.max(0L, getCurrentPlaybackPositionMs() - SEEK_INTERVAL_MS);
            seekToPosition(newPosition);
            hideControlsDelayed(CONTROLS_HIDE_DELAY_MS);
            showCenterIndicator(false);
        });

        fastForwardButton.setOnClickListener(v -> {
            if (!canSeek()) {
                return;
            }

            long newPosition = Math.min(
                    getEffectiveDurationMs(),
                    getCurrentPlaybackPositionMs() + SEEK_INTERVAL_MS
            );
            seekToPosition(newPosition);
            hideControlsDelayed(CONTROLS_HIDE_DELAY_MS);
            showCenterIndicator(true);
        });

        videoView.setOnClickListener(v -> toggleControls());
    }

    private void setupYouTubePlayer(String youtubeUrl) {
        if (youtubeWebView == null) {
            Utils.showToast(this, "Failed to initialize the YouTube player");
            return;
        }

        String embedUrl = MovieMediaUtil.buildYouTubeEmbedUrl(youtubeUrl);
        if (embedUrl == null) {
            Utils.showToast(this, "Unable to recognize the YouTube video URL");
            return;
        }

        isEmbeddedWebPlayer = true;
        isPrepared = true;
        showYouTubePlayerUi();

        WebSettings webSettings = youtubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(false);
        normalizeEmbeddedPlayerUserAgent(webSettings);
        youtubeWebView.setVerticalScrollBarEnabled(false);
        youtubeWebView.setHorizontalScrollBarEnabled(false);
        youtubeWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        youtubeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || !request.isForMainFrame()) {
                    return false;
                }
                return shouldBlockYouTubeNavigation(request.getUrl() == null
                        ? null
                        : request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldBlockYouTubeNavigation(url);
            }
        });
        youtubeWebView.setWebChromeClient(new WebChromeClient());
        youtubeWebView.setBackgroundColor(android.graphics.Color.BLACK);
        String youtubeReferer = YOUTUBE_EMBED_ORIGIN + "/embedded/" + getPackageName();
        String embedUrlWithOrigin = Uri.parse(embedUrl)
                .buildUpon()
                .appendQueryParameter("origin", YOUTUBE_EMBED_ORIGIN)
                .appendQueryParameter("widget_referrer", youtubeReferer)
                .build()
                .toString();
        Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Referer", youtubeReferer);
        youtubeWebView.loadUrl(embedUrlWithOrigin, additionalHeaders);
    }

    private void showNativePlayerUi() {
        if (videoView != null) {
            videoView.setVisibility(View.VISIBLE);
        }
        if (youtubeWebView != null) {
            youtubeWebView.setVisibility(View.GONE);
        }
        if (gradientOverlay != null) {
            gradientOverlay.setVisibility(View.VISIBLE);
        }
        if (controlPanel != null) {
            controlPanel.setVisibility(View.VISIBLE);
        }
    }

    private void showYouTubePlayerUi() {
        if (videoView != null) {
            videoView.setVisibility(View.GONE);
        }
        if (youtubeWebView != null) {
            youtubeWebView.setVisibility(View.VISIBLE);
        }
        if (gradientOverlay != null) {
            gradientOverlay.setVisibility(View.GONE);
        }
        if (controlPanel != null) {
            controlPanel.setVisibility(View.GONE);
        }
        if (centerPlayIndicator != null) {
            centerPlayIndicator.setVisibility(View.GONE);
        }
    }

    private boolean shouldBlockYouTubeNavigation(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        if ("about:blank".equals(url)) {
            return false;
        }
        return !MovieMediaUtil.isYouTubeEmbedUrl(url);
    }

    private void normalizeEmbeddedPlayerUserAgent(WebSettings webSettings) {
        String currentUserAgent = webSettings.getUserAgentString();
        if (currentUserAgent == null || currentUserAgent.trim().isEmpty()) {
            return;
        }

        String normalizedUserAgent = currentUserAgent
                .replace("; wv", "")
                .replace(" wv", "");
        if (!normalizedUserAgent.equals(currentUserAgent)) {
            webSettings.setUserAgentString(normalizedUserAgent);
        }
    }

    private void configureSeekBar() {
        seekBar.setKeyProgressIncrement((int) SEEK_INTERVAL_MS);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTimeText.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
                stopProgressUpdates();
                cancelHideControls();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;

                if (!canSeek()) {
                    syncProgressWithPlayer();
                    return;
                }

                long newPosition = Math.max(0L, Math.min(getEffectiveDurationMs(), seekBar.getProgress()));
                seekToPosition(newPosition);
                if (isPlaying) {
                    startProgressUpdates();
                    hideControlsDelayed(CONTROLS_HIDE_DELAY_MS);
                }
            }
        });
    }

    /**
     * 加载当前用户信息
     */
    private void loadUserInfo() {
        repository.loadCurrentUser(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                currentUser = data;
            }

            @Override
            public void onError(String errorMessage) {
                currentUser = null;
            }
        });
    }

    /**
     * 添加到购物车
     */
    private void addToCart() {
        if (currentMovie == null) {
            return;
        }

        CartManager cartManager = CartManager.getInstance();
        if (cartManager.addToCart(currentMovie)) {
            Utils.showToast(this, "Added to cart: " + currentMovie.getTitle());
        } else {
            Utils.showToast(this, "This movie is already in your cart");
        }
    }

    /**
     * 立即购买
     */
    private void buyNow() {
        if (currentMovie == null) {
            Utils.showToast(this, "Movie information is invalid");
            return;
        }

        if (currentUser == null) {
            Utils.showToast(this, "Please sign in first");
            return;
        }

        if (currentUser.getCredits() < currentMovie.getPrice()) {
            Utils.showToast(this, "You do not have enough credits to purchase this movie");
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Purchase")
                .setMessage("Spend " + currentMovie.getPrice() + " credits to purchase \"" + currentMovie.getTitle() + "\"?")
                .setPositiveButton("Confirm", (dialog, which) -> processPurchase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 处理购买逻辑
     */
    private void processPurchase() {
        if (currentMovie == null) {
            return;
        }

        repository.buyMovie(this, currentMovie, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                currentUser = data;
                createOrderRecord(data.getCredits());
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(MovieDetailActivity.this, errorMessage);
            }
        });
    }

    /**
     * 创建订单记录
     */
    private void createOrderRecord(int remainingCredits) {
        Utils.showToast(this, "Purchase successful. Remaining credits: " + remainingCredits);
        finish();
    }

    /**
     * 格式化时间显示
     */
    private String formatTime(long milliseconds) {
        int totalSeconds = (int) Math.max(0L, milliseconds / 1000L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 开始进度更新
     */
    private void startProgressUpdates() {
        stopProgressUpdates();
        progressRunnable = () -> {
            if (player != null && !isUserSeeking && (isPrepared || isPlaying)) {
                syncProgressWithPlayer();
                progressHandler.postDelayed(progressRunnable, 300);
            }
        };
        progressHandler.post(progressRunnable);
    }

    /**
     * 停止进度更新
     */
    private void stopProgressUpdates() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    /**
     * 更新播放/暂停按钮图标
     */
    private void updatePlayPauseButton() {
        if (playPauseButton == null) {
            return;
        }

        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    /**
     * 显示中央播放指示器
     */
    private void showCenterIndicator(boolean isPlayingAfterAction) {
        if (centerPlayIndicator == null) {
            return;
        }

        centerPlayIndicator.setImageResource(isPlayingAfterAction
                ? android.R.drawable.ic_media_play
                : android.R.drawable.ic_media_pause);
        centerPlayIndicator.setVisibility(View.VISIBLE);
        centerPlayIndicator.postDelayed(() -> {
            if (centerPlayIndicator != null) {
                centerPlayIndicator.setVisibility(View.GONE);
            }
        }, 500);
    }

    /**
     * 延迟隐藏控制面板
     */
    private void hideControlsDelayed(long delayMs) {
        cancelHideControls();
        hideRunnable = () -> {
            if (controlPanel != null && isPlaying) {
                controlPanel.setVisibility(View.GONE);
            }
        };
        hideHandler.postDelayed(hideRunnable, delayMs);
    }

    private void cancelHideControls() {
        if (hideRunnable != null) {
            hideHandler.removeCallbacks(hideRunnable);
        }
    }

    /**
     * 切换控制面板显示
     */
    private void toggleControls() {
        if (controlPanel == null) {
            return;
        }

        if (controlPanel.getVisibility() == View.VISIBLE) {
            controlPanel.setVisibility(View.GONE);
            cancelHideControls();
        } else {
            controlPanel.setVisibility(View.VISIBLE);
            if (isPlaying) {
                hideControlsDelayed(CONTROLS_HIDE_DELAY_MS);
            }
        }
    }

    private void syncProgressWithPlayer() {
        if (player == null || seekBar == null || currentTimeText == null) {
            return;
        }

        maybeUpdateDurationFromPlayer();
        long currentPosition = Math.max(0L, player.getCurrentPosition());
        currentTimeText.setText(formatTime(currentPosition));

        long effectiveDurationMs = getEffectiveDurationMs();
        if (effectiveDurationMs > 0L) {
            int boundedPosition = (int) Math.min(currentPosition, effectiveDurationMs);
            seekBar.setProgress(boundedPosition);
        } else {
            seekBar.setProgress(0);
        }
    }

    private void seekToPosition(long targetPositionMs) {
        if (player == null) {
            return;
        }

        long effectiveDurationMs = getEffectiveDurationMs();
        long boundedPositionMs = effectiveDurationMs > 0L
                ? Math.max(0L, Math.min(effectiveDurationMs, targetPositionMs))
                : Math.max(0L, targetPositionMs);
        pendingSeekPositionMs = boundedPositionMs;
        seekBar.setProgress((int) Math.min(boundedPositionMs, Integer.MAX_VALUE));
        currentTimeText.setText(formatTime(boundedPositionMs));
        player.seekTo(boundedPositionMs);
    }

    private void updateDurationUi(long durationMs) {
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) {
            videoDurationMs = 0L;
            seekBar.setMax(0);
            currentTimeText.setText("00:00");
            totalTimeText.setText("00:00");
            setSeekControlsEnabled(false);
            return;
        }

        videoDurationMs = durationMs;
        seekBar.setMax((int) Math.min(Integer.MAX_VALUE, durationMs));
        currentTimeText.setText(formatTime(Math.max(0L, player != null ? player.getCurrentPosition() : 0L)));
        totalTimeText.setText(formatTime(durationMs));
        setSeekControlsEnabled(true);
    }

    private void maybeUpdateDurationFromPlayer() {
        if (player == null) {
            return;
        }

        long durationMs = player.getDuration();
        if (durationMs != C.TIME_UNSET && durationMs > 0L) {
            if (videoDurationMs != durationMs) {
                updateDurationUi(durationMs);
            }
            return;
        }

        if (videoDurationMs <= 0L && currentMovie != null && currentMovie.getPreviewVideoUrl() != null) {
            long resolvedDurationMs = resolveDurationFromUri(Uri.parse(currentMovie.getPreviewVideoUrl().trim()));
            if (resolvedDurationMs > 0L) {
                updateDurationUi(resolvedDurationMs);
            }
        }
    }

    private void setSeekControlsEnabled(boolean enabled) {
        if (seekBar != null) {
            seekBar.setEnabled(enabled);
        }
        if (rewindButton != null) {
            rewindButton.setEnabled(enabled);
            rewindButton.setAlpha(enabled ? 1f : 0.5f);
        }
        if (fastForwardButton != null) {
            fastForwardButton.setEnabled(enabled);
            fastForwardButton.setAlpha(enabled ? 1f : 0.5f);
        }
    }

    private boolean canSeek() {
        return player != null
                && getEffectiveDurationMs() > 0L
                && (isPrepared
                || player.getPlaybackState() == Player.STATE_READY
                || player.isCurrentMediaItemSeekable());
    }

    private long getEffectiveDurationMs() {
        if (videoDurationMs > 0L) {
            return videoDurationMs;
        }
        if (player == null) {
            return 0L;
        }
        long playerDurationMs = player.getDuration();
        return playerDurationMs == C.TIME_UNSET || playerDurationMs <= 0L ? 0L : playerDurationMs;
    }

    private long getCurrentPlaybackPositionMs() {
        long playerPositionMs = player == null ? 0L : Math.max(0L, player.getCurrentPosition());
        long seekBarPositionMs = seekBar == null ? 0L : Math.max(0L, seekBar.getProgress());

        if (pendingSeekPositionMs != C.TIME_UNSET) {
            return pendingSeekPositionMs;
        }

        return Math.max(playerPositionMs, seekBarPositionMs);
    }

    private void resetPlayerState() {
        isPlaying = false;
        isPrepared = false;
        isUserSeeking = false;
        isEmbeddedWebPlayer = false;
        videoDurationMs = 0L;
        pendingSeekPositionMs = C.TIME_UNSET;
        stopProgressUpdates();
        cancelHideControls();
        updatePlayPauseButton();
        setSeekControlsEnabled(false);

        if (seekBar != null) {
            seekBar.setMax(0);
            seekBar.setProgress(0);
        }
        if (currentTimeText != null) {
            currentTimeText.setText("00:00");
        }
        if (totalTimeText != null) {
            totalTimeText.setText("00:00");
        }
        if (controlPanel != null) {
            controlPanel.setVisibility(View.VISIBLE);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (videoView != null) {
            videoView.setPlayer(null);
            videoView.setVisibility(View.VISIBLE);
        }
        if (youtubeWebView != null) {
            youtubeWebView.stopLoading();
            youtubeWebView.loadUrl("about:blank");
            youtubeWebView.setVisibility(View.GONE);
        }
        if (gradientOverlay != null) {
            gradientOverlay.setVisibility(View.VISIBLE);
        }
    }

    private Uri buildPlayableVideoUri(Uri sourceVideoUri) throws IOException {
        if (sourceVideoUri == null || sourceVideoUri.getScheme() == null) {
            return sourceVideoUri;
        }

        if ("rawresource".equals(sourceVideoUri.getScheme())) {
            String resourceIdText = sourceVideoUri.getLastPathSegment();
            if (resourceIdText == null) {
                return sourceVideoUri;
            }

            int rawResId = Integer.parseInt(resourceIdText);
            File cachedVideoFile = copyRawVideoToCache(rawResId);
            return Uri.fromFile(cachedVideoFile);
        }

        return sourceVideoUri;
    }

    private File copyRawVideoToCache(int rawResId) throws IOException {
        File videoCacheDir = new File(getCacheDir(), "video_cache");
        if (!videoCacheDir.exists() && !videoCacheDir.mkdirs()) {
            throw new IOException("Unable to create the video cache directory");
        }

        String resourceName = getResources().getResourceEntryName(rawResId);
        File cachedVideoFile = new File(videoCacheDir, resourceName + ".mp4");
        if (cachedVideoFile.exists() && !cachedVideoFile.delete()) {
            throw new IOException("Unable to replace the old cached video file");
        }

        try (InputStream inputStream = getResources().openRawResource(rawResId);
             FileOutputStream outputStream = new FileOutputStream(cachedVideoFile, false)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        return cachedVideoFile;
    }

    private long resolveDurationFromUri(Uri videoUri) {
        if (videoUri == null || videoUri.getScheme() == null) {
            return 0L;
        }

        if ("rawresource".equals(videoUri.getScheme())) {
            String resourceIdText = videoUri.getLastPathSegment();
            if (resourceIdText == null) {
                return 0L;
            }

            try {
                int rawResId = Integer.parseInt(resourceIdText);
                return resolveDurationFromRawResource(rawResId);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid raw resource id in uri: " + videoUri, e);
            }
        }

        return 0L;
    }

    private long resolveDurationFromRawResource(int rawResId) {
        AssetFileDescriptor descriptor = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            descriptor = getResources().openRawResourceFd(rawResId);
            if (descriptor == null) {
                return 0L;
            }

            retriever.setDataSource(
                    descriptor.getFileDescriptor(),
                    descriptor.getStartOffset(),
                    descriptor.getLength()
            );

            String durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationMs == null ? 0L : Long.parseLong(durationMs);
        } catch (Exception e) {
            Log.w(TAG, "Failed to resolve raw duration for resource: " + rawResId, e);
            return 0L;
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.w(TAG, "Failed to release metadata retriever", e);
            }
            if (descriptor != null) {
                try {
                    descriptor.close();
                } catch (IOException e) {
                    Log.w(TAG, "Failed to close raw resource descriptor", e);
                }
            }
        }
    }

    private String buildPlaybackErrorMessage(PlaybackException error) {
        int errorCode = error.errorCode;
        if (errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
            return "The video file was not found. Please verify the resource in the raw directory.";
        }
        if (errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                || errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED) {
            return "Video decoding failed. Please make sure the video format is supported by the device.";
        }
        if (errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED
                || errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
            return "Failed to load the video. Please try again later.";
        }
        return "Video playback failed: " + error.getErrorCodeName();
    }
}
