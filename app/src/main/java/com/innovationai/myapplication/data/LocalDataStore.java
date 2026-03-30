package com.innovationai.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.SecurityUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地持久化数据源
 * 在没有Firebase配置时仍然保证电影、用户、订单、积分都能完整工作
 */
public final class LocalDataStore {
    private static final String PREF_NAME = "movie_app_local_store";
    private static final String KEY_USERS_JSON = "users_json";
    private static final String KEY_MOVIES_JSON = "movies_json";
    private static final String KEY_ORDERS_JSON = "orders_json";
    private static final String KEY_INITIALIZED = "initialized";

    private static final LocalDataStore INSTANCE = new LocalDataStore();

    private LocalDataStore() {
    }

    public static LocalDataStore getInstance() {
        return INSTANCE;
    }

    public synchronized void ensureInitialized(Context context) {
        SharedPreferences preferences = getPrefs(context);
        if (preferences.getBoolean(KEY_INITIALIZED, false)) {
            return;
        }

        preferences.edit()
                .putString(KEY_USERS_JSON, buildDefaultUsers().toString())
                .putString(KEY_MOVIES_JSON, buildDefaultMovies().toString())
                .putString(KEY_ORDERS_JSON, new JSONArray().toString())
                .putBoolean(KEY_INITIALIZED, true)
                .apply();
    }

    public synchronized void resetForTesting(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public synchronized List<User> getActiveUsers(Context context) {
        ensureInitialized(context);
        List<User> users = new ArrayList<>();
        JSONArray array = readArray(context, KEY_USERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null || !userJson.optBoolean("active", true)) {
                continue;
            }
            users.add(jsonToUser(userJson));
        }
        return users;
    }

