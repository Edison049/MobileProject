package com.innovationai.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.DefaultMovieCatalog;
import com.innovationai.myapplication.util.FirebaseUtil;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.SecurityUtil;
import com.innovationai.myapplication.util.SessionManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用统一数据仓库
 * 有Firebase配置时优先走Firestore，否则自动回退到本地持久化
 */
public final class AppRepository {
    private static final String TAG = "AppRepository";
    private static final String FIREBASE_BOOTSTRAP_PREF = "firebase_bootstrap_state";
    private static final String KEY_LOCAL_SYNC_COMPLETED = "local_sync_completed";
    private static final String KEY_BOOTSTRAP_VERSION = "bootstrap_version";
    private static final int CURRENT_BOOTSTRAP_VERSION = 2;
    private static final long FIREBASE_INIT_TIMEOUT_MS = 15_000L;
    private static final long FIREBASE_RETRY_INTERVAL_MS = 30_000L;

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private static final AppRepository INSTANCE = new AppRepository();

    private final LocalDataStore localDataStore = LocalDataStore.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean useLocalFallback;
    private volatile boolean forceLocalModeForTesting;
    private volatile long lastFirebaseAttemptElapsedMs;
    private volatile String firebaseStatusMessage = "Firebase has not been initialized yet";

    private AppRepository() {
    }

    public static AppRepository getInstance() {
        return INSTANCE;
    }

    public boolean isUsingFirebase(Context context) {
        return FirebaseUtil.isFirebaseConfigured(context)
                && !useLocalFallback
                && !forceLocalModeForTesting;
    }

    public String getDataModeLabel(Context context) {
        if (!FirebaseUtil.isFirebaseConfigured(context)) {
            return "Local mode";
        }
        return useLocalFallback ? "Local mode (Firebase offline)" : "Firebase";
    }

    public String getFirebaseStatusMessage(Context context) {
        if (!FirebaseUtil.isFirebaseConfigured(context)) {
            return "Firebase configuration file was not found";
        }
        if (forceLocalModeForTesting) {
            return "Test mode is enabled, so local data is being used";
        }
        return firebaseStatusMessage;
    }

