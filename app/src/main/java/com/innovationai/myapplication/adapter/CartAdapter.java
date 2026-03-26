package com.innovationai.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Movie;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.Utils;
import java.util.List;

/**
 * 购物车适配器
 * 用于在RecyclerView中显示购物车项目
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private Context context;
    private List<CartItem> cartItems;
    private OnCartItemClickListener listener;

    public interface OnCartItemClickListener {
        void onRemoveItemClick(CartItem item);
        void onCartUpdated();
    }

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    public void setOnCartItemClickListener(OnCartItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    /**
     * 更新购物车数据
     * @param newItems 新的购物车项目列表
     */
    public void updateCartItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    /**
     * 购物车ViewHolder内部类
     */
    public class CartViewHolder extends RecyclerView.ViewHolder {
        private ImageView moviePoster;
        private TextView movieTitle;
        private TextView moviePrice;
        private TextView movieGenre;
        private ImageButton removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.movie_poster_cart);
            movieTitle = itemView.findViewById(R.id.movie_title_cart);
            moviePrice = itemView.findViewById(R.id.movie_price_cart);
            movieGenre = itemView.findViewById(R.id.movie_genre_cart);
            removeButton = itemView.findViewById(R.id.remove_item_button);
        }

        public void bind(CartItem item) {
            Movie movie = item.getMovie();
            
            // 设置电影标题
            movieTitle.setText(movie.getTitle());
            
            // 设置价格
            moviePrice.setText(movie.getPrice() + "积分");
            
            // 设置类型
            movieGenre.setText(movie.getGenre());
            
            // 加载电影海报（支持本地和网络图片）
            if (movie.isLocalImage()) {
                loadLocalMoviePoster(movie.getPosterResourceId());
            } else {
                loadMoviePoster(movie.getPosterUrl());
            }
            
            // 设置删除按钮点击事件
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 从购物车中移除项目
                    CartManager.getInstance().removeFromCart(movie.getId());
                    
                    // 更新UI
                    cartItems.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                    
                    // 通知监听器
                    if (listener != null) {
                        listener.onRemoveItemClick(item);
                        listener.onCartUpdated();
                    }
                    
                    Utils.showToast(context, "已从购物车移除: " + movie.getTitle());
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
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(moviePoster);
            } else {
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
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(moviePoster);
            } else {
                moviePoster.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }
    }
}