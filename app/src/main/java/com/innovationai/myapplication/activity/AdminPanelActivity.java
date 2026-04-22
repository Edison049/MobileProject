package com.innovationai.myapplication.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.AdminMovieAdapter;
import com.innovationai.myapplication.adapter.AdminUserAdapter;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.MovieMediaUtil;
import com.innovationai.myapplication.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 管理员后台
 * 支持增删电影与用户
 */
public class AdminPanelActivity extends AppCompatActivity {
    private final AppRepository repository = AppRepository.getInstance();

    private ImageButton backButton;
    private TextView modeText;
    private MaterialButton addMovieButton;
    private MaterialButton addUserButton;
    private RecyclerView moviesRecycler;
    private RecyclerView usersRecycler;

    private AdminMovieAdapter movieAdapter;
    private AdminUserAdapter userAdapter;
    private final List<Movie> movies = new ArrayList<>();
    private final List<User> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        initViews();
        setupRecyclerViews();
        setupListeners();
        verifyAdminAndLoad();
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        modeText = findViewById(R.id.admin_mode_text);
        addMovieButton = findViewById(R.id.add_movie_button);
        addUserButton = findViewById(R.id.add_user_button);
        moviesRecycler = findViewById(R.id.admin_movies_recycler);
        usersRecycler = findViewById(R.id.admin_users_recycler);
    }

    private void setupRecyclerViews() {
        moviesRecycler.setLayoutManager(new LinearLayoutManager(this));
        usersRecycler.setLayoutManager(new LinearLayoutManager(this));

        movieAdapter = new AdminMovieAdapter(movies, this::confirmDeleteMovie);
        userAdapter = new AdminUserAdapter(users, this::confirmDeleteUser);

        moviesRecycler.setAdapter(movieAdapter);
        usersRecycler.setAdapter(userAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        addMovieButton.setOnClickListener(v -> showAddMovieDialog());
        addUserButton.setOnClickListener(v -> showAddUserDialog());
    }

    private void verifyAdminAndLoad() {
        repository.loadCurrentUser(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                if (!data.isAdmin()) {
                    Utils.showToast(AdminPanelActivity.this, "Only administrators can access the admin panel");
                    finish();
                    return;
                }
                modeText.setText("Current data mode: " + repository.getDataModeLabel(AdminPanelActivity.this));
                loadAllData();
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(AdminPanelActivity.this, errorMessage);
                finish();
            }
        });
    }

    private void loadAllData() {
        repository.loadMovies(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Movie> data) {
                movies.clear();
                movies.addAll(data);
                movieAdapter.updateMovies(movies);
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(AdminPanelActivity.this, errorMessage);
            }
        });

        repository.loadUsers(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<User> data) {
                users.clear();
                users.addAll(data);
                userAdapter.updateUsers(users);
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(AdminPanelActivity.this, errorMessage);
            }
        });
    }

    private void showAddMovieDialog() {
        android.view.View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_admin_movie, null, false);

        TextInputEditText titleInput = dialogView.findViewById(R.id.movie_title_input);
        TextInputEditText descriptionInput = dialogView.findViewById(R.id.movie_description_input);
        MaterialAutoCompleteTextView genreInput = dialogView.findViewById(R.id.movie_genre_input);
        TextInputEditText directorInput = dialogView.findViewById(R.id.movie_director_input);
        TextInputEditText castInput = dialogView.findViewById(R.id.movie_cast_input);
        TextInputEditText priceInput = dialogView.findViewById(R.id.movie_price_input);
        TextInputEditText ratingInput = dialogView.findViewById(R.id.movie_rating_input);
        TextInputEditText posterInput = dialogView.findViewById(R.id.movie_poster_input);
        TextInputEditText videoInput = dialogView.findViewById(R.id.movie_video_input);
        ImageView posterPreview = dialogView.findViewById(R.id.movie_poster_preview);
        String[] movieGenres = getResources().getStringArray(R.array.admin_movie_genres);
        genreInput.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, movieGenres));
        genreInput.setKeyListener(null);
        genreInput.setOnClickListener(view -> genreInput.showDropDown());
        genreInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                genreInput.showDropDown();
            }
        });
        loadPosterPreview(posterPreview, MovieMediaUtil.drawableRef("ic_launcher_foreground"));
        posterInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                loadPosterPreview(posterPreview, textOf(posterInput));
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Movie")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String title = textOf(titleInput);
                    String description = textOf(descriptionInput);
                    String genre = textOf(genreInput);
                    String director = textOf(directorInput);
                    String cast = textOf(castInput);
                    String posterUrl = textOf(posterInput);
                    String videoUrl = textOf(videoInput);

                    if (title.isEmpty() || description.isEmpty() || genre.isEmpty()) {
                        Utils.showToast(this, "Please provide at least a title, synopsis, and genre");
                        return;
                    }

                    if (!posterUrl.isEmpty() && !MovieMediaUtil.isRemoteUrl(posterUrl)) {
                        Utils.showToast(this, "Poster URL must start with http or https");
                        return;
                    }

                    int price;
                    float rating;
                    try {
                        price = Integer.parseInt(textOf(priceInput));
                        rating = Float.parseFloat(textOf(ratingInput));
                    } catch (NumberFormatException e) {
                        Utils.showToast(this, "Price and rating must be valid numbers");
                        return;
                    }

                    Movie movie = new Movie();
                    movie.setId("movie_" + UUID.randomUUID().toString().replace("-", ""));
                    movie.setTitle(title);
                    movie.setDescription(description);
                    movie.setGenre(genre);
                    movie.setDirector(director);
                    movie.setCast(cast);
                    movie.setPrice(price);
                    movie.setRating(rating);
                    movie.setPosterUrl(posterUrl.isEmpty()
                            ? MovieMediaUtil.drawableRef("ic_launcher_foreground")
                            : posterUrl);
                    movie.setPreviewVideoUrl(videoUrl.isEmpty()
                            ? MovieMediaUtil.rawRef("seabird1")
                            : videoUrl);

                    repository.addMovie(this, movie, new AppRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            Utils.showToast(AdminPanelActivity.this, "Movie added successfully");
                            loadAllData();
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Utils.showToast(AdminPanelActivity.this, errorMessage);
                        }
                    });
                }));
        dialog.show();
    }

    private void showAddUserDialog() {
        android.view.View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_admin_user, null, false);

        TextInputEditText nameInput = dialogView.findViewById(R.id.user_name_input);
        TextInputEditText ageInput = dialogView.findViewById(R.id.user_age_input);
        TextInputEditText emailInput = dialogView.findViewById(R.id.user_email_input);
        TextInputEditText passwordInput = dialogView.findViewById(R.id.user_password_input);
        TextInputEditText creditsInput = dialogView.findViewById(R.id.user_credits_input);
        CheckBox adminCheckBox = dialogView.findViewById(R.id.user_admin_checkbox);

        new AlertDialog.Builder(this)
                .setTitle("Add User")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = textOf(nameInput);
                    String email = textOf(emailInput);
                    String password = textOf(passwordInput);
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        Utils.showToast(this, "Please fill in the username, email, and password");
                        return;
                    }

                    int age;
                    int credits;
                    try {
                        age = Integer.parseInt(textOf(ageInput));
                        credits = Integer.parseInt(textOf(creditsInput));
                    } catch (NumberFormatException e) {
                        Utils.showToast(this, "Age and credits must be valid numbers");
                        return;
                    }

                    repository.createUser(this, name, age, email, password, credits,
                            adminCheckBox.isChecked() ? Constants.ROLE_ADMIN : Constants.ROLE_USER,
                            false,
                            new AppRepository.DataCallback<>() {
                                @Override
                                public void onSuccess(User data) {
                                    Utils.showToast(AdminPanelActivity.this, "User added successfully");
                                    loadAllData();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Utils.showToast(AdminPanelActivity.this, errorMessage);
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteMovie(Movie movie) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Movie")
                .setMessage("Delete \"" + movie.getTitle() + "\"? It will no longer appear in the movie list.")
                .setPositiveButton("Delete", (dialog, which) -> repository.deleteMovie(this, movie.getId(),
                        new AppRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Utils.showToast(AdminPanelActivity.this, "Movie deleted successfully");
                                loadAllData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Utils.showToast(AdminPanelActivity.this, errorMessage);
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteUser(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Deactivate user " + user.getName() + "? They will no longer be able to sign in.")
                .setPositiveButton("Delete", (dialog, which) -> repository.deleteUser(this, user.getUid(),
                        new AppRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Utils.showToast(AdminPanelActivity.this, "User deleted successfully");
                                loadAllData();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Utils.showToast(AdminPanelActivity.this, errorMessage);
                            }
                        }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String textOf(TextView textView) {
        return textView.getText() == null ? "" : textView.getText().toString().trim();
    }

    private void loadPosterPreview(ImageView previewView, String posterSource) {
        if (previewView == null) {
            return;
        }

        Object loadSource = posterSource;
        if (posterSource != null && posterSource.startsWith(MovieMediaUtil.DRAWABLE_PREFIX)) {
            String resourceName = posterSource.substring(MovieMediaUtil.DRAWABLE_PREFIX.length());
            int resourceId = getResources().getIdentifier(resourceName, "drawable", getPackageName());
            loadSource = resourceId == 0 ? R.drawable.ic_launcher_foreground : resourceId;
        }

        Glide.with(this)
                .load(loadSource)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(previewView);
    }
}
