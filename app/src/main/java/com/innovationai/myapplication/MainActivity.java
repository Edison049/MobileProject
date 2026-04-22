package com.innovationai.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.innovationai.myapplication.activity.LoginActivity;
import com.innovationai.myapplication.activity.MainMenuActivity;
import com.innovationai.myapplication.data.AppRepository;
import com.innovationai.myapplication.util.FirebaseUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动Activity
 * 作为应用的入口点，检查用户登录状态并导航到相应页面
 */
public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 启动页显示时间（毫秒）
    private static final int NAVIGATION_FALLBACK_DELAY = 7000; // 初始化异常时的兜底跳转时间

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean hasNavigated = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 启动页期间初始化数据源，再决定进入登录页还是主菜单
        mainHandler.postDelayed(this::initializeAndNavigate, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 初始化数据后，根据会话状态导航到相应页面
     */
    private void initializeAndNavigate() {
        mainHandler.postDelayed(this::navigateToNextScreenSafely, NAVIGATION_FALLBACK_DELAY);
        AppRepository repository = AppRepository.getInstance();
        repository.initialize(this, new AppRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                if (FirebaseUtil.isFirebaseConfigured(MainActivity.this) && !repository.isUsingFirebase(MainActivity.this)) {
                    android.widget.Toast.makeText(
                            MainActivity.this,
                            repository.getFirebaseStatusMessage(MainActivity.this),
                            android.widget.Toast.LENGTH_LONG
                    ).show();
                }
                navigateToNextScreenSafely();
            }

            @Override
            public void onError(String errorMessage) {
                android.widget.Toast.makeText(MainActivity.this,
                        errorMessage,
                        android.widget.Toast.LENGTH_SHORT).show();
                navigateToNextScreenSafely();
            }
        });
    }

    private void navigateToNextScreenSafely() {
        if (!hasNavigated.compareAndSet(false, true)) {
            return;
        }

        mainHandler.removeCallbacksAndMessages(null);
        Intent intent = AppRepository.getInstance().isLoggedIn(this)
                ? new Intent(MainActivity.this, MainMenuActivity.class)
                : new Intent(MainActivity.this, LoginActivity.class);

        startActivity(intent);
        finish();
    }
}
