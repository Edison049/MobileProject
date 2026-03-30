package com.innovationai.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.Movie;

import java.util.List;

/**
 * 管理员电影列表适配器
 */
public class AdminMovieAdapter extends RecyclerView.Adapter<AdminMovieAdapter.MovieViewHolder> {
    public interface OnMovieActionListener {
        void onDeleteMovie(Movie movie);
    }

    private List<Movie> movies;
    private final OnMovieActionListener listener;

    public AdminMovieAdapter(List<Movie> movies, OnMovieActionListener listener) {
        this.movies = movies;
        this.listener = listener;
    }

    public void updateMovies(List<Movie> newMovies) {
        this.movies = newMovies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        holder.bind(movies.get(position));
    }

    @Override
    public int getItemCount() {
        return movies == null ? 0 : movies.size();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView subtitleText;
        private final MaterialButton deleteButton;

        MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.admin_movie_title);
            subtitleText = itemView.findViewById(R.id.admin_movie_subtitle);
            deleteButton = itemView.findViewById(R.id.admin_delete_movie_button);
        }

        void bind(Movie movie) {
            titleText.setText(movie.getTitle());
            subtitleText.setText(movie.getGenre() + " | " + movie.getPrice() + "积分");
            deleteButton.setOnClickListener(v -> listener.onDeleteMovie(movie));
        }
    }
}
