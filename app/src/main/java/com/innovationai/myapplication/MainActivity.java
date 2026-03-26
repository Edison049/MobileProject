package com.innovationai.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.innovationai.myapplication.activity.LoginActivity;
import com.innovationai.myapplication.activity.MainMenuActivity;
import com.innovationai.myapplication.util.FirebaseUtil;

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

        // 延迟跳转到下一个页面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextScreen();
            }
        }, SPLASH_DELAY);
    }

    /**
     * 根据用户登录状态导航到相应页面
     */
    private void navigateToNextScreen() {
        try {
            Intent intent;
            
            // 由于Firebase尚未配置，暂时总是跳转到登录页面
            intent = new Intent(MainActivity.this, LoginActivity.class);
            
            startActivity(intent);
            finish(); // 关闭启动Activity
        } catch (Exception e) {
            // 如果出现异常，显示错误信息并跳转到登录页面
            android.widget.Toast.makeText(this, "应用初始化中，请稍后...", android.widget.Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}