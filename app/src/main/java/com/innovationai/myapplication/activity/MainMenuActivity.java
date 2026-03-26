package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
// import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.firestore.Query;
// import com.google.firebase.firestore.QueryDocumentSnapshot;
// import com.google.firebase.firestore.QuerySnapshot;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.MovieAdapter;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.FirebaseUtil;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单Activity
 * 展示Netflix风格的电影列表，支持分类浏览和搜索功能
 */
public class MainMenuActivity extends AppCompatActivity {

    // UI组件
    private TextView userNameText;
    private TextView creditsText;
    private EditText searchEditText;
    private RecyclerView actionMoviesRecycler;
    private RecyclerView comedyMoviesRecycler;
    private RecyclerView dramaMoviesRecycler;
    private MaterialButton homeButton;
    private MaterialButton cartButton;
    private MaterialButton ordersButton;
    private MaterialButton profileButton;

    // 适配器
    private MovieAdapter actionMoviesAdapter;
    private MovieAdapter comedyMoviesAdapter;
    private MovieAdapter dramaMoviesAdapter;

    // 数据列表
    private List<Movie> actionMoviesList = new ArrayList<>();
    private List<Movie> comedyMoviesList = new ArrayList<>();
    private List<Movie> dramaMoviesList = new ArrayList<>();
    private List<Movie> allMoviesList = new ArrayList<>(); // 用于搜索

    // 当前用户
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // 初始化视图
        initViews();
        
        // 设置RecyclerView
        setupRecyclerViews();
        
        // 设置点击监听器
        setupClickListeners();
        
        // 加载用户信息
        loadUserInfo();
        
        // 加载电影数据
        loadMoviesData();
        
