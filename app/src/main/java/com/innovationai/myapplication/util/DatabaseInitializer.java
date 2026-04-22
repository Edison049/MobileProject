package com.innovationai.myapplication.util;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.innovationai.myapplication.model.Movie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库初始化工具类
 * 用于初始化测试数据和演示数据
 */
public class DatabaseInitializer {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * 初始化测试电影数据
     */
    public static void initializeSampleMovies() {
        // 动作片数据
        Movie[] actionMovies = {
            new Movie("action_1", "Avengers: Endgame", "Earth's mightiest heroes assemble for one final battle against Thanos.", 150,
                     "https://example.com/avengers4.jpg", 
                     "https://example.com/avengers4_trailer.mp4", 
                     "Action", 8.5f, "Anthony and Joe Russo", "Robert Downey Jr., Chris Evans"),
            new Movie("action_2", "F9", "Dom and his family face a dangerous new threat from the past.", 120,
                     "https://example.com/fast9.jpg", 
                     "https://example.com/fast9_trailer.mp4", 
                     "Action", 7.2f, "Justin Lin", "Vin Diesel, Michelle Rodriguez"),
            new Movie("action_3", "Mission: Impossible - Dead Reckoning Part One", "Ethan Hunt faces his most dangerous mission yet.", 180,
                     "https://example.com/mission7.jpg", 
                     "https://example.com/mission7_trailer.mp4", 
                     "Action", 8.1f, "Christopher McQuarrie", "Tom Cruise, Rebecca Ferguson")
        };

        // 喜剧片数据
        Movie[] comedyMovies = {
            new Movie("comedy_1", "The Hangover", "A wild bachelor party in Las Vegas spirals into unforgettable chaos.", 80,
                     "https://example.com/hangover.jpg", 
                     "https://example.com/hangover_trailer.mp4", 
                     "Comedy", 7.8f, "Todd Phillips", "Bradley Cooper, Ed Helms"),
            new Movie("comedy_2", "We're the Millers", "A fake family road trip turns into a hilarious disaster.", 90,
                     "https://example.com/weed.jpg", 
                     "https://example.com/weed_trailer.mp4", 
                     "Comedy", 7.0f, "Rawson Marshall Thurber", "Jason Sudeikis, Jennifer Aniston"),
            new Movie("comedy_3", "Ted", "A foul-mouthed teddy bear brings chaos to his best friend's life.", 75,
                     "https://example.com/ted.jpg", 
                     "https://example.com/ted_trailer.mp4", 
                     "Comedy", 7.3f, "Seth MacFarlane", "Mark Wahlberg, Mila Kunis")
        };

        // 剧情片数据
        Movie[] dramaMovies = {
            new Movie("drama_1", "The Shawshank Redemption", "A banker finds hope and friendship while serving a life sentence in prison.", 100,
                     "https://example.com/shawshank.jpg", 
                     "https://example.com/shawshank_trailer.mp4", 
                     "Drama", 9.7f, "Frank Darabont", "Tim Robbins, Morgan Freeman"),
            new Movie("drama_2", "Forrest Gump", "A kind-hearted man with a low IQ lives an extraordinary life.", 110,
                     "https://example.com/forrest.jpg", 
                     "https://example.com/forrest_trailer.mp4", 
                     "Drama", 9.5f, "Robert Zemeckis", "Tom Hanks, Robin Wright"),
            new Movie("drama_3", "The Pursuit of Happyness", "A father keeps fighting for a better life for himself and his son.", 95,
                     "https://example.com/pursuit.jpg", 
                     "https://example.com/pursuit_trailer.mp4", 
                     "Drama", 8.0f, "Gabriele Muccino", "Will Smith, Jaden Smith")
        };

        // 批量上传数据
        uploadMoviesBatch(actionMovies);
        uploadMoviesBatch(comedyMovies);
        uploadMoviesBatch(dramaMovies);
    }

    /**
     * 批量上传电影数据
     * @param movies 电影数组
     */
    private static void uploadMoviesBatch(Movie[] movies) {
        for (Movie movie : movies) {
            Map<String, Object> movieData = new HashMap<>();
            movieData.put("title", movie.getTitle());
            movieData.put("description", movie.getDescription());
            movieData.put("price", movie.getPrice());
            movieData.put("posterUrl", movie.getPosterUrl());
            movieData.put("previewVideoUrl", movie.getPreviewVideoUrl());
            movieData.put("genre", movie.getGenre());
            movieData.put("rating", movie.getRating());
            movieData.put("director", movie.getDirector());
            movieData.put("cast", movie.getCast());

            db.collection(Constants.COLLECTION_MOVIES)
                    .document(movie.getId())
                    .set(movieData)
                    .addOnSuccessListener(aVoid -> {
                        // 成功回调
                    })
                    .addOnFailureListener(e -> {
                        // 失败回调
                    });
        }
    }

    /**
     * 初始化测试用户数据
     */
    public static void initializeTestUsers() {
        // Mary用户数据
        Map<String, Object> maryData = new HashMap<>();
        maryData.put("name", Constants.TEST_USER_MARY_NAME);
        maryData.put("age", 25);
        maryData.put("email", Constants.TEST_USER_MARY_EMAIL);
        maryData.put("credits", Constants.DEFAULT_USER_CREDITS);
        maryData.put("createdAt", Timestamp.now());

        // John用户数据
        Map<String, Object> johnData = new HashMap<>();
        johnData.put("name", Constants.TEST_USER_JOHN_NAME);
        johnData.put("age", 30);
        johnData.put("email", Constants.TEST_USER_JOHN_EMAIL);
        johnData.put("credits", Constants.DEFAULT_USER_CREDITS);
        johnData.put("createdAt", Timestamp.now());

        // 上传用户数据
        db.collection(Constants.COLLECTION_USERS)
                .document("test_user_mary")
                .set(maryData);

        db.collection(Constants.COLLECTION_USERS)
                .document("test_user_john")
                .set(johnData);
    }

    /**
     * 清除所有测试数据
     */
    public static void clearTestData() {
        // 清除电影数据
        db.collection(Constants.COLLECTION_MOVIES)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : task.getResult()) {
                            db.collection(Constants.COLLECTION_MOVIES)
                                    .document(document.getId())
                                    .delete();
                        }
                    }
                });

        // 清除测试用户数据
        db.collection(Constants.COLLECTION_USERS)
                .whereIn("email", Arrays.asList(
                    Constants.TEST_USER_MARY_EMAIL,
                    Constants.TEST_USER_JOHN_EMAIL
                ))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : task.getResult()) {
                            db.collection(Constants.COLLECTION_USERS)
                                    .document(document.getId())
                                    .delete();
                        }
                    }
                });
    }
}