    public void initialize(Context context, ActionCallback callback) {
        localDataStore.ensureInitialized(context);
        boolean bootstrapRefreshRequired = requiresBootstrapRefresh(context);
        if (forceLocalModeForTesting) {
            firebaseStatusMessage = "Test mode is enabled, so local data is being used";
            postActionSuccess(callback);
            return;
        }
        if (!FirebaseUtil.isFirebaseConfigured(context)) {
            firebaseStatusMessage = "Firebase configuration file was not found";
            postActionSuccess(callback);
            return;
        }

        if (useLocalFallback && !shouldRetryFirebaseNow()) {
            postActionSuccess(callback);
            return;
        }

        lastFirebaseAttemptElapsedMs = SystemClock.elapsedRealtime();

        if (!FirebaseUtil.hasUsableNetwork(context)) {
            useLocalFallback = true;
            firebaseStatusMessage = "No network connection is available, so the app switched to local mode";
            postActionSuccess(callback);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            useLocalFallback = true;
            firebaseStatusMessage = "Firebase initialization failed because Firestore could not be created";
            postActionSuccess(callback);
            return;
        }

        AtomicBoolean initializationHandled = new AtomicBoolean(false);
        Runnable timeoutRunnable = () -> {
            if (!initializationHandled.compareAndSet(false, true)) {
                return;
            }

            firebaseStatusMessage = "Firebase timed out after 15 seconds, so the app switched to local mode";
            Log.w(TAG, firebaseStatusMessage);
            useLocalFallback = true;
            postActionSuccess(callback);
        };
        mainHandler.postDelayed(timeoutRunnable, FIREBASE_INIT_TIMEOUT_MS);

        List<Task<?>> setupTasks = new ArrayList<>();
        setupTasks.add(ensureUserDocument(db, Constants.ADMIN_USER_ID, buildUserMap(
                Constants.ADMIN_USER_ID,
                Constants.ADMIN_USER_NAME,
                30,
                Constants.ADMIN_USER_EMAIL,
                SecurityUtil.hashPassword(Constants.ADMIN_USER_PASSWORD),
                5000,
                Constants.ROLE_ADMIN,
                true
        )));
        setupTasks.add(ensureUserDocument(db, "user_mary", buildUserMap(
                "user_mary",
                Constants.TEST_USER_MARY_NAME,
                25,
                Constants.TEST_USER_MARY_EMAIL,
                SecurityUtil.hashPassword(Constants.TEST_USER_MARY_PASSWORD),
                Constants.DEFAULT_USER_CREDITS,
                Constants.ROLE_USER,
                true
        )));
        setupTasks.add(ensureUserDocument(db, "user_john", buildUserMap(
                "user_john",
                Constants.TEST_USER_JOHN_NAME,
                28,
                Constants.TEST_USER_JOHN_EMAIL,
                SecurityUtil.hashPassword(Constants.TEST_USER_JOHN_PASSWORD),
                Constants.DEFAULT_USER_CREDITS,
                Constants.ROLE_USER,
                true
        )));

        for (Movie defaultMovie : DefaultMovieCatalog.getDefaultMovies()) {
            Task<?> defaultMovieTask = bootstrapRefreshRequired
                    ? upsertMovieDocument(db, defaultMovie.getId(), movieToMap(defaultMovie, true))
                    : ensureMovieDocument(db, defaultMovie.getId(), movieToMap(defaultMovie, true));
            setupTasks.add(defaultMovieTask);
        }

        if (bootstrapRefreshRequired || !isLocalSyncCompleted(context)) {
            setupTasks.addAll(buildLocalSyncTasks(context, db));
        }

        Tasks.whenAllComplete(setupTasks)
                .addOnCompleteListener(task -> {
                    if (!initializationHandled.compareAndSet(false, true)) {
                        return;
                    }

                    mainHandler.removeCallbacks(timeoutRunnable);
                    List<? extends Task<?>> completedTasks = task.getResult();
                    String failureMessage = extractFailureMessage(completedTasks);
                    if (failureMessage != null) {
                        firebaseStatusMessage = "Firebase initialization failed: " + failureMessage;
                        Log.w(TAG, firebaseStatusMessage);
                        useLocalFallback = true;
                        postActionSuccess(callback);
                        return;
                    }

                    useLocalFallback = false;
                    firebaseStatusMessage = "Firebase is connected";
                    if (bootstrapRefreshRequired || !isLocalSyncCompleted(context)) {
                        markLocalSyncCompleted(context);
                    }
                    markBootstrapVersion(context);
                    postActionSuccess(callback);
                });
    }

    public boolean isLoggedIn(Context context) {
        return SessionManager.isLoggedIn(context);
    }

    public void logout(Context context) {
        SessionManager.clearSession(context);
    }

    public void setForceLocalModeForTesting(boolean enabled) {
        forceLocalModeForTesting = enabled;
        if (enabled) {
            useLocalFallback = false;
        }
    }

    public void login(Context context, String identifier, String password, DataCallback<User> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            User user = localDataStore.findActiveUserByLogin(context, identifier);
            if (user == null) {
                postError(callback, "Username or email was not found");
                return;
            }
            if (!SecurityUtil.matches(password, user.getPasswordHash())) {
                postError(callback, "Incorrect password");
                return;
            }
            SessionManager.saveCurrentUserId(context, user.getUid());
            postSuccess(callback, user);
            return;
        }

