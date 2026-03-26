package com.innovationai.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Order;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 订单适配器
 * 用于在RecyclerView中显示订单历史
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private Context context;
    private List<Order> orders;

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    /**
     * 更新订单数据
     * @param newOrders 新的订单列表
     */
    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    /**
     * 订单ViewHolder内部类
     */
    public class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView orderIdText;
        private TextView orderDateText;
        private TextView orderTotalText;
        private TextView orderStatusText;
        private RecyclerView orderItemsRecycler;
        private OrderItemsAdapter itemsAdapter;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            orderDateText = itemView.findViewById(R.id.order_date_text);
            orderTotalText = itemView.findViewById(R.id.order_total_text);
            orderStatusText = itemView.findViewById(R.id.order_status_text);
            orderItemsRecycler = itemView.findViewById(R.id.order_items_recycler);
            
            // 设置嵌套的RecyclerView
            orderItemsRecycler.setLayoutManager(new LinearLayoutManager(context));
        }

        public void bind(Order order) {
            // 设置订单ID
            orderIdText.setText("订单号: " + order.getOrderId());
            
            // 设置订单日期
            if (order.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                orderDateText.setText("下单时间: " + sdf.format(order.getTimestamp().toDate()));
            } else {
                orderDateText.setText("下单时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
            }
            
            // 设置总金额
            orderTotalText.setText("总金额: " + order.getTotalAmount() + "积分");
            
            // 设置状态
            orderStatusText.setText("状态: " + order.getStatus());
            
            // 设置订单项目
            if (itemsAdapter == null) {
                itemsAdapter = new OrderItemsAdapter(context, order.getMovies());
                orderItemsRecycler.setAdapter(itemsAdapter);
            } else {
                itemsAdapter.updateItems(order.getMovies());
            }
        }
    }

    /**
     * 订单项目适配器（内部RecyclerView）
     */
    private static class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ItemViewHolder> {
        private Context context;
        private List<CartItem> items;

        public OrderItemsAdapter(Context context, List<CartItem> items) {
            this.context = context;
            this.items = items;
        }

        public void updateItems(List<CartItem> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_order_movie, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            CartItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        static class ItemViewHolder extends RecyclerView.ViewHolder {
            private TextView movieTitle;
            private TextView moviePrice;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                movieTitle = itemView.findViewById(R.id.order_movie_title);
                moviePrice = itemView.findViewById(R.id.order_movie_price);
            }

            public void bind(CartItem item) {
                movieTitle.setText(item.getMovie().getTitle());
                moviePrice.setText(item.getMovie().getPrice() + "积分");
            }
        }
    }
}