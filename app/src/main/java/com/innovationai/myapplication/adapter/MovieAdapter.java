package com.innovationai.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.activity.MovieDetailActivity;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Constants;
import com.innovationai.myapplication.util.Utils;
import java.util.List;

/**
 * 电影列表适配器
 * 用于在RecyclerView中显示电影卡片
 */
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private Context context;
    private List<Movie> movieList;
    private OnMovieClickListener listener;

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
        void onAddToCartClick(Movie movie);
    }

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    public void setOnMovieClickListener(OnMovieClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie_card, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    /**
     * 更新电影列表数据
     * @param newMovies 新的电影列表
     */
    public void updateMovies(List<Movie> newMovies) {
        this.movieList = newMovies;
        notifyDataSetChanged();
    }

    /**
     * 电影ViewHolder内部类
     */
    public class MovieViewHolder extends RecyclerView.ViewHolder {
        private ImageView moviePoster;
        private TextView movieTitle;
        private TextView movieRating;
        private TextView moviePrice;
        private TextView movieYear;
        private FloatingActionButton addToCartFab;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.movie_poster);
            movieTitle = itemView.findViewById(R.id.movie_title);
            movieRating = itemView.findViewById(R.id.movie_rating);
            moviePrice = itemView.findViewById(R.id.movie_price);
            movieYear = itemView.findViewById(R.id.movie_year);
            addToCartFab = itemView.findViewById(R.id.add_to_cart_fab);
        }

        public void bind(Movie movie) {
            // 设置电影标题
            movieTitle.setText(movie.getTitle());
            
            // 设置评分
            movieRating.setText(String.format("%.1f★", movie.getRating()));
            
            // 设置价格
            moviePrice.setText(movie.getPrice() + "积分");
            
            // 设置年份（从发布日期提取）
            movieYear.setText("2023"); // TODO: 从实际数据中提取年份
            
            // 加载电影海报（支持本地和网络图片）
            if (movie.isLocalImage()) {
                loadLocalMoviePoster(movie.getPosterResourceId());
            } else {
                loadMoviePoster(movie.getPosterUrl());
            }
            
            // 设置点击事件
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMovieClick(movie);
                    }
                    
                    // 跳转到电影详情页面
                    Intent intent = new Intent(context, MovieDetailActivity.class);
                    intent.putExtra(Constants.EXTRA_MOVIE_ID, movie.getId());
                    intent.putExtra(Constants.EXTRA_MOVIE_TITLE, movie.getTitle());
                    context.startActivity(intent);
                }
            });
            
            // 设置添加到购物车按钮点击事件
            addToCartFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onAddToCartClick(movie);
                    }
                    
                    // 添加到购物车逻辑
                    CartManager cartManager = CartManager.getInstance();
                    if (cartManager.addToCart(movie)) {
                        Utils.showToast(context, "已添加到购物车: " + movie.getTitle());
                        // 更新购物车徽章数量
                        updateCartBadge();
                    } else {
                        Utils.showToast(context, "该电影已在购物车中");
                    }
                }
            });
        }

        /**
         * 加载电影海报图片（网络图片）
         * @param posterUrl 海报 URL
         */
        private void loadMoviePoster(String posterUrl) {
            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(context)
                        .load(posterUrl)
                        .transform(new CenterCrop(), new RoundedCorners(8))
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(moviePoster);
            } else {
                // 使用默认图片
                moviePoster.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
                
        /**
         * 加载本地电影海报图片
         * @param resourceId 本地资源 ID
         */
        private void loadLocalMoviePoster(int resourceId) {
            if (resourceId != 0) {
                Glide.with(context)
                        .load(resourceId)
                        .transform(new CenterCrop(), new RoundedCorners(8))
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(moviePoster);
            } else {
                // 使用默认图片
                moviePoster.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }

        /**
         * 更新购物车徽章数量
         */
        private void updateCartBadge() {
            // TODO: 实现购物车徽章更新逻辑
            // 这里可以在后续完善时添加具体的徽章更新代码
        }
    }
}