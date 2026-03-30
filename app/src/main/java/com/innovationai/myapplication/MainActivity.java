package com.innovationai.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.innovationai.myapplication.activity.LoginActivity;
import com.innovationai.myapplication.activity.MainMenuActivity;
import com.innovationai.myapplication.data.AppRepository;

/**
 * 启动Activity
 * 作为应用的入口点，检查用户登录状态并导航到相应页面
 */
public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 启动页显示时间（毫秒）

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 启动页期间初始化数据源，再决定进入登录页还是主菜单
        new Handler().postDelayed(this::initializeAndNavigate, SPLASH_DELAY);
    }

    /**
     * 初始化数据后，根据会话状态导航到相应页面
     */
    private void initializeAndNavigate() {
        AppRepository.getInstance().initialize(this, new AppRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                navigateToNextScreen();
            }

            @Override
            public void onError(String errorMessage) {
                android.widget.Toast.makeText(MainActivity.this,
                        errorMessage,
                        android.widget.Toast.LENGTH_SHORT).show();
                navigateToNextScreen();
            }
        });
    }

    private void navigateToNextScreen() {
        Intent intent = AppRepository.getInstance().isLoggedIn(this)
                ? new Intent(MainActivity.this, MainMenuActivity.class)
                : new Intent(MainActivity.this, LoginActivity.class);

        startActivity(intent);
        finish();
    }
}