        // 设置搜索功能
        setupSearchFunctionality();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到主菜单时刷新用户信息和购物车状态
        loadUserInfo();
        updateCartBadge();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        userNameText = findViewById(R.id.user_name_text);
        creditsText = findViewById(R.id.credits_text);
        searchEditText = findViewById(R.id.search_edit_text);
        actionMoviesRecycler = findViewById(R.id.action_movies_recycler);
        comedyMoviesRecycler = findViewById(R.id.comedy_movies_recycler);
        dramaMoviesRecycler = findViewById(R.id.drama_movies_recycler);
        homeButton = findViewById(R.id.home_button);
        cartButton = findViewById(R.id.cart_button);
        ordersButton = findViewById(R.id.orders_button);
        profileButton = findViewById(R.id.profile_button);
    }

    /**
     * 设置RecyclerView
     */
    private void setupRecyclerViews() {
        // 动作片RecyclerView
        actionMoviesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        actionMoviesAdapter = new MovieAdapter(this, actionMoviesList);
        actionMoviesRecycler.setAdapter(actionMoviesAdapter);

        // 喜剧片RecyclerView
        comedyMoviesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        comedyMoviesAdapter = new MovieAdapter(this, comedyMoviesList);
        comedyMoviesRecycler.setAdapter(comedyMoviesAdapter);

        // 剧情片RecyclerView
        dramaMoviesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dramaMoviesAdapter = new MovieAdapter(this, dramaMoviesList);
        dramaMoviesRecycler.setAdapter(dramaMoviesAdapter);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 底部导航按钮
        homeButton.setOnClickListener(v -> {
            // 当前就在首页，无需操作
            Utils.showToast(this, "已在首页");
        });

        cartButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, CartActivity.class);
            startActivity(intent);
        });

        ordersButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 加载用户信息（使用临时认证）
     */
    private void loadUserInfo() {
        // 使用临时认证获取用户信息
        String userName = TempAuthUtil.getCurrentUserName(this);
        int credits = TempAuthUtil.getCurrentUserCredits(this);
        
        if (!userName.isEmpty()) {
            // 创建临时用户对象用于显示
            currentUser = new User();
            currentUser.setName(userName);
            currentUser.setCredits(credits);
            updateUserInfoDisplay();
        }
    }

    /**
     * 更新用户信息显示
     */
    private void updateUserInfoDisplay() {
        if (currentUser != null) {
            userNameText.setText(currentUser.getName());
            creditsText.setText(String.valueOf(currentUser.getCredits()));
        }
    }

    /**
     * 加载电影数据（使用模拟数据）
     */
    private void loadMoviesData() {
        // 使用模拟电影数据
        createMockMovies();
        
        // 更新适配器
        actionMoviesAdapter.updateMovies(actionMoviesList);
        comedyMoviesAdapter.updateMovies(comedyMoviesList);
        dramaMoviesAdapter.updateMovies(dramaMoviesList);
    }
    
    /**
     * 创建模拟电影数据
     */
    private void createMockMovies() {
        allMoviesList.clear();
        actionMoviesList.clear();
        comedyMoviesList.clear();
        dramaMoviesList.clear();
        
        // 动作片 - 使用本地图片
        Movie avengers = new Movie("1", "复仇者联盟 4", "超级英雄们集结对抗灭霸", 150,
            R.drawable.avengers4, "https://example.com/trailer.mp4",
            "Action", 8.5f, "Anthony and Joe Russo", "小罗伯特·唐尼，克里斯·埃文斯");
        actionMoviesList.add(avengers);
        allMoviesList.add(avengers);
                
        Movie fast9 = new Movie("2", "速度与激情 9", "多米尼克和他的家人面临新的威胁", 120,
            R.drawable.fast_and_furious, "https://example.com/trailer.mp4",
            "动作", 7.2f, "林诣彬", "范·迪塞尔，米歇尔·罗德里格兹");
        actionMoviesList.add(fast9);
        allMoviesList.add(fast9);
        
        // 喜剧片
        Movie hangover = new Movie("3", "宿醉", "四个朋友拉斯维加斯狂欢后的疯狂经历", 80,
            "https://example.com/hangover.jpg", "https://example.com/trailer.mp4",
            "喜剧", 7.8f, "托德·菲利普斯", "布莱德利·库珀,艾德·赫尔姆斯");
        comedyMoviesList.add(hangover);
        allMoviesList.add(hangover);
        
        // 剧情片
        Movie shawshank = new Movie("4", "肖申克的救赎", "银行家安迪在监狱中的希望之旅", 100,
            "https://example.com/shawshank.jpg", "https://example.com/trailer.mp4",
            "剧情", 9.7f, "弗兰克·德拉邦特", "蒂姆·罗宾斯,摩根·弗里曼");
        dramaMoviesList.add(shawshank);
        allMoviesList.add(shawshank);
    }

    /**
     * 设置搜索功能
     */
    private void setupSearchFunctionality() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMovies(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 过滤电影（搜索功能）
     * @param query 搜索关键词
     */
    private void filterMovies(String query) {
        if (query.isEmpty()) {
            // 恢复原始数据
            actionMoviesAdapter.updateMovies(actionMoviesList);
            comedyMoviesAdapter.updateMovies(comedyMoviesList);
            dramaMoviesAdapter.updateMovies(dramaMoviesList);
            return;
        }

        // 创建过滤后的列表
        List<Movie> filteredActionMovies = new ArrayList<>();
        List<Movie> filteredComedyMovies = new ArrayList<>();
        List<Movie> filteredDramaMovies = new ArrayList<>();

        for (Movie movie : allMoviesList) {
            if (movieMatchesQuery(movie, query)) {
                String genre = movie.getGenre().toLowerCase();
                if (genre.contains("action") || genre.contains("动作")) {
                    filteredActionMovies.add(movie);
                } else if (genre.contains("comedy") || genre.contains("喜剧")) {
                    filteredComedyMovies.add(movie);
                } else if (genre.contains("drama") || genre.contains("剧情")) {
                    filteredDramaMovies.add(movie);
                }
            }
        }

        // 更新适配器
        actionMoviesAdapter.updateMovies(filteredActionMovies);
        comedyMoviesAdapter.updateMovies(filteredComedyMovies);
        dramaMoviesAdapter.updateMovies(filteredDramaMovies);
    }

    /**
     * 检查电影是否匹配搜索查询
     * @param movie 电影对象
     * @param query 搜索关键词
     * @return true表示匹配，false表示不匹配
     */
    private boolean movieMatchesQuery(Movie movie, String query) {
        String lowerQuery = query.toLowerCase();
        return movie.getTitle().toLowerCase().contains(lowerQuery) ||
               movie.getDirector().toLowerCase().contains(lowerQuery) ||
               movie.getCast().toLowerCase().contains(lowerQuery) ||
               movie.getGenre().toLowerCase().contains(lowerQuery);
    }

    /**
     * 更新购物车徽章
     */
    private void updateCartBadge() {
        int cartItemCount = CartManager.getInstance().getItemCount();
        // TODO: 实现具体的徽章更新逻辑
        if (cartItemCount > 0) {
            // 显示徽章数字
        } else {
            // 隐藏徽章
        }
    }
}