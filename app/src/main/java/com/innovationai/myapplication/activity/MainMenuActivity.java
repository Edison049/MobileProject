package com.innovationai.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.MovieAdapter;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单Activity
 * 展示Netflix风格的电影列表，支持分类浏览和搜索功能
 */
public class MainMenuActivity extends AppCompatActivity {
    private final AppRepository repository = AppRepository.getInstance();

    // UI组件
    private TextView userNameText;
    private TextView creditsText;
    private EditText searchEditText;
    private View actionMoviesSection;
    private View comedyMoviesSection;
    private View dramaMoviesSection;
    private View otherMoviesSection;
    private RecyclerView actionMoviesRecycler;
    private RecyclerView comedyMoviesRecycler;
    private RecyclerView dramaMoviesRecycler;
    private RecyclerView otherMoviesRecycler;
    private MaterialButton homeButton;
    private MaterialButton cartButton;
    private MaterialButton ordersButton;
    private MaterialButton profileButton;

    // 适配器
    private MovieAdapter actionMoviesAdapter;
    private MovieAdapter comedyMoviesAdapter;
    private MovieAdapter dramaMoviesAdapter;
    private MovieAdapter otherMoviesAdapter;

    // 数据列表
    private List<Movie> actionMoviesList = new ArrayList<>();
    private List<Movie> comedyMoviesList = new ArrayList<>();
    private List<Movie> dramaMoviesList = new ArrayList<>();
    private List<Movie> otherMoviesList = new ArrayList<>();
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
        actionMoviesSection = findViewById(R.id.action_movies_section);
        comedyMoviesSection = findViewById(R.id.comedy_movies_section);
        dramaMoviesSection = findViewById(R.id.drama_movies_section);
        otherMoviesSection = findViewById(R.id.other_movies_section);
        actionMoviesRecycler = findViewById(R.id.action_movies_recycler);
        comedyMoviesRecycler = findViewById(R.id.comedy_movies_recycler);
        dramaMoviesRecycler = findViewById(R.id.drama_movies_recycler);
        otherMoviesRecycler = findViewById(R.id.other_movies_recycler);
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

        // 其他类型RecyclerView
        otherMoviesRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        otherMoviesAdapter = new MovieAdapter(this, otherMoviesList);
        otherMoviesRecycler.setAdapter(otherMoviesAdapter);
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // 底部导航按钮
        homeButton.setOnClickListener(v -> {
            // 当前就在首页，无需操作
            Utils.showToast(this, "You are already on the Home screen");
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
     * 加载当前用户信息
     */
    private void loadUserInfo() {
        repository.loadCurrentUser(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                currentUser = data;
                updateUserInfoDisplay();
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(MainMenuActivity.this, errorMessage);
                navigateToLogin();
            }
        });
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
     * 加载电影数据
     */
    private void loadMoviesData() {
        repository.loadMovies(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Movie> data) {
                allMoviesList.clear();
                actionMoviesList.clear();
                comedyMoviesList.clear();
                dramaMoviesList.clear();
                otherMoviesList.clear();

                for (Movie movie : data) {
                    allMoviesList.add(movie);
                    addMovieToCategoryList(movie, actionMoviesList, comedyMoviesList,
                            dramaMoviesList, otherMoviesList);
                }

                sortMovies(actionMoviesList);
                sortMovies(comedyMoviesList);
                sortMovies(dramaMoviesList);
                sortMovies(otherMoviesList);
                sortMovies(allMoviesList);
                updateMovieSections(actionMoviesList, comedyMoviesList, dramaMoviesList, otherMoviesList);
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(MainMenuActivity.this, errorMessage);
            }
        });
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
            updateMovieSections(actionMoviesList, comedyMoviesList, dramaMoviesList, otherMoviesList);
            return;
        }

        // 创建过滤后的列表
        List<Movie> filteredActionMovies = new ArrayList<>();
        List<Movie> filteredComedyMovies = new ArrayList<>();
        List<Movie> filteredDramaMovies = new ArrayList<>();
        List<Movie> filteredOtherMovies = new ArrayList<>();

        for (Movie movie : allMoviesList) {
            if (movieMatchesQuery(movie, query)) {
                addMovieToCategoryList(movie, filteredActionMovies, filteredComedyMovies,
                        filteredDramaMovies, filteredOtherMovies);
            }
        }

        sortMovies(filteredActionMovies);
        sortMovies(filteredComedyMovies);
        sortMovies(filteredDramaMovies);
        sortMovies(filteredOtherMovies);
        updateMovieSections(filteredActionMovies, filteredComedyMovies,
                filteredDramaMovies, filteredOtherMovies);
    }

    /**
     * 检查电影是否匹配搜索查询
     * @param movie 电影对象
     * @param query 搜索关键词
     * @return true表示匹配，false表示不匹配
     */
    private boolean movieMatchesQuery(Movie movie, String query) {
        String lowerQuery = query.toLowerCase();
        return safeText(movie.getTitle()).toLowerCase().contains(lowerQuery) ||
               safeText(movie.getDirector()).toLowerCase().contains(lowerQuery) ||
               safeText(movie.getCast()).toLowerCase().contains(lowerQuery) ||
               safeText(movie.getGenre()).toLowerCase().contains(lowerQuery);
    }

    private void addMovieToCategoryList(Movie movie, List<Movie> actionMovies,
                                        List<Movie> comedyMovies, List<Movie> dramaMovies,
                                        List<Movie> otherMovies) {
        String genre = safeText(movie.getGenre()).toLowerCase();
        if (genre.contains("action") || genre.contains("动作")) {
            actionMovies.add(movie);
        } else if (genre.contains("comedy") || genre.contains("喜剧")) {
            comedyMovies.add(movie);
        } else if (genre.contains("drama") || genre.contains("剧情")) {
            dramaMovies.add(movie);
        } else {
            otherMovies.add(movie);
        }
    }

    private void sortMovies(List<Movie> movies) {
        movies.sort((first, second) -> {
            int ratingComparison = Float.compare(second.getRating(), first.getRating());
            if (ratingComparison != 0) {
                return ratingComparison;
            }
            return safeText(first.getTitle()).compareToIgnoreCase(safeText(second.getTitle()));
        });
    }

    private void updateMovieSections(List<Movie> actionMovies, List<Movie> comedyMovies,
                                     List<Movie> dramaMovies, List<Movie> otherMovies) {
        actionMoviesAdapter.updateMovies(actionMovies);
        comedyMoviesAdapter.updateMovies(comedyMovies);
        dramaMoviesAdapter.updateMovies(dramaMovies);
        otherMoviesAdapter.updateMovies(otherMovies);

        updateSectionVisibility(actionMoviesSection, actionMovies);
        updateSectionVisibility(comedyMoviesSection, comedyMovies);
        updateSectionVisibility(dramaMoviesSection, dramaMovies);
        updateSectionVisibility(otherMoviesSection, otherMovies);
    }

    private void updateSectionVisibility(View section, List<Movie> movies) {
        section.setVisibility(movies.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
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

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
