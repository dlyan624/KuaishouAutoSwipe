package com.kuaishou.autoswipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Random;

/**
 * 快手极速版自动滑动无障碍服务
 * 核心功能：
 * 1. 自动检测快手极速版界面
 * 2. 执行人性化滑动操作
 * 3. 防检测机制（随机间隔、位置抖动、非线性速度、偶尔回看）
 */
public class AutoSwipeService extends AccessibilityService {

    private static final String TAG = "AutoSwipeService";
    private static AutoSwipeService instance;  // 单例引用
    
    // 配置参数
    private static final String TARGET_PACKAGE = "com.kuaishou.nebula";
    private static final int SWIPE_INTERVAL_MIN = 3000;   // 最小间隔3秒
    private static final int SWIPE_INTERVAL_MAX = 15000;  // 最大间隔15秒
    private static final int POSITION_JITTER = 150;       // 位置抖动±150px
    private static final double REWIND_PROBABILITY = 0.08; // 回看概率8%
    private static final int MAX_RUNTIME_MINUTES = 120;    // 最大运行时间
    
    // 运行状态
    private boolean isRunning = false;
    private Handler handler;
    private Runnable swipeRunnable;
    private Random random;
    private long startTime;
    private int swipeCount = 0;
    private int rewindCount = 0;
    
    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;  // 保存实例引用
        Log.d(TAG, "无障碍服务已连接");
        
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        
        // 获取屏幕尺寸
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        Log.d(TAG, "屏幕尺寸: " + screenWidth + "x" + screenHeight);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 监听窗口变化，确保在快手极速版界面时才执行滑动
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence packageName = event.getPackageName();
            if (packageName != null && TARGET_PACKAGE.equals(packageName.toString())) {
                Log.d(TAG, "检测到快手极速版界面");
                
                // 如果正在运行且没有在执行滑动，开始滑动循环
                if (!isRunning) {
                    startAutoSwipe();
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "无障碍服务被中断");
        stopAutoSwipe();
    }

    /**
     * 开始自动滑动
     */
    public void startAutoSwipe() {
        if (isRunning) return;
        
        isRunning = true;
        startTime = System.currentTimeMillis();
        swipeCount = 0;
        rewindCount = 0;
        
        Log.d(TAG, "开始自动滑动");
        
        swipeRunnable = () -> {
            if (!isRunning) return;
            
            // 检查是否超过最大运行时间
            long elapsedMinutes = (System.currentTimeMillis() - startTime) / (1000 * 60);
            if (elapsedMinutes >= MAX_RUNTIME_MINUTES) {
                Log.d(TAG, "达到最大运行时间，停止");
                stopAutoSwipe();
                return;
            }
            
            // 执行滑动
            performSwipeAction();
            
            // 计算下次滑动的随机延迟
            int delay = calculateRandomDelay();
            handler.postDelayed(swipeRunnable, delay);
        };
        
        // 立即执行第一次滑动
        handler.post(swipeRunnable);
    }

    /**
     * 停止自动滑动
     */
    public void stopAutoSwipe() {
        isRunning = false;
        if (handler != null && swipeRunnable != null) {
            handler.removeCallbacks(swipeRunnable);
        }
        Log.d(TAG, "停止自动滑动");
    }

    /**
     * 执行滑动动作
     */
    private void performSwipeAction() {
        // 根据概率决定是下一个还是回看上一个视频
        if (random.nextDouble() < REWIND_PROBABILITY) {
            // 回看上一个视频（向下滑）
            performSwipe(false);
            rewindCount++;
            Log.d(TAG, "执行回看操作");
            
            // 回看后额外等待2-5秒
            try {
                Thread.sleep(random.nextInt(3000) + 2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // 滑动到下一个视频（向上滑）
            performSwipe(true);
            swipeCount++;
        }
        
        Log.d(TAG, String.format("第%d次滑动 | 已运行%.1f分钟 | 回看%d次",
                swipeCount,
                (System.currentTimeMillis() - startTime) / 60000.0,
                rewindCount));
    }

    /**
     * 执行具体的滑动操作
     * @param isNext true=下一个视频(上滑), false=上一个视频(下滑)
     */
    private void performSwipe(boolean isNext) {
        // 计算基准坐标
        int baseStartX = screenWidth / 2;
        int baseEndX = screenWidth / 2;
        
        int baseStartY, baseEndY;
        if (isNext) {
            // 上滑：从70%高度滑到30%高度
            baseStartY = (int)(screenHeight * 0.7);
            baseEndY = (int)(screenHeight * 0.3);
        } else {
            // 下滑：从30%高度滑到70%高度
            baseStartY = (int)(screenHeight * 0.3);
            baseEndY = (int)(screenHeight * 0.7);
        }
        
        // 加入位置随机抖动
        int startX = clamp(baseStartX + randomInRange(-POSITION_JITTER, POSITION_JITTER), 0, screenWidth);
        int startY = clamp(baseStartY + randomInRange(-POSITION_JITTER, POSITION_JITTER), 0, screenHeight);
        int endX = clamp(baseEndX + randomInRange(-POSITION_JITTER, POSITION_JITTER), 0, screenWidth);
        int endY = clamp(baseEndY + randomInRange(-POSITION_JITTER, POSITION_JITTER), 0, screenHeight);
        
        // 计算滑动距离和持续时间（非线性速度）
        int distance = Math.abs(endY - startY);
        int duration = calculateSwipeDuration(distance);
        
        // 创建手势路径
        Path path = new Path();
        path.moveTo(startX, startY);
        
        // 使用贝塞尔曲线模拟非线性速度（先慢后快再慢）
        int midX = (startX + endX) / 2 + randomInRange(-50, 50);
        int midY = (startY + endY) / 2;
        path.quadTo(midX, midY, endX, endY);
        
        // 构建手势描述
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription.Builder()
                .setDuration(duration)
                .setPath(path)
                .build();
        
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        
        // 执行手势
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "滑动完成");
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "滑动取消");
            }
        }, null);
    }

    /**
     * 计算随机延迟时间（使用正态分布，更接近真人行为）
     */
    private int calculateRandomDelay() {
        double center = (SWIPE_INTERVAL_MIN + SWIPE_INTERVAL_MAX) / 2.0;
        double stdDev = (SWIPE_INTERVAL_MAX - SWIPE_INTERVAL_MIN) / 4.0;
        
        // 高斯分布
        double delay = random.nextGaussian() * stdDev + center;
        
        // 限制在合理范围内
        return (int) Math.max(SWIPE_INTERVAL_MIN, Math.min(SWIPE_INTERVAL_MAX, delay));
    }

    /**
     * 根据距离计算滑动持续时间（非线性速度）
     */
    private int calculateSwipeDuration(int distance) {
        // 基础时间250-350ms
        int baseDuration = random.nextInt(101) + 250;
        
        // 距离因子（长距离稍慢）
        double distanceFactor = Math.min(distance / 1000.0, 0.5);
        int duration = (int)(baseDuration * (1 + distanceFactor));
        
        // 随机波动±50ms
        duration += random.nextInt(101) - 50;
        
        return duration;
    }

    /**
     * 生成指定范围内的随机数
     */
    private int randomInRange(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * 将值限制在指定范围内
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 获取运行统计信息
     */
    public String getStatistics() {
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        return String.format(
                "总滑动次数: %d\n" +
                "总回看次数: %d\n" +
                "运行时间: %.1f 分钟",
                swipeCount,
                rewindCount,
                elapsedSeconds / 60.0
        );
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 获取服务实例（供其他组件调用）
     */
    public static AutoSwipeService getInstance() {
        return instance;
    }
}
