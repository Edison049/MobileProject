package com.innovationai.myapplication.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.OrderAdapter;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.Utils;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单Activity
 * 显示用户的购买历史和订单信息
 */
public class OrdersActivity extends AppCompatActivity {
    private final AppRepository repository = AppRepository.getInstance();

    // UI组件
    private ImageButton backButton;
    private RecyclerView ordersRecycler;
    private LinearLayout emptyOrdersLayout;
    private TextView userInfoText;

    // 适配器和数据
    private OrderAdapter orderAdapter;
    private List<Order> ordersList;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        // 初始化视图
        initViews();
        
        // 设置点击监听器
        setupClickListeners();
        
        // 加载用户信息
        loadUserInfo();
        
        // 加载订单数据
        loadOrdersData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        ordersRecycler = findViewById(R.id.orders_recycler);
        emptyOrdersLayout = findViewById(R.id.empty_orders_layout);
        userInfoText = findViewById(R.id.user_info_text);

        // 设置RecyclerView
        ordersRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        repository.loadCurrentUser(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(User data) {
                currentUser = data;
                userInfoText.setText("User: " + data.getName() + " | Credits: " + data.getCredits());
            }

            @Override
            public void onError(String errorMessage) {
                Utils.showToast(OrdersActivity.this, errorMessage);
                userInfoText.setText("Failed to load user information");
            }
        });
    }

    /**
     * 加载订单数据
     */
    private void loadOrdersData() {
        repository.loadOrders(this, new AppRepository.DataCallback<>() {
            @Override
            public void onSuccess(List<Order> data) {
                ordersList = new ArrayList<>(data);

                if (orderAdapter == null) {
                    orderAdapter = new OrderAdapter(OrdersActivity.this, ordersList);
                    ordersRecycler.setAdapter(orderAdapter);
                } else {
                    orderAdapter.updateOrders(ordersList);
                }

                updateOrdersDisplay();
            }

            @Override
            public void onError(String errorMessage) {
                ordersList = new ArrayList<>();
                updateOrdersDisplay();
                Utils.showToast(OrdersActivity.this, errorMessage);
            }
        });
    }

    /**
     * 更新订单显示
     */
    private void updateOrdersDisplay() {
        if (ordersList.isEmpty()) {
            // 没有订单
            emptyOrdersLayout.setVisibility(View.VISIBLE);
            ordersRecycler.setVisibility(View.GONE);
        } else {
            // 有订单
            emptyOrdersLayout.setVisibility(View.GONE);
            ordersRecycler.setVisibility(View.VISIBLE);
        }
    }
}
