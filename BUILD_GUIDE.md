# 快速编译指南

## 一键编译脚本（Windows）

将以下内容保存为 `build_apk.bat` 并放在 AndroidProject 目录下运行：

```batch
@echo off
echo ========================================
echo   快手极速版自动刷视频 - APK编译工具
echo ========================================
echo.

:: 检查Android Studio是否安装
if not exist "%ANDROID_HOME%" (
    echo [错误] 未检测到Android SDK
    echo 请安装Android Studio并配置ANDROID_HOME环境变量
    pause
    exit /b 1
)

echo [1/3] 清理旧版本...
call gradlew clean

echo.
echo [2/3] 编译Debug APK...
call gradlew assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 编译失败！请检查错误信息
    pause
    exit /b 1
)

echo.
echo [3/3] 编译成功！
echo ========================================
echo APK输出位置:
echo app\build\outputs\apk\debug\app-debug.apk
echo ========================================
echo.
echo 请将APK传输到手机并安装...
pause
```

## 手动编译步骤

### 前置条件检查清单：
- [ ] 已安装 Android Studio (2023.1.1+)
- [ ] 已安装 Android SDK (API 24-34)
- [ ] JDK 8+ 已配置
- [ ] 至少8GB可用内存

### 详细步骤：

#### Step 1: 打开项目
```
1. 启动 Android Studio
2. File → Open
3. 选择: D:\快手短视频自动化\AndroidProject
4. 等待Gradle同步（首次5-10分钟）
```

#### Step 2: 配置设备
```
方式A - 真机:
  1. USB连接手机
  2. 手机开启USB调试
  3. 在手机上允许USB调试授权

方式B - 模拟器:
  1. Tools → Device Manager
  2. 创建虚拟设备（推荐Pixel 4, API 30）
  3. 启动模拟器
```

#### Step 3: 编译运行
```
方式A - 直接运行到设备:
  1. 点击顶部绿色三角形按钮 (Run)
  2. 或按 Shift+F10
  
方式B - 仅编译APK:
  1. Build → Build Bundle(s) / APK(s) → Build APK(s)
  2. 等待编译完成
  3. 点击通知中的 "locate" 查看APK位置
```

#### Step 4: 安装到手机
```
1. 找到APK文件: app/build/outputs/apk/debug/app-debug.apk
2. 复制到手机
3. 在手机上打开APK文件
4. 允许"安装未知来源应用"
5. 完成安装
```

## 常见编译问题解决

### 问题1: Gradle同步失败
**原因:** 网络问题或Gradle版本不兼容  
**解决:** 
```bash
# 使用国内镜像（在settings.gradle中添加）
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/public' }
}
```

### 问题2: SDK未找到
**解决:**
1. File → Project Structure → SDK Location
2. 设置正确的Android SDK路径
3. 或设置环境变量: ANDROID_HOME=C:\Users\xxx\AppData\Local\Android\Sdk

### 问题3: 编译错误: R类找不到
**解决:**
```bash
Build → Clean Project
Build → Rebuild Project
File → Invalidate Caches → Just Restart
```

### 问题4: 签名错误（Release版本）
**解决:**
1. Build → Generate Signed Bundle/APK
2. 创建新的密钥库(keystore)
3. 记住密码和别名！

## Release版本签名指南

首次发布必须签名，步骤如下：

### 创建密钥库：
```bash
keytool -genkey -v -keystore kuaishou.keystore \
    -alias kuaishou -keyalg RSA -keysize 2048 \
    -validity 10000
```

### 在Android Studio中配置：
1. Build → Generate Signed Bundle/APK → APK → Next
2. Keystore path: 选择刚创建的 .keystore 文件
3. Passwords: 输入设置的密码
4. Key alias: 输入 kuaishou
5. Key password: 输入密钥密码
6. Build Variant: release
7. Signature Versions: 勾选 V1 和 V2
8. Finish

### 输出位置：
`app/build/outputs/apk/release/app-release.apk`

---

**预计编译时间：**
- 首次编译（含依赖下载）：10-20分钟
- 后续增量编译：1-3分钟

**APK大小预估：**
- Debug版本：约 3-5 MB
- Release版本（混淆后）：约 2-3 MB
