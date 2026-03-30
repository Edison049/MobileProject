package com.innovationai.myapplication.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.Assume;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AppRepositoryFlowTest {
    private static final long TIMEOUT_SECONDS = 10L;

    private final AppRepository repository = AppRepository.getInstance();
    private Context context;

    @Before
    public void setUp() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        LocalDataStore.getInstance().resetForTesting(context);
        SessionManager.clearSession(context);
        awaitAction(callback -> repository.initialize(context, callback));
        Assume.assumeFalse(repository.isUsingFirebase(context));
    }

    @Test
    public void adminShouldManageMoviesAndUsers() throws InterruptedException {
        User admin = awaitData(callback ->
                repository.login(context, Constants.ADMIN_USER_EMAIL, Constants.ADMIN_USER_PASSWORD, callback));
        assertNotNull(admin);
        assertTrue(admin.isAdmin());

        User createdUser = awaitData(callback ->
                repository.createUser(
                        context,
                        "tester",
                        21,
                        "tester@example.com",
                        "tester123",
                        600,
                        Constants.ROLE_USER,
                        false,
                        callback
                ));
        assertNotNull(createdUser);

        List<User> usersAfterCreate = awaitData(callback -> repository.loadUsers(context, callback));
        assertTrue(containsUser(usersAfterCreate, "tester@example.com"));

        repository.logout(context);
        User testerLogin = awaitData(callback ->
                repository.login(context, "tester@example.com", "tester123", callback));
        assertNotNull(testerLogin);

        User reloginAdmin = awaitData(callback ->
                repository.login(context, Constants.ADMIN_USER_EMAIL, Constants.ADMIN_USER_PASSWORD, callback));
        assertNotNull(reloginAdmin);

        awaitAction(callback -> repository.deleteUser(context, createdUser.getUid(), callback));
        repository.logout(context);
        String loginError = this.<User>awaitError(callback ->
                repository.login(context, "tester@example.com", "tester123", callback));
        assertTrue(loginError.contains("不存在") || loginError.contains("停用"));

        User adminAfterDelete = awaitData(callback ->
                repository.login(context, Constants.ADMIN_USER_EMAIL, Constants.ADMIN_USER_PASSWORD, callback));
        assertNotNull(adminAfterDelete);

        List<Movie> initialMovies = awaitData(callback -> repository.loadMovies(context, callback));
        int initialCount = initialMovies.size();

        Movie movie = new Movie();
        movie.setId("movie_test_admin");
        movie.setTitle("管理员测试电影");
        movie.setDescription("用于验证管理员新增和删除电影流程");
        movie.setGenre("动作");
        movie.setDirector("Codex");
        movie.setCast("Tester");
        movie.setPrice(88);
        movie.setRating(8.8f);
        movie.setPosterUrl(MovieMediaUtil.drawableRef("ic_launcher_foreground"));
        movie.setPreviewVideoUrl(MovieMediaUtil.rawRef("seabird1"));

        awaitAction(callback -> repository.addMovie(context, movie, callback));
        List<Movie> moviesAfterAdd = awaitData(callback -> repository.loadMovies(context, callback));
        assertEquals(initialCount + 1, moviesAfterAdd.size());
        assertTrue(containsMovie(moviesAfterAdd, "管理员测试电影"));

        awaitAction(callback -> repository.deleteMovie(context, movie.getId(), callback));
        List<Movie> moviesAfterDelete = awaitData(callback -> repository.loadMovies(context, callback));
        assertEquals(initialCount, moviesAfterDelete.size());
        assertFalse(containsMovie(moviesAfterDelete, "管理员测试电影"));
    }

    @Test
    public void topUpAndPurchaseShouldPersistCreditsAndOrders() throws InterruptedException {
        User mary = awaitData(callback ->
                repository.login(context, Constants.TEST_USER_MARY_EMAIL, Constants.TEST_USER_MARY_PASSWORD, callback));
        assertNotNull(mary);
        assertEquals(Constants.DEFAULT_USER_CREDITS, mary.getCredits());

        User afterTopUp = awaitData(callback -> repository.topUpCredits(context, 200, callback));
        assertEquals(1200, afterTopUp.getCredits());

        List<Movie> movies = awaitData(callback -> repository.loadMovies(context, callback));
        assertFalse(movies.isEmpty());

        User afterPurchase = awaitData(callback -> repository.buyMovie(context, movies.get(0), callback));
        assertEquals(1200 - movies.get(0).getPrice(), afterPurchase.getCredits());

        List<Order> orders = awaitData(callback -> repository.loadOrders(context, callback));
        assertEquals(1, orders.size());
        assertEquals(movies.get(0).getPrice(), orders.get(0).getTotalAmount());
        assertEquals(1, orders.get(0).getMovies().size());
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

    private <T> String awaitError(DataRequest<T> request) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        request.execute(new AppRepository.DataCallback<T>() {
            @Override
            public void onSuccess(T data) {
                latch.countDown();
            }

            @Override
            public void onError(String errorMessage) {
                error.set(errorMessage);
                latch.countDown();
            }
        });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        return error.get() == null ? "" : error.get();
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

    private boolean containsUser(List<User> users, String email) {
        for (User user : users) {
            if (email.equals(user.getEmail())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsMovie(List<Movie> movies, String title) {
        for (Movie movie : movies) {
            if (title.equals(movie.getTitle())) {
                return true;
            }
        }
        return false;
    }
}
