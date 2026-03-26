package com.innovationai.myapplication.activity;

import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.SystemClock;
import android.widget.ImageButton;

import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.innovationai.myapplication.util.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(AndroidJUnit4.class)
public class MovieDetailActivitySeekTest {
    private static final long READY_TIMEOUT_MS = 20_000L;
    private static final long POSITION_TIMEOUT_MS = 8_000L;

    @Test
    public void seekControlsShouldContinueFromTargetPosition() throws Exception {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MovieDetailActivity.class);
        intent.putExtra(Constants.EXTRA_MOVIE_ID, "1");

        try (ActivityScenario<MovieDetailActivity> scenario = ActivityScenario.launch(intent)) {
            waitUntilReady(scenario);

            scenario.onActivity(activity -> getPlayer(activity).pause());

            long directSeekTargetMs = 15_000L;
            scenario.onActivity(activity -> invokeSeekToPosition(activity, directSeekTargetMs));
            waitForPositionNear(scenario, directSeekTargetMs, 2_000L, POSITION_TIMEOUT_MS);

            scenario.onActivity(activity -> getPlayer(activity).play());
            SystemClock.sleep(1_500L);

            long afterDirectSeekPlaybackMs = readPlayerPosition(scenario);
            assertTrue(
                    "Expected playback to continue near the seek target instead of restarting from 0, but was "
                            + afterDirectSeekPlaybackMs,
                    afterDirectSeekPlaybackMs > 12_000L
            );

            scenario.onActivity(activity -> getPlayer(activity).pause());

            long beforeFastForwardMs = readPlayerPosition(scenario);
            scenario.onActivity(activity -> getFastForwardButton(activity).performClick());
            long afterFastForwardMs = waitForPositionGreaterThan(
                    scenario,
                    beforeFastForwardMs + 6_000L,
                    POSITION_TIMEOUT_MS
            );
            assertTrue(
                    "Fast forward should move the player forward instead of restarting from 0, but was "
                            + afterFastForwardMs,
                    afterFastForwardMs > beforeFastForwardMs + 5_000L
            );

            scenario.onActivity(activity -> getPlayer(activity).pause());

            long beforeRewindMs = readPlayerPosition(scenario);
            scenario.onActivity(activity -> getRewindButton(activity).performClick());
            long afterRewindMs = waitForPositionLessThan(
                    scenario,
                    beforeRewindMs - 4_000L,
                    POSITION_TIMEOUT_MS
            );
            assertTrue(
                    "Rewind should move the player backward but stay away from 0 unless seeking to the start. Was "
                            + afterRewindMs,
                    afterRewindMs > 1_000L
            );
        }
    }

    private static void waitUntilReady(ActivityScenario<MovieDetailActivity> scenario) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + READY_TIMEOUT_MS;
        long lastPlayerDurationMs = -1L;
        long lastResolvedDurationMs = -1L;
        boolean lastSeekable = false;
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicLong durationMs = new AtomicLong(0L);
            AtomicLong resolvedDurationMs = new AtomicLong(0L);
            AtomicLong seekable = new AtomicLong(0L);
            scenario.onActivity(activity -> {
                ExoPlayer player = getPlayer(activity);
                durationMs.set(player == null ? 0L : player.getDuration());
                resolvedDurationMs.set(readLongField(activity, "videoDurationMs"));
                seekable.set(player != null && player.isCurrentMediaItemSeekable() ? 1L : 0L);
            });

            lastPlayerDurationMs = durationMs.get();
            lastResolvedDurationMs = resolvedDurationMs.get();
            lastSeekable = seekable.get() == 1L;
            if (durationMs.get() > 0L && resolvedDurationMs.get() > 0L && seekable.get() == 1L) {
                return;
            }

            SystemClock.sleep(250L);
        }

        throw new AssertionError(
                "Timed out waiting for MovieDetailActivity player to become ready and seekable. "
                        + "playerDuration=" + lastPlayerDurationMs
                        + ", activityDuration=" + lastResolvedDurationMs
                        + ", seekable=" + lastSeekable
        );
    }

    private static void waitForPositionNear(
            ActivityScenario<MovieDetailActivity> scenario,
            long targetMs,
            long toleranceMs,
            long timeoutMs
    ) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        long lastPositionMs = -1L;
        long lastResolvedDurationMs = -1L;
        long lastPlayerDurationMs = -1L;
        boolean lastSeekable = false;
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicLong currentPositionMs = new AtomicLong(0L);
            AtomicLong resolvedDurationMs = new AtomicLong(0L);
            AtomicLong playerDurationMs = new AtomicLong(0L);
            AtomicLong seekable = new AtomicLong(0L);
            scenario.onActivity(activity -> {
                ExoPlayer player = getPlayer(activity);
                currentPositionMs.set(player == null ? 0L : player.getCurrentPosition());
                playerDurationMs.set(player == null ? 0L : player.getDuration());
                resolvedDurationMs.set(readLongField(activity, "videoDurationMs"));
                seekable.set(player != null && player.isCurrentMediaItemSeekable() ? 1L : 0L);
            });

            lastPositionMs = currentPositionMs.get();
            lastResolvedDurationMs = resolvedDurationMs.get();
            lastPlayerDurationMs = playerDurationMs.get();
            lastSeekable = seekable.get() == 1L;
            if (Math.abs(lastPositionMs - targetMs) <= toleranceMs) {
                return;
            }
            SystemClock.sleep(200L);
        }

        throw new AssertionError(
                "Timed out waiting for player position near " + targetMs
                        + ", lastPosition=" + lastPositionMs
                        + ", activityDuration=" + lastResolvedDurationMs
                        + ", playerDuration=" + lastPlayerDurationMs
                        + ", seekable=" + lastSeekable
        );
    }

    private static long waitForPositionGreaterThan(
            ActivityScenario<MovieDetailActivity> scenario,
            long expectedMinimumMs,
            long timeoutMs
    ) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline) {
            long currentPositionMs = readPlayerPosition(scenario);
            if (currentPositionMs >= expectedMinimumMs) {
                return currentPositionMs;
            }
            SystemClock.sleep(200L);
        }

        throw new AssertionError("Timed out waiting for player position >= " + expectedMinimumMs);
    }

    private static long waitForPositionLessThan(
            ActivityScenario<MovieDetailActivity> scenario,
            long expectedMaximumMs,
            long timeoutMs
    ) throws Exception {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline) {
            long currentPositionMs = readPlayerPosition(scenario);
            if (currentPositionMs <= expectedMaximumMs) {
                return currentPositionMs;
            }
            SystemClock.sleep(200L);
        }

        throw new AssertionError("Timed out waiting for player position <= " + expectedMaximumMs);
    }

    private static long readPlayerPosition(ActivityScenario<MovieDetailActivity> scenario) {
        AtomicLong currentPositionMs = new AtomicLong(0L);
        scenario.onActivity(activity -> {
            ExoPlayer player = getPlayer(activity);
            currentPositionMs.set(player == null ? 0L : player.getCurrentPosition());
        });
        return currentPositionMs.get();
    }

    private static void invokeSeekToPosition(MovieDetailActivity activity, long targetMs) {
        try {
            Method method = MovieDetailActivity.class.getDeclaredMethod("seekToPosition", long.class);
            method.setAccessible(true);
            method.invoke(activity, targetMs);
        } catch (Exception e) {
            throw new AssertionError("Unable to invoke seekToPosition via reflection", e);
        }
    }

    private static ExoPlayer getPlayer(MovieDetailActivity activity) {
        return (ExoPlayer) getField(activity, "player");
    }

    private static ImageButton getFastForwardButton(MovieDetailActivity activity) {
        return (ImageButton) getField(activity, "fastForwardButton");
    }

    private static ImageButton getRewindButton(MovieDetailActivity activity) {
        return (ImageButton) getField(activity, "rewindButton");
    }

    private static Object getField(MovieDetailActivity activity, String fieldName) {
        try {
            Field field = MovieDetailActivity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(activity);
        } catch (Exception e) {
            throw new AssertionError("Unable to access field: " + fieldName, e);
        }
    }

    private static long readLongField(MovieDetailActivity activity, String fieldName) {
        Object value = getField(activity, fieldName);
        if (value instanceof Long) {
            return (Long) value;
        }
        throw new AssertionError("Field is not a long: " + fieldName);
    }
}