    public synchronized List<User> getAllUsers(Context context) {
        ensureInitialized(context);
        List<User> users = new ArrayList<>();
        JSONArray array = readArray(context, KEY_USERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null) {
                continue;
            }
            users.add(jsonToUser(userJson));
        }
        return users;
    }

    public synchronized User getUserById(Context context, String userId) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_USERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null) {
                continue;
            }
            if (userId.equals(userJson.optString("uid"))) {
                return jsonToUser(userJson);
            }
        }
        return null;
    }

    public synchronized User findActiveUserByLogin(Context context, String identifier) {
        ensureInitialized(context);
        String lowerIdentifier = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        JSONArray array = readArray(context, KEY_USERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null || !userJson.optBoolean("active", true)) {
                continue;
            }

            String name = userJson.optString("name", "").toLowerCase(Locale.ROOT);
            String email = userJson.optString("email", "").toLowerCase(Locale.ROOT);
            if (lowerIdentifier.equals(name) || lowerIdentifier.equals(email)) {
                return jsonToUser(userJson);
            }
        }
        return null;
    }

    public synchronized boolean userNameOrEmailExists(Context context, String name, String email, String excludeUserId) {
        ensureInitialized(context);
        String lowerName = safeLower(name);
        String lowerEmail = safeLower(email);
        JSONArray array = readArray(context, KEY_USERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null || !userJson.optBoolean("active", true)) {
                continue;
            }

            String currentUserId = userJson.optString("uid");
            if (excludeUserId != null && excludeUserId.equals(currentUserId)) {
                continue;
            }

            String currentName = safeLower(userJson.optString("name"));
            String currentEmail = safeLower(userJson.optString("email"));
            if (!lowerName.isEmpty() && lowerName.equals(currentName)) {
                return true;
            }
            if (!lowerEmail.isEmpty() && lowerEmail.equals(currentEmail)) {
                return true;
            }
        }
        return false;
    }

    public synchronized User saveUser(Context context, User user) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_USERS_JSON);
        JSONObject serializedUser = userToJson(user);
        boolean updated = false;

        for (int i = 0; i < array.length(); i++) {
            JSONObject existing = array.optJSONObject(i);
            if (existing == null) {
                continue;
            }
            if (user.getUid().equals(existing.optString("uid"))) {
                try {
                    array.put(i, serializedUser);
                } catch (JSONException ignored) {
                    array.put(serializedUser);
                }
                updated = true;
                break;
            }
        }

        if (!updated) {
            array.put(serializedUser);
        }

        writeArray(context, KEY_USERS_JSON, array);
        return user;
    }

    public synchronized boolean deactivateUser(Context context, String userId) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_USERS_JSON);
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            JSONObject userJson = array.optJSONObject(i);
            if (userJson == null) {
                continue;
            }
            if (userId.equals(userJson.optString("uid"))) {
                try {
                    userJson.put("active", false);
                    array.put(i, userJson);
                    changed = true;
                } catch (JSONException ignored) {
                    return false;
                }
                break;
            }
        }

        if (changed) {
            writeArray(context, KEY_USERS_JSON, array);
        }
        return changed;
    }

    public synchronized List<Movie> getActiveMovies(Context context) {
        ensureInitialized(context);
        List<Movie> movies = new ArrayList<>();
        JSONArray array = readArray(context, KEY_MOVIES_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject movieJson = array.optJSONObject(i);
            if (movieJson == null || !movieJson.optBoolean("active", true)) {
                continue;
            }
            movies.add(jsonToMovie(context, movieJson));
        }
        return movies;
    }

    public synchronized List<Movie> getAllMovies(Context context) {
        ensureInitialized(context);
        List<Movie> movies = new ArrayList<>();
        JSONArray array = readArray(context, KEY_MOVIES_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject movieJson = array.optJSONObject(i);
            if (movieJson == null) {
                continue;
            }
            movies.add(jsonToMovie(context, movieJson));
        }
        return movies;
    }

    public synchronized Movie getMovieById(Context context, String movieId) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_MOVIES_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject movieJson = array.optJSONObject(i);
            if (movieJson == null || !movieJson.optBoolean("active", true)) {
                continue;
            }
            if (movieId.equals(movieJson.optString("id"))) {
                return jsonToMovie(context, movieJson);
            }
        }
        return null;
    }

    public synchronized Movie saveMovie(Context context, Movie movie) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_MOVIES_JSON);
        JSONObject serializedMovie = movieToJson(movie);
        boolean updated = false;

        for (int i = 0; i < array.length(); i++) {
            JSONObject existing = array.optJSONObject(i);
            if (existing == null) {
                continue;
            }
            if (movie.getId().equals(existing.optString("id"))) {
                try {
                    array.put(i, serializedMovie);
                } catch (JSONException ignored) {
                    array.put(serializedMovie);
                }
                updated = true;
                break;
            }
        }

        if (!updated) {
            array.put(serializedMovie);
        }

        writeArray(context, KEY_MOVIES_JSON, array);
        return movie;
    }

    public synchronized boolean deactivateMovie(Context context, String movieId) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_MOVIES_JSON);
        boolean changed = false;
        for (int i = 0; i < array.length(); i++) {
            JSONObject movieJson = array.optJSONObject(i);
            if (movieJson == null) {
                continue;
            }
            if (movieId.equals(movieJson.optString("id"))) {
                try {
                    movieJson.put("active", false);
                    array.put(i, movieJson);
                    changed = true;
                } catch (JSONException ignored) {
                    return false;
                }
                break;
            }
        }

        if (changed) {
            writeArray(context, KEY_MOVIES_JSON, array);
        }
        return changed;
    }

    public synchronized User updateCredits(Context context, String userId, int newCredits) {
        User existing = getUserById(context, userId);
        if (existing == null) {
            return null;
        }

        existing.setCredits(newCredits);
        saveUser(context, existing);
        return existing;
    }

    public synchronized Order createOrder(Context context, String userId, List<CartItem> cartItems, int totalAmount) {
        ensureInitialized(context);
        JSONArray array = readArray(context, KEY_ORDERS_JSON);
        Order order = new Order();
        order.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        order.setUserId(userId);
        order.setMovies(cloneCartItems(cartItems));
        order.setTotalAmount(totalAmount);
        order.setTimestamp(Timestamp.now());
        order.setStatus("已完成");
        array.put(orderToJson(order));
        writeArray(context, KEY_ORDERS_JSON, array);
        return order;
    }

    public synchronized List<Order> getOrdersForUser(Context context, String userId) {
        ensureInitialized(context);
        List<Order> orders = new ArrayList<>();
        JSONArray array = readArray(context, KEY_ORDERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject orderJson = array.optJSONObject(i);
            if (orderJson == null) {
                continue;
            }
            if (userId.equals(orderJson.optString("userId"))) {
                orders.add(jsonToOrder(context, orderJson));
            }
        }
        return orders;
    }

    public synchronized List<Order> getAllOrders(Context context) {
        ensureInitialized(context);
        List<Order> orders = new ArrayList<>();
        JSONArray array = readArray(context, KEY_ORDERS_JSON);
        for (int i = 0; i < array.length(); i++) {
            JSONObject orderJson = array.optJSONObject(i);
            if (orderJson == null) {
                continue;
            }
            orders.add(jsonToOrder(context, orderJson));
        }
        return orders;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private JSONArray readArray(Context context, String key) {
        String raw = getPrefs(context).getString(key, "[]");
        try {
            return new JSONArray(raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private void writeArray(Context context, String key, JSONArray array) {
        getPrefs(context).edit().putString(key, array.toString()).apply();
    }

    private JSONArray buildDefaultUsers() {
        JSONArray array = new JSONArray();
        array.put(buildUserJson(
                Constants.ADMIN_USER_ID,
                Constants.ADMIN_USER_NAME,
                30,
                Constants.ADMIN_USER_EMAIL,
                SecurityUtil.hashPassword(Constants.ADMIN_USER_PASSWORD),
                5000,
                Constants.ROLE_ADMIN,
                true
        ));
        array.put(buildUserJson(
                "user_mary",
                Constants.TEST_USER_MARY_NAME,
                25,
                Constants.TEST_USER_MARY_EMAIL,
                SecurityUtil.hashPassword(Constants.TEST_USER_MARY_PASSWORD),
                Constants.DEFAULT_USER_CREDITS,
                Constants.ROLE_USER,
                true
        ));
        array.put(buildUserJson(
                "user_john",
                Constants.TEST_USER_JOHN_NAME,
                28,
                Constants.TEST_USER_JOHN_EMAIL,
                SecurityUtil.hashPassword(Constants.TEST_USER_JOHN_PASSWORD),
                Constants.DEFAULT_USER_CREDITS,
                Constants.ROLE_USER,
                true
        ));
        return array;
    }

    private JSONArray buildDefaultMovies() {
        JSONArray array = new JSONArray();
        array.put(buildMovieJson(
                "movie_avengers4",
                "复仇者联盟 4",
                "超级英雄们集结对抗灭霸",
                150,
                MovieMediaUtil.drawableRef("avengers4"),
                MovieMediaUtil.rawRef("avenger_trailer"),
                "动作",
                8.5f,
                "罗素兄弟",
                "小罗伯特·唐尼，克里斯·埃文斯"
        ));
        array.put(buildMovieJson(
                "movie_fast9",
                "速度与激情 9",
                "多米尼克和他的家人面临新的威胁",
                120,
                MovieMediaUtil.drawableRef("fast_and_furious"),
                MovieMediaUtil.rawRef("fastandfurious_trailer"),
                "动作",
                7.2f,
                "林诣彬",
                "范·迪塞尔，米歇尔·罗德里格兹"
        ));
        array.put(buildMovieJson(
                "movie_hangover",
                "宿醉",
                "四个朋友拉斯维加斯狂欢后的疯狂经历",
                80,
                MovieMediaUtil.drawableRef("ic_launcher_foreground"),
                MovieMediaUtil.rawRef("seabird1"),
                "喜剧",
                7.8f,
                "托德·菲利普斯",
                "布莱德利·库珀，艾德·赫尔姆斯"
        ));
        array.put(buildMovieJson(
                "movie_shawshank",
                "肖申克的救赎",
                "银行家安迪在监狱中的希望之旅",
                100,
                MovieMediaUtil.drawableRef("ic_launcher_foreground"),
                MovieMediaUtil.rawRef("seabird1"),
                "剧情",
                9.7f,
                "弗兰克·德拉邦特",
                "蒂姆·罗宾斯，摩根·弗里曼"
        ));
        return array;
    }

    private JSONObject buildUserJson(String uid, String name, int age, String email,
                                     String passwordHash, int credits, String role, boolean active) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", uid);
            jsonObject.put("name", name);
            jsonObject.put("age", age);
            jsonObject.put("email", email);
            jsonObject.put("passwordHash", passwordHash);
            jsonObject.put("credits", credits);
            jsonObject.put("role", role);
            jsonObject.put("active", active);
            jsonObject.put("createdAtMillis", System.currentTimeMillis());
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private JSONObject buildMovieJson(String id, String title, String description, int price,
                                      String posterUrl, String previewVideoUrl, String genre,
                                      float rating, String director, String cast) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("title", title);
            jsonObject.put("description", description);
            jsonObject.put("price", price);
            jsonObject.put("posterUrl", posterUrl);
            jsonObject.put("previewVideoUrl", previewVideoUrl);
            jsonObject.put("genre", genre);
            jsonObject.put("rating", rating);
            jsonObject.put("director", director);
            jsonObject.put("cast", cast);
            jsonObject.put("active", true);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private JSONObject userToJson(User user) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", user.getUid());
            jsonObject.put("name", user.getName());
            jsonObject.put("age", user.getAge());
            jsonObject.put("email", user.getEmail());
            jsonObject.put("passwordHash", user.getPasswordHash());
            jsonObject.put("credits", user.getCredits());
            jsonObject.put("role", user.getRole());
            jsonObject.put("active", user.isActive());
            long createdAtMillis = user.getCreatedAt() != null
                    ? user.getCreatedAt().toDate().getTime()
                    : System.currentTimeMillis();
            jsonObject.put("createdAtMillis", createdAtMillis);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private User jsonToUser(JSONObject jsonObject) {
        User user = new User();
        user.setUid(jsonObject.optString("uid"));
        user.setName(jsonObject.optString("name"));
        user.setAge(jsonObject.optInt("age"));
        user.setEmail(jsonObject.optString("email"));
        user.setPasswordHash(jsonObject.optString("passwordHash"));
        user.setCredits(jsonObject.optInt("credits"));
        user.setRole(jsonObject.optString("role", Constants.ROLE_USER));
        user.setActive(jsonObject.optBoolean("active", true));
        user.setCreatedAt(new Timestamp(new Date(jsonObject.optLong("createdAtMillis", System.currentTimeMillis()))));
        return user;
    }

    private JSONObject movieToJson(Movie movie) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", movie.getId());
            jsonObject.put("title", movie.getTitle());
            jsonObject.put("description", movie.getDescription());
            jsonObject.put("price", movie.getPrice());
            jsonObject.put("posterUrl", movie.getPosterUrl());
            jsonObject.put("previewVideoUrl", movie.getPreviewVideoUrl());
            jsonObject.put("genre", movie.getGenre());
            jsonObject.put("rating", movie.getRating());
            jsonObject.put("director", movie.getDirector());
            jsonObject.put("cast", movie.getCast());
            jsonObject.put("active", true);
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private Movie jsonToMovie(Context context, JSONObject jsonObject) {
        Movie movie = new Movie();
        movie.setId(jsonObject.optString("id"));
        movie.setTitle(jsonObject.optString("title"));
        movie.setDescription(jsonObject.optString("description"));
        movie.setPrice(jsonObject.optInt("price"));
        movie.setPosterUrl(jsonObject.optString("posterUrl"));
        movie.setPreviewVideoUrl(jsonObject.optString("previewVideoUrl"));
        movie.setGenre(jsonObject.optString("genre"));
        movie.setRating((float) jsonObject.optDouble("rating"));
        movie.setDirector(jsonObject.optString("director"));
        movie.setCast(jsonObject.optString("cast"));
        MovieMediaUtil.hydrateLocalResources(context, movie);
        return movie;
    }

    private JSONObject orderToJson(Order order) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("orderId", order.getOrderId());
            jsonObject.put("userId", order.getUserId());
            jsonObject.put("totalAmount", order.getTotalAmount());
            jsonObject.put("status", order.getStatus());
            long timestampMillis = order.getTimestamp() != null
                    ? order.getTimestamp().toDate().getTime()
                    : System.currentTimeMillis();
            jsonObject.put("timestampMillis", timestampMillis);
            jsonObject.put("movies", cartItemsToJson(order.getMovies()));
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    private Order jsonToOrder(Context context, JSONObject jsonObject) {
        Order order = new Order();
        order.setOrderId(jsonObject.optString("orderId"));
        order.setUserId(jsonObject.optString("userId"));
        order.setTotalAmount(jsonObject.optInt("totalAmount"));
        order.setStatus(jsonObject.optString("status", "已完成"));
        order.setTimestamp(new Timestamp(new Date(jsonObject.optLong("timestampMillis", System.currentTimeMillis()))));
        order.setMovies(jsonToCartItems(context, jsonObject.optJSONArray("movies")));
        return order;
    }

    private JSONArray cartItemsToJson(List<CartItem> items) {
        JSONArray array = new JSONArray();
        if (items == null) {
            return array;
        }

        for (CartItem item : items) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("quantity", item.getQuantity());
                jsonObject.put("movie", movieToJson(item.getMovie()));
                array.put(jsonObject);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    private List<CartItem> jsonToCartItems(Context context, JSONArray array) {
        List<CartItem> items = new ArrayList<>();
        if (array == null) {
            return items;
        }

        for (int i = 0; i < array.length(); i++) {
            JSONObject itemJson = array.optJSONObject(i);
            if (itemJson == null) {
                continue;
            }
            Movie movie = jsonToMovie(context, itemJson.optJSONObject("movie"));
            items.add(new CartItem(movie, itemJson.optInt("quantity", 1)));
        }
        return items;
    }

    private List<CartItem> cloneCartItems(List<CartItem> items) {
        List<CartItem> cloned = new ArrayList<>();
        if (items == null) {
            return cloned;
        }

        for (CartItem item : items) {
            Movie movie = item.getMovie();
            Movie copiedMovie = new Movie();
            copiedMovie.setId(movie.getId());
            copiedMovie.setTitle(movie.getTitle());
            copiedMovie.setDescription(movie.getDescription());
            copiedMovie.setPrice(movie.getPrice());
            copiedMovie.setPosterUrl(movie.getPosterUrl());
            copiedMovie.setPreviewVideoUrl(movie.getPreviewVideoUrl());
            copiedMovie.setGenre(movie.getGenre());
            copiedMovie.setRating(movie.getRating());
            copiedMovie.setDirector(movie.getDirector());
            copiedMovie.setCast(movie.getCast());
            if (movie.isLocalImage()) {
                copiedMovie.setPosterResourceId(movie.getPosterResourceId());
            }
            cloned.add(new CartItem(copiedMovie, item.getQuantity()));
        }
        return cloned;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
