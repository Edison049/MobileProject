package com.innovationai.myapplication.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.innovationai.myapplication.R;
import com.innovationai.myapplication.adapter.OrderAdapter;
import com.innovationai.myapplication.model.CartItem;
import com.innovationai.myapplication.model.Order;
import com.innovationai.myapplication.model.User;
import com.innovationai.myapplication.util.CartManager;
import com.innovationai.myapplication.util.TempAuthUtil;
import com.innovationai.myapplication.util.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单Activity
 * 显示用户的购买历史和订单信息
 */
public class OrdersActivity extends AppCompatActivity {

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
        String userName = TempAuthUtil.getCurrentUserName(this);
        int credits = TempAuthUtil.getCurrentUserCredits(this);
        
        if (!userName.isEmpty()) {
            currentUser = new User();
            currentUser.setName(userName);
            currentUser.setCredits(credits);
            
            // 显示用户信息
            userInfoText.setText("用户: " + userName + " | 积分余额: " + credits);
        }
    }

    /**
     * 加载订单数据
     */
    private void loadOrdersData() {
        // 创建模拟订单数据（在实际应用中这应该从数据库加载）
        createMockOrders();
        
        // 更新适配器
        if (orderAdapter == null) {
            orderAdapter = new OrderAdapter(this, ordersList);
            ordersRecycler.setAdapter(orderAdapter);
        } else {
            orderAdapter.updateOrders(ordersList);
        }
        
        // 更新UI显示
        updateOrdersDisplay();
    }

    /**
     * 创建模拟订单数据
     */
    private void createMockOrders() {
        ordersList = new ArrayList<>();
        
        // 检查是否有最近的购买记录
        CartManager cartManager = CartManager.getInstance();
        if (!cartManager.isEmpty()) {
            // 如果购物车不为空，创建一个模拟订单
            List<CartItem> cartItems = cartManager.getCartItems();
            int totalAmount = cartManager.getTotalAmount();
            
            Order recentOrder = new Order();
            recentOrder.setOrderId("ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            recentOrder.setUserId(TempAuthUtil.getCurrentUserName(this));
            recentOrder.setMovies(new ArrayList<>(cartItems));
            recentOrder.setTotalAmount(totalAmount);
            recentOrder.setTimestamp(Timestamp.now());
            recentOrder.setStatus("已完成");
            
            ordersList.add(recentOrder);
        }
        
        // 添加一些历史订单示例
        addSampleOrders();
    }

    /**
     * 添加示例订单
     */
    private void addSampleOrders() {
        // 示例订单1
        Order sampleOrder1 = new Order();
        sampleOrder1.setOrderId("ORD001");
        sampleOrder1.setUserId("mary");
        sampleOrder1.setMovies(createSampleCartItems(2));
        sampleOrder1.setTotalAmount(250);
        sampleOrder1.setTimestamp(Timestamp.now());
        sampleOrder1.setStatus("已完成");
        ordersList.add(sampleOrder1);
        
        // 示例订单2
        Order sampleOrder2 = new Order();
        sampleOrder2.setOrderId("ORD002");
        sampleOrder2.setUserId("john");
        sampleOrder2.setMovies(createSampleCartItems(1));
        sampleOrder2.setTotalAmount(120);
        sampleOrder2.setTimestamp(Timestamp.now());
        sampleOrder2.setStatus("已完成");
        ordersList.add(sampleOrder2);
    }

    /**
     * 创建示例购物车项目
     */
    private List<CartItem> createSampleCartItems(int count) {
        List<CartItem> items = new ArrayList<>();
        
        if (count >= 1) {
            // 创建示例电影
            com.innovationai.myapplication.model.Movie movie1 = new com.innovationai.myapplication.model.Movie();
            movie1.setTitle("复仇者联盟4");
            movie1.setPrice(150);
            items.add(new CartItem(movie1, 1));
        }
        
        if (count >= 2) {
            com.innovationai.myapplication.model.Movie movie2 = new com.innovationai.myapplication.model.Movie();
            movie2.setTitle("速度与激情9");
            movie2.setPrice(100);
            items.add(new CartItem(movie2, 1));
        }
        
        return items;
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

    /**
     * 添加新订单（当用户完成购买时调用）
     * @param newOrder 新订单
     */
    public static void addNewOrder(Order newOrder) {
        // 在实际应用中，这里应该将订单保存到数据库
        // 现在只是在内存中保存
        // 这个方法可以被其他Activity调用
    }
}