        initialize(context, new ActionCallback() {
            @Override
            public void onSuccess() {
                loginWithFirebase(context, identifier, password, callback);
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void loginWithFirebase(Context context, String identifier, String password, DataCallback<User> callback) {
        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    User matchedUser = null;
                    String lowerIdentifier = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = documentToUser(context, document);
                        if (user == null) {
                            continue;
                        }
                        String name = user.getName() == null ? "" : user.getName().toLowerCase(Locale.ROOT);
                        String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.ROOT);
                        if (lowerIdentifier.equals(name) || lowerIdentifier.equals(email)) {
                            matchedUser = user;
                            break;
                        }
                    }

                    if (matchedUser == null) {
                        postError(callback, "Username or email was not found");
                        return;
                    }

                    if (!SecurityUtil.matches(password, matchedUser.getPasswordHash())) {
                        postError(callback, "Incorrect password");
                        return;
                    }

                    SessionManager.saveCurrentUserId(context, matchedUser.getUid());
                    postSuccess(callback, matchedUser);
                })
                .addOnFailureListener(e -> postError(callback, "Login failed: " + e.getMessage()));
    }

    public void register(Context context, String name, int age, String email, String password,
                         DataCallback<User> callback) {
        createUser(context, name, age, email, password, Constants.DEFAULT_USER_CREDITS,
                Constants.ROLE_USER, true, callback);
    }

    public void createUser(Context context, String name, int age, String email, String password,
                           int credits, String role, boolean autoLogin,
                           DataCallback<User> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            if (localDataStore.userNameOrEmailExists(context, name, email, null)) {
                postError(callback, "The username or email already exists");
                return;
            }

            User user = new User(
                    UUID.randomUUID().toString(),
                    name,
                    age,
                    email,
                    SecurityUtil.hashPassword(password),
                    credits,
                    role,
                    true,
                    Timestamp.now()
            );
            localDataStore.saveUser(context, user);
            if (autoLogin) {
                SessionManager.saveCurrentUserId(context, user.getUid());
            }
            postSuccess(callback, user);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String existingName = safeLower(document.getString("name"));
                        String existingEmail = safeLower(document.getString("email"));
                        if (safeLower(name).equals(existingName) || safeLower(email).equals(existingEmail)) {
                            postError(callback, "The username or email already exists");
                            return;
                        }
                    }

                    DocumentReference documentReference = db.collection(Constants.COLLECTION_USERS).document();
                    User user = new User(
                            documentReference.getId(),
                            name,
                            age,
                            email,
                            SecurityUtil.hashPassword(password),
                            credits,
                            role,
                            true,
                            Timestamp.now()
                    );
                    documentReference.set(userToMap(user))
                            .addOnSuccessListener(unused -> {
                                if (autoLogin) {
                                    SessionManager.saveCurrentUserId(context, user.getUid());
                                }
                                postSuccess(callback, user);
                            })
                            .addOnFailureListener(e -> postError(callback, "Failed to create user: " + e.getMessage()));
                })
                .addOnFailureListener(e -> postError(callback, "Failed to create user: " + e.getMessage()));
    }

    public void loadCurrentUser(Context context, DataCallback<User> callback) {
        String currentUserId = SessionManager.getCurrentUserId(context);
        if (currentUserId == null) {
            postError(callback, "No user is currently signed in");
            return;
        }

        loadUserById(context, currentUserId, callback);
    }

    public void loadUserById(Context context, String userId, DataCallback<User> callback) {
        initialize(context, new ActionCallback() {
            @Override
            public void onSuccess() {
                loadUserByIdInternal(context, userId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void loadUserByIdInternal(Context context, String userId, DataCallback<User> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            User user = localDataStore.getUserById(context, userId);
            if (user == null || !user.isActive()) {
                if (userId.equals(SessionManager.getCurrentUserId(context))) {
                    SessionManager.clearSession(context);
                }
                postError(callback, "The user was not found or has been deactivated");
                return;
            }
            postSuccess(callback, user);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentToUser(context, documentSnapshot);
                    if (user == null || !user.isActive()) {
                        if (userId.equals(SessionManager.getCurrentUserId(context))) {
                            SessionManager.clearSession(context);
                        }
                        postError(callback, "The user was not found or has been deactivated");
                        return;
                    }
                    postSuccess(callback, user);
                })
                .addOnFailureListener(e -> postError(callback, "Failed to load user: " + e.getMessage()));
    }

    public void loadMovies(Context context, DataCallback<List<Movie>> callback) {
        initialize(context, new ActionCallback() {
            @Override
            public void onSuccess() {
                loadMoviesInternal(context, callback);
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void loadMoviesInternal(Context context, DataCallback<List<Movie>> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            List<Movie> movies = new ArrayList<>(localDataStore.getActiveMovies(context));
            sortMoviesByTitle(movies);
            postSuccess(callback, movies);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_MOVIES)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Movie> movies = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Movie movie = documentToMovie(context, document);
                        if (movie != null) {
                            movies.add(movie);
                        }
                    }
                    sortMoviesByTitle(movies);
                    postSuccess(callback, movies);
                })
                .addOnFailureListener(e -> postError(callback, "Failed to load movies: " + e.getMessage()));
    }

    public void loadMovieById(Context context, String movieId, DataCallback<Movie> callback) {
        initialize(context, new ActionCallback() {
            @Override
            public void onSuccess() {
                loadMovieByIdInternal(context, movieId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void loadMovieByIdInternal(Context context, String movieId, DataCallback<Movie> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            Movie movie = localDataStore.getMovieById(context, movieId);
            if (movie == null) {
                postError(callback, "The movie was not found or has been removed");
                return;
            }
            postSuccess(callback, movie);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_MOVIES)
                .document(movieId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Movie movie = documentToMovie(context, documentSnapshot);
                    Boolean active = documentSnapshot.getBoolean("active");
                    if (movie == null || Boolean.FALSE.equals(active)) {
                        postError(callback, "The movie was not found or has been removed");
                        return;
                    }
                    postSuccess(callback, movie);
                })
                .addOnFailureListener(e -> postError(callback, "Failed to load movie: " + e.getMessage()));
    }

    public void addMovie(Context context, Movie movie, ActionCallback callback) {
        if (movie.getId() == null || movie.getId().trim().isEmpty()) {
            movie.setId(UUID.randomUUID().toString());
        }

        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            localDataStore.saveMovie(context, movie);
            postActionSuccess(callback);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postActionError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_MOVIES)
                .document(movie.getId())
                .set(movieToMap(movie, true))
                .addOnSuccessListener(unused -> postActionSuccess(callback))
                .addOnFailureListener(e -> postActionError(callback, "Failed to add movie: " + e.getMessage()));
    }

    public void deleteMovie(Context context, String movieId, ActionCallback callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            if (localDataStore.deactivateMovie(context, movieId)) {
                postActionSuccess(callback);
            } else {
                postActionError(callback, "Failed to delete the movie");
            }
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postActionError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_MOVIES)
                .document(movieId)
                .update("active", false)
                .addOnSuccessListener(unused -> postActionSuccess(callback))
                .addOnFailureListener(e -> postActionError(callback, "Failed to delete the movie: " + e.getMessage()));
    }

    public void loadUsers(Context context, DataCallback<List<User>> callback) {
        initialize(context, new ActionCallback() {
            @Override
            public void onSuccess() {
                loadUsersInternal(context, callback);
            }

            @Override
            public void onError(String errorMessage) {
                postError(callback, errorMessage);
            }
        });
    }

    private void loadUsersInternal(Context context, DataCallback<List<User>> callback) {
        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            List<User> users = new ArrayList<>(localDataStore.getActiveUsers(context));
            sortUsersByName(users);
            postSuccess(callback, users);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = documentToUser(context, document);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    sortUsersByName(users);
                    postSuccess(callback, users);
                })
                .addOnFailureListener(e -> postError(callback, "Failed to load users: " + e.getMessage()));
    }

    public void deleteUser(Context context, String userId, ActionCallback callback) {
        String currentUserId = SessionManager.getCurrentUserId(context);
        if (userId != null && userId.equals(currentUserId)) {
            postActionError(callback, "You cannot delete the administrator who is currently signed in");
            return;
        }

        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            if (localDataStore.deactivateUser(context, userId)) {
                postActionSuccess(callback);
            } else {
                postActionError(callback, "Failed to delete the user");
            }
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postActionError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("active", false)
                .addOnSuccessListener(unused -> postActionSuccess(callback))
                .addOnFailureListener(e -> postActionError(callback, "Failed to delete the user: " + e.getMessage()));
    }

    public void topUpCredits(Context context, int amount, DataCallback<User> callback) {
        String currentUserId = SessionManager.getCurrentUserId(context);
        if (currentUserId == null) {
            postError(callback, "Please sign in first");
            return;
        }

        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            User user = localDataStore.getUserById(context, currentUserId);
            if (user == null || !user.isActive()) {
                postError(callback, "The user was not found or has been deactivated");
                return;
            }

            User updatedUser = localDataStore.updateCredits(context, currentUserId, user.getCredits() + amount);
            postSuccess(callback, updatedUser);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        DocumentReference userRef = db.collection(Constants.COLLECTION_USERS).document(currentUserId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(userRef);
            Long credits = snapshot.getLong("credits");
            int newCredits = (credits == null ? 0 : credits.intValue()) + amount;
            transaction.update(userRef, "credits", newCredits);
            return newCredits;
        }).addOnSuccessListener(unused -> loadUserById(context, currentUserId, callback))
                .addOnFailureListener(e -> postError(callback, "Top-up failed: " + e.getMessage()));
    }

    public void buyMovie(Context context, Movie movie, DataCallback<User> callback) {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(movie, 1));
        purchaseCartItems(context, items, callback);
    }

    public void checkoutCart(Context context, List<CartItem> cartItems, DataCallback<User> callback) {
        purchaseCartItems(context, cartItems, callback);
    }

    private void purchaseCartItems(Context context, List<CartItem> cartItems, DataCallback<User> callback) {
        String currentUserId = SessionManager.getCurrentUserId(context);
        if (currentUserId == null) {
            postError(callback, "Please sign in first");
            return;
        }

        int totalAmount = 0;
        for (CartItem item : cartItems) {
            totalAmount += item.getTotalPrice();
        }
        final int purchaseTotal = totalAmount;

        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            User user = localDataStore.getUserById(context, currentUserId);
            if (user == null || !user.isActive()) {
                postError(callback, "The user was not found or has been deactivated");
                return;
            }
            if (user.getCredits() < purchaseTotal) {
                postError(callback, "You do not have enough credits to complete this purchase");
                return;
            }

            User updatedUser = localDataStore.updateCredits(context, currentUserId, user.getCredits() - purchaseTotal);
            localDataStore.createOrder(context, currentUserId, cartItems, purchaseTotal);
            postSuccess(callback, updatedUser);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        DocumentReference userRef = db.collection(Constants.COLLECTION_USERS).document(currentUserId);
        DocumentReference orderRef = db.collection(Constants.COLLECTION_ORDERS).document();
        Map<String, Object> orderMap = buildOrderMap(
                orderRef.getId(),
                currentUserId,
                cartItems,
                purchaseTotal,
                Timestamp.now()
        );

        db.runTransaction(transaction -> {
            DocumentSnapshot userSnapshot = transaction.get(userRef);
            Boolean active = userSnapshot.getBoolean("active");
            Long credits = userSnapshot.getLong("credits");

            if (Boolean.FALSE.equals(active)) {
                throw new IllegalStateException("The current user has been deactivated");
            }

            int currentCredits = credits == null ? 0 : credits.intValue();
            if (currentCredits < purchaseTotal) {
                throw new IllegalStateException("You do not have enough credits to complete this purchase");
            }

            transaction.update(userRef, "credits", currentCredits - purchaseTotal);
            transaction.set(orderRef, orderMap);
            return true;
        }).addOnSuccessListener(unused -> loadUserById(context, currentUserId, callback))
                .addOnFailureListener(e -> postError(callback, e.getMessage() == null ? "Purchase failed" : e.getMessage()));
    }

    public void loadOrders(Context context, DataCallback<List<Order>> callback) {
        String currentUserId = SessionManager.getCurrentUserId(context);
        if (currentUserId == null) {
            postError(callback, "Please sign in first");
            return;
        }

        if (!isUsingFirebase(context)) {
            localDataStore.ensureInitialized(context);
            List<Order> orders = new ArrayList<>(localDataStore.getOrdersForUser(context, currentUserId));
            sortOrdersByLatest(orders);
            postSuccess(callback, orders);
            return;
        }

        FirebaseFirestore db = FirebaseUtil.getFirestore(context);
        if (db == null) {
            postError(callback, "Firebase is not fully configured");
            return;
        }

        db.collection(Constants.COLLECTION_ORDERS)
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Order> orders = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        orders.add(documentToOrder(context, document));
                    }
                    sortOrdersByLatest(orders);
                    postSuccess(callback, orders);
                })
                .addOnFailureListener(e -> postError(callback, "Failed to load orders: " + e.getMessage()));
    }

    private void sortMoviesByTitle(List<Movie> movies) {
        movies.sort(Comparator.comparing(movie ->
                safeLower(movie == null ? "" : movie.getTitle())));
    }

    private void sortUsersByName(List<User> users) {
        users.sort(Comparator.comparing(user ->
                safeLower(user == null ? "" : user.getName())));
    }

    private void sortOrdersByLatest(List<Order> orders) {
        orders.sort((left, right) -> {
            long leftTime = left == null || left.getTimestamp() == null
                    ? 0L
                    : left.getTimestamp().toDate().getTime();
            long rightTime = right == null || right.getTimestamp() == null
                    ? 0L
                    : right.getTimestamp().toDate().getTime();
            return Long.compare(rightTime, leftTime);
        });
    }

    private Task<?> ensureUserDocument(FirebaseFirestore db, String documentId, Map<String, Object> data) {
        DocumentReference documentReference = db.collection(Constants.COLLECTION_USERS).document(documentId);
        return documentReference.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                return Tasks.forResult(null);
            }
            return documentReference.set(data);
        });
    }

    private Task<?> ensureMovieDocument(FirebaseFirestore db, String documentId, Map<String, Object> data) {
        DocumentReference documentReference = db.collection(Constants.COLLECTION_MOVIES).document(documentId);
        return documentReference.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                return Tasks.forResult(null);
            }
            return documentReference.set(data);
        });
    }

    private Task<?> upsertMovieDocument(FirebaseFirestore db, String documentId, Map<String, Object> data) {
        return db.collection(Constants.COLLECTION_MOVIES)
                .document(documentId)
                .set(data);
    }

    private Map<String, Object> buildUserMap(String uid, String name, int age, String email,
                                             String passwordHash, int credits, String role, boolean active) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("name", name);
        data.put("age", age);
        data.put("email", email);
        data.put("passwordHash", passwordHash);
        data.put("credits", credits);
        data.put("role", role);
        data.put("active", active);
        data.put("createdAt", Timestamp.now());
        return data;
    }

    private Map<String, Object> buildMovieMap(String id, String title, String description, int price,
                                              String posterUrl, String previewVideoUrl, String genre,
                                              float rating, String director, String cast, boolean active) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("title", title);
        data.put("description", description);
        data.put("price", price);
        data.put("posterUrl", posterUrl);
        data.put("previewVideoUrl", previewVideoUrl);
        data.put("genre", DefaultMovieCatalog.normalizeGenre(genre));
        data.put("rating", rating);
        data.put("director", director);
        data.put("cast", cast);
        data.put("active", active);
        return data;
    }

    private Map<String, Object> userToMap(User user) {
        return buildUserMap(
                user.getUid(),
                user.getName(),
                user.getAge(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCredits(),
                user.getRole(),
                user.isActive()
        );
    }

    private Map<String, Object> movieToMap(Movie movie, boolean active) {
        return buildMovieMap(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getPrice(),
                movie.getPosterUrl(),
                movie.getPreviewVideoUrl(),
                movie.getGenre(),
                movie.getRating(),
                movie.getDirector(),
                movie.getCast(),
                active
        );
    }

    private Map<String, Object> buildOrderMap(String orderId, String userId, List<CartItem> items,
                                              int totalAmount, Timestamp timestamp) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("orderId", orderId);
        orderMap.put("userId", userId);
        orderMap.put("totalAmount", totalAmount);
        orderMap.put("status", "Completed");
        orderMap.put("timestamp", timestamp == null ? Timestamp.now() : timestamp);

        List<Map<String, Object>> movies = new ArrayList<>();
        for (CartItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("movie", movieToMap(item.getMovie(), true));
            movies.add(itemMap);
        }
        orderMap.put("movies", movies);
        return orderMap;
    }

    private List<Task<?>> buildLocalSyncTasks(Context context, FirebaseFirestore db) {
        List<Task<?>> tasks = new ArrayList<>();

        for (User user : localDataStore.getAllUsers(context)) {
            if (user == null || user.getUid() == null || user.getUid().trim().isEmpty()) {
                continue;
            }
            tasks.add(db.collection(Constants.COLLECTION_USERS)
                    .document(user.getUid())
                    .set(userToMap(user)));
        }

        for (Movie movie : localDataStore.getAllMovies(context)) {
            if (movie == null || movie.getId() == null || movie.getId().trim().isEmpty()) {
                continue;
            }
            tasks.add(db.collection(Constants.COLLECTION_MOVIES)
                    .document(movie.getId())
                    .set(movieToMap(movie, true)));
        }

        for (Order order : localDataStore.getAllOrders(context)) {
            if (order == null || order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
                continue;
            }
            tasks.add(db.collection(Constants.COLLECTION_ORDERS)
                    .document(order.getOrderId())
                    .set(orderToMap(order)));
        }

        return tasks;
    }

    private Map<String, Object> orderToMap(Order order) {
        return buildOrderMap(
                order.getOrderId(),
                order.getUserId(),
                order.getMovies(),
                order.getTotalAmount(),
                order.getTimestamp()
        );
    }

    private String extractFailureMessage(List<? extends Task<?>> tasks) {
        if (tasks == null) {
            return "Unknown error";
        }

        for (Task<?> task : tasks) {
            if (task == null || task.isSuccessful()) {
                continue;
            }

            Exception exception = task.getException();
            if (exception != null && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
                return exception.getMessage();
            }
            return "Unknown error";
        }
        return null;
    }

    private boolean isLocalSyncCompleted(Context context) {
        return getBootstrapPrefs(context).getBoolean(KEY_LOCAL_SYNC_COMPLETED, false);
    }

    private void markLocalSyncCompleted(Context context) {
        getBootstrapPrefs(context).edit().putBoolean(KEY_LOCAL_SYNC_COMPLETED, true).apply();
    }

    private boolean requiresBootstrapRefresh(Context context) {
        return getBootstrapPrefs(context).getInt(KEY_BOOTSTRAP_VERSION, 0) < CURRENT_BOOTSTRAP_VERSION;
    }

    private void markBootstrapVersion(Context context) {
        getBootstrapPrefs(context).edit().putInt(KEY_BOOTSTRAP_VERSION, CURRENT_BOOTSTRAP_VERSION).apply();
    }

    private SharedPreferences getBootstrapPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FIREBASE_BOOTSTRAP_PREF, Context.MODE_PRIVATE);
    }

    private boolean shouldRetryFirebaseNow() {
        return SystemClock.elapsedRealtime() - lastFirebaseAttemptElapsedMs >= FIREBASE_RETRY_INTERVAL_MS;
    }

    private User documentToUser(Context context, DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        User user = new User();
        user.setUid(document.getId());
        user.setName(document.getString("name"));
        Long age = document.getLong("age");
        user.setAge(age == null ? 0 : age.intValue());
        user.setEmail(document.getString("email"));
        user.setPasswordHash(document.getString("passwordHash"));
        Long credits = document.getLong("credits");
        user.setCredits(credits == null ? 0 : credits.intValue());
        user.setRole(document.getString("role") == null ? Constants.ROLE_USER : document.getString("role"));
        Boolean active = document.getBoolean("active");
        user.setActive(active == null || active);
        Timestamp createdAt = document.getTimestamp("createdAt");
        user.setCreatedAt(createdAt == null ? Timestamp.now() : createdAt);
        return user;
    }

    private Movie documentToMovie(Context context, DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        Movie movie = new Movie();
        movie.setId(document.getId());
        movie.setTitle(document.getString("title"));
        movie.setDescription(document.getString("description"));
        Long price = document.getLong("price");
        movie.setPrice(price == null ? 0 : price.intValue());
        movie.setPosterUrl(document.getString("posterUrl"));
        movie.setPreviewVideoUrl(document.getString("previewVideoUrl"));
        movie.setGenre(DefaultMovieCatalog.normalizeGenre(document.getString("genre")));
        Double rating = document.getDouble("rating");
        movie.setRating(rating == null ? 0f : rating.floatValue());
        movie.setDirector(document.getString("director"));
        movie.setCast(document.getString("cast"));
        DefaultMovieCatalog.normalizeMovie(movie);
        MovieMediaUtil.hydrateLocalResources(context, movie);
        return movie;
    }

    @SuppressWarnings("unchecked")
    private Order documentToOrder(Context context, DocumentSnapshot document) {
        Order order = new Order();
        order.setOrderId(document.getString("orderId"));
        order.setUserId(document.getString("userId"));
        Long totalAmount = document.getLong("totalAmount");
        order.setTotalAmount(totalAmount == null ? 0 : totalAmount.intValue());
        order.setStatus(DefaultMovieCatalog.normalizeOrderStatus(document.getString("status")));
        Timestamp timestamp = document.getTimestamp("timestamp");
        order.setTimestamp(timestamp == null ? Timestamp.now() : timestamp);

        List<CartItem> items = new ArrayList<>();
        List<Map<String, Object>> movies = (List<Map<String, Object>>) document.get("movies");
        if (movies != null) {
            for (Map<String, Object> itemMap : movies) {
                int quantity = ((Number) itemMap.getOrDefault("quantity", 1)).intValue();
                Map<String, Object> movieMap = (Map<String, Object>) itemMap.get("movie");
                Movie movie = movieFromMap(context, movieMap);
                items.add(new CartItem(movie, quantity));
            }
        }
        order.setMovies(items);
        return order;
    }

    private Movie movieFromMap(Context context, Map<String, Object> movieMap) {
        Movie movie = new Movie();
        if (movieMap == null) {
            return movie;
        }
        movie.setId((String) movieMap.get("id"));
        movie.setTitle((String) movieMap.get("title"));
        movie.setDescription((String) movieMap.get("description"));
        Number price = (Number) movieMap.get("price");
        movie.setPrice(price == null ? 0 : price.intValue());
        movie.setPosterUrl((String) movieMap.get("posterUrl"));
        movie.setPreviewVideoUrl((String) movieMap.get("previewVideoUrl"));
        movie.setGenre(DefaultMovieCatalog.normalizeGenre((String) movieMap.get("genre")));
        Number rating = (Number) movieMap.get("rating");
        movie.setRating(rating == null ? 0f : rating.floatValue());
        movie.setDirector((String) movieMap.get("director"));
        movie.setCast((String) movieMap.get("cast"));
        DefaultMovieCatalog.normalizeMovie(movie);
        MovieMediaUtil.hydrateLocalResources(context, movie);
        return movie;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private <T> void postSuccess(DataCallback<T> callback, T data) {
        mainHandler.post(() -> callback.onSuccess(data));
    }

    private <T> void postError(DataCallback<T> callback, String errorMessage) {
        mainHandler.post(() -> callback.onError(errorMessage));
    }

    private void postActionSuccess(ActionCallback callback) {
        mainHandler.post(callback::onSuccess);
    }

    private void postActionError(ActionCallback callback, String errorMessage) {
        mainHandler.post(() -> callback.onError(errorMessage));
    }
}
