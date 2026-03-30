package com.innovationai.myapplication.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.SessionManager;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class FirebaseBootstrapSmokeTest {
    private static final long TIMEOUT_SECONDS = 15L;

    private final AppRepository repository = AppRepository.getInstance();
    private Context context;

    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        SessionManager.clearSession(context);
        awaitAction(callback -> repository.initialize(context, callback));
        Assume.assumeTrue(repository.isUsingFirebase(context));
    }

    @Test
    public void shouldBootstrapDefaultAccountsAndMoviesInFirebase() throws InterruptedException {
        User mary = awaitData(callback ->
                repository.login(context, Constants.TEST_USER_MARY_EMAIL, Constants.TEST_USER_MARY_PASSWORD, callback));
        assertNotNull(mary);
        assertEqualsIgnoreCase(Constants.TEST_USER_MARY_EMAIL, mary.getEmail());

        List<Movie> movies = awaitData(callback -> repository.loadMovies(context, callback));
        assertNotNull(movies);
        assertFalse(movies.isEmpty());
        assertTrue(containsMovie(movies, "movie_avengers4"));
    }

    private interface DataRequest<T> {
        void execute(AppRepository.DataCallback<T> callback);
    }

    private interface ActionRequest {
        void execute(AppRepository.ActionCallback callback);
    }

    private <T> T awaitData(DataRequest<T> request) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        request.execute(new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(T data) {
                result.set(data);
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
        return result.get();
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

    private boolean containsMovie(List<Movie> movies, String movieId) {
        for (Movie movie : movies) {
            if (movie != null && movieId.equals(movie.getId())) {
                return true;
            }
        }
        return false;
    }

    private void assertEqualsIgnoreCase(String expected, String actual) {
        assertNotNull(actual);
        assertTrue(expected.equalsIgnoreCase(actual));
    }
}
