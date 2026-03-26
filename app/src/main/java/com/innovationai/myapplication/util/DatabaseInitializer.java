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
            new Movie("action_1", "复仇者联盟4", "超级英雄们集结对抗灭霸", 150, 
                     "https://example.com/avengers4.jpg", 
                     "https://example.com/avengers4_trailer.mp4", 
                     "动作", 8.5f, "罗素兄弟", "小罗伯特·唐尼,克里斯·埃文斯"),
            new Movie("action_2", "速度与激情9", "多米尼克和他的家人面临新的威胁", 120, 
                     "https://example.com/fast9.jpg", 
                     "https://example.com/fast9_trailer.mp4", 
                     "动作", 7.2f, "林诣彬", "范·迪塞尔,米歇尔·罗德里格兹"),
            new Movie("action_3", "碟中谍7", "伊森·亨特面对最危险的任务", 180, 
                     "https://example.com/mission7.jpg", 
                     "https://example.com/mission7_trailer.mp4", 
                     "动作", 8.1f, "克里斯托夫·迈考利", "汤姆·克鲁斯,丽贝卡·弗格森")
        };

        // 喜剧片数据
        Movie[] comedyMovies = {
            new Movie("comedy_1", "宿醉", "四个朋友拉斯维加斯狂欢后的疯狂经历", 80, 
                     "https://example.com/hangover.jpg", 
                     "https://example.com/hangover_trailer.mp4", 
                     "喜剧", 7.8f, "托德·菲利普斯", "布莱德利·库珀,艾德·赫尔姆斯"),
            new Movie("comedy_2", "冒牌家庭", "一家人假扮成大麻商人的真实故事", 90, 
                     "https://example.com/weed.jpg", 
                     "https://example.com/weed_trailer.mp4", 
                     "喜剧", 7.0f, "罗森·马歇尔·瑟伯", "威尔·法瑞尔,马克·沃尔伯格"),
            new Movie("comedy_3", "泰迪熊", "会说话的泰迪熊带来的搞笑冒险", 75, 
                     "https://example.com/ted.jpg", 
                     "https://example.com/ted_trailer.mp4", 
                     "喜剧", 7.3f, "塞思·麦克法兰", "马克·沃尔伯格,米拉·库尼斯")
        };

        // 剧情片数据
        Movie[] dramaMovies = {
            new Movie("drama_1", "肖申克的救赎", "银行家安迪在监狱中的希望之旅", 100, 
                     "https://example.com/shawshank.jpg", 
                     "https://example.com/shawshank_trailer.mp4", 
                     "剧情", 9.7f, "弗兰克·德拉邦特", "蒂姆·罗宾斯,摩根·弗里曼"),
            new Movie("drama_2", "阿甘正传", "智商只有75的男人的非凡人生", 110, 
                     "https://example.com/forrest.jpg", 
                     "https://example.com/forrest_trailer.mp4", 
                     "剧情", 9.5f, "罗伯特·泽米吉斯", "汤姆·汉克斯,罗宾·怀特"),
            new Movie("drama_3", "当幸福来敲门", "父亲为了梦想坚持不懈的故事", 95, 
                     "https://example.com/pursuit.jpg", 
                     "https://example.com/pursuit_trailer.mp4", 
                     "剧情", 8.0f, "加布里尔·穆奇诺", "威尔·史密斯,贾登·史密斯")
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