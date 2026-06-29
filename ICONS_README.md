# 图标资源说明

本项目使用Android默认的启动器图标（ic_launcher）。

## 图标位置
`app/src/main/res/mipmap-*/`

## 需要的图标尺寸：
- `mipmap-mdpi`: 48x48 px
- `mipmap-hdpi`: 72x72 px
- `mipmap-xhdpi`: 96x96 px
- `mipmap-xxhdpi`: 144x144 px
- `mipmap-xxxhdpi`: 192x192 px

## 如何添加自定义图标：

### 方法一：使用Android Studio内置工具
1. 右键点击 `res` 目录 → `New` → `Image Asset`
2. 选择 `Launcher Icons (Adaptive and Legacy)`
3. 选择前景层图片（建议1024x1024px）
4. 点击 `Next` → `Finish`

### 方法二：手动替换
1. 准备好各分辨率的PNG图标
2. 分别命名为：
   - `ic_launcher.png`
   - `ic_launcher_round.png`（圆形版本）
3. 放入对应的 `mipmap-*` 文件夹

### 方法三：使用在线生成工具
推荐网站：
- https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
- https://www.easyicon.net/

**注意：** 如果不添加自定义图标，编译时会使用Android默认图标（绿色机器人），不影响功能。
