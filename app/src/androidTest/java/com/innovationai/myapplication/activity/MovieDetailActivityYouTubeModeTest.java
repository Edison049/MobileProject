package com.innovationai.myapplication.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.media3.ui.PlayerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.innovationai.myapplication.R;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.data.LocalDataStore;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MovieDetailActivityYouTubeModeTest {
    private static final long TIMEOUT_SECONDS = 10L;

    private final AppRepository repository = AppRepository.getInstance();
    private Context context;

    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        repository.setForceLocalModeForTesting(true);
        LocalDataStore.getInstance().resetForTesting(context);
        SessionManager.clearSession(context);
        awaitAction(callback -> repository.initialize(context, callback));
    }

    @After
    public void tearDown() {
        repository.setForceLocalModeForTesting(false);
    }

    @Test
    public void shouldShowDescriptionAndEnableYouTubePlayerMode() throws InterruptedException {
        Movie movie = new Movie();
        movie.setId("movie_youtube_mode_test");
        movie.setTitle("YouTube Mode Test");
        movie.setDescription("The synopsis entered by the admin should appear on the detail page.");
        movie.setGenre("Documentary");
        movie.setDirector("Codex");
        movie.setCast("Tester");
        movie.setPrice(88);
        movie.setRating(8.6f);
        movie.setPosterUrl(MovieMediaUtil.drawableRef("ic_launcher_foreground"));
        movie.setPreviewVideoUrl("https://youtu.be/FXJZ3n6x7Nw?si=PR5NpsNeZsTezXhK");
        awaitAction(callback -> repository.addMovie(context, movie, callback));

        Intent intent = new Intent(context, MovieDetailActivity.class);
        intent.putExtra(Constants.EXTRA_MOVIE_ID, movie.getId());

        try (ActivityScenario<MovieDetailActivity> scenario = ActivityScenario.launch(intent)) {
            AtomicReference<String> descriptionText = new AtomicReference<>();
            AtomicReference<Integer> youtubeVisibility = new AtomicReference<>(View.GONE);
            AtomicReference<Integer> playerVisibility = new AtomicReference<>(View.VISIBLE);
            AtomicReference<String> youtubeUrl = new AtomicReference<>();

            waitUntilMovieLoaded(scenario, descriptionText, youtubeVisibility, playerVisibility, youtubeUrl);

            assertEquals("The synopsis entered by the admin should appear on the detail page.", descriptionText.get());
            assertEquals(View.VISIBLE, (int) youtubeVisibility.get());
            assertEquals(View.GONE, (int) playerVisibility.get());
            assertTrue(MovieMediaUtil.isYouTubeUrl(movie.getPreviewVideoUrl()));
            assertNotNull(youtubeUrl.get());
            assertTrue(youtubeUrl.get().contains("youtube-nocookie.com/embed/FXJZ3n6x7Nw"));
            assertTrue(!youtubeUrl.get().contains("watch?v="));
            assertTrue(youtubeUrl.get().contains("origin="));
            assertTrue(youtubeUrl.get().contains("widget_referrer="));
        }
    }

    private interface ActionRequest {
        void execute(AppRepository.ActionCallback callback);
    }

    private void waitUntilMovieLoaded(
            ActivityScenario<MovieDetailActivity> scenario,
            AtomicReference<String> descriptionText,
            AtomicReference<Integer> youtubeVisibility,
            AtomicReference<Integer> playerVisibility,
            AtomicReference<String> youtubeUrl
    ) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + (TIMEOUT_SECONDS * 1000L);
        while (SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity(activity -> {
                TextView descriptionView = activity.findViewById(R.id.movie_description_detail);
                WebView youtubeWebView = activity.findViewById(R.id.youtube_web_view);
                PlayerView playerView = activity.findViewById(R.id.video_view);

                assertNotNull(descriptionView);
                assertNotNull(youtubeWebView);
                assertNotNull(playerView);

                descriptionText.set(descriptionView.getText().toString());
                youtubeVisibility.set(youtubeWebView.getVisibility());
                playerVisibility.set(playerView.getVisibility());
                youtubeUrl.set(youtubeWebView.getUrl());
            });

            if ("The synopsis entered by the admin should appear on the detail page.".contentEquals(descriptionText.get())
                    && youtubeVisibility.get() == View.VISIBLE
                    && youtubeUrl.get() != null
                    && youtubeUrl.get().contains("/embed/FXJZ3n6x7Nw")) {
                return;
            }
            SystemClock.sleep(200L);
        }

        throw new AssertionError("Timed out waiting for MovieDetailActivity to enter YouTube mode");
    }

    private void awaitAction(ActionRequest request) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        request.execute(new AppRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                error.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }
}
