package com.kuaishou.autoswipe;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnStartService;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartService = findViewById(R.id.btn_start_service);
        tvStatus = findViewById(R.id.tv_status);

        // 检查并请求悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
        }

        // 检查无障碍服务状态
        updateAccessibilityStatus();

        // 开始服务按钮点击事件
        btnStartService.setOnClickListener(v -> {
            if (isAccessibilityServiceEnabled()) {
                startFloatingWindow();
            } else {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show();
                openAccessibilitySettings();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAccessibilityStatus();
    }

    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AutoSwipeService.class.getCanonicalName();
        try {
            int enabled = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabled != 0 && service.contains(service.split("/")[0]);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    /**
     * 更新无障碍服务状态显示
     */
    private void updateAccessibilityStatus() {
        if (isAccessibilityServiceEnabled()) {
            tvStatus.setText("无障碍服务：已开启");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnStartService.setEnabled(true);
        } else {
            tvStatus.setText("无障碍服务：未开启");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnStartService.setEnabled(false);
        }
    }

    /**
     * 打开无障碍设置页面
     */
    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    /**
     * 启动悬浮窗服务
     */
    private void startFloatingWindow() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        // 移到后台，让用户去操作快手极速版
        moveTaskToBack(true);
        Toast.makeText(this, "已启动，请打开快手极速版", Toast.LENGTH_SHORT).show();
    }
}
