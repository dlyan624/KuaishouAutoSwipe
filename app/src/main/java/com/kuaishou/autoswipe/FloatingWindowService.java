package com.kuaishou.autoswipe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

/**
 * 悬浮窗控制面板服务
 * 提供开始/停止按钮和运行状态显示
 */
public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "kuaishou_auto_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "FloatingWindowService";

    private WindowManager windowManager;
    private View floatingView;
    private Button btnToggle;
    private TextView tvStatus;
    private boolean isShowing = true;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 创建前台通知
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 创建悬浮窗
        createFloatingWindow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "快手自动刷视频",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持后台运行");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 创建前台通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("快手极速版自动刷视频")
                .setContentText("正在后台运行，点击返回主界面")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    /**
     * 创建悬浮窗
     */
    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 加载悬浮窗布局
        floatingView = View.inflate(this, R.layout.floating_window, null);
        
        // 设置布局参数
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;
        
        // 添加悬浮窗
        windowManager.addView(floatingView, params);
        
        // 初始化控件
        btnToggle = floatingView.findViewById(R.id.btn_toggle);
        tvStatus = floatingView.findViewById(R.id.tv_floating_status);
        
        // 初始状态
        updateStatus(false);
        
        // 开始/停止按钮事件
        btnToggle.setOnClickListener(v -> toggleAutoSwipe());
        
        // 拖动功能
        setupDragFunction(params);
    }

    /**
     * 设置拖动功能
     */
    private void setupDragFunction(WindowManager.LayoutParams params) {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private boolean isMoving = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isMoving = false;
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;
                        
                        // 判断是拖动还是点击（移动超过10px才算拖动）
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoving = true;
                            params.x = initialX + (int) dx;
                            params.y = initialY + (int) dy;
                            windowManager.updateViewLayout(floatingView, params);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        if (!isMoving) {
                            // 是点击事件，不处理（由OnClickListener处理）
                        }
                        return false;
                }
                return false;
            }
        });
    }

    /**
     * 切换自动滑动状态
     */
    private void toggleAutoSwipe() {
        AutoSwipeService service = AutoSwipeService.getInstance();
        if (service != null) {
            if (service.isRunning()) {
                service.stopAutoSwipe();
                updateStatus(false);
            } else {
                service.startAutoSwipe();
                updateStatus(true);
            }
        }
    }

    /**
     * 更新状态显示
     */
    private void updateStatus(boolean isRunning) {
        if (isRunning) {
            btnToggle.setText("停止");
            btnToggle.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            tvStatus.setText("运行中...");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            
            // 定时更新统计信息
            updateStatisticsPeriodically();
        } else {
            btnToggle.setText("开始");
            btnToggle.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            tvStatus.setText("已停止");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    /**
     * 定时更新统计信息
     */
    private void updateStatisticsPeriodically() {
        // 使用Handler定时更新
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AutoSwipeService service = AutoSwipeService.getInstance();
                if (service != null && service.isRunning()) {
                    tvStatus.setText(service.getStatistics());
                    handler.postDelayed(this, 1000); // 每秒更新
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 移除悬浮窗
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        
        // 停止自动滑动
        AutoSwipeService service = AutoSwipeService.getInstance();
        if (service != null) {
            service.stopAutoSwipe();
        }
    }
}
