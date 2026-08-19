# RestartHelper 应用重启助手

一个轻量级 Android 工具，用于快速杀掉并重启指定应用。

## 功能

- 选择目标应用后，一键杀掉并重启
- 支持外部调用（语音助手 / 自动化脚本）
- 记住上次选择的应用，下次直接使用

## 使用方法

1. 安装并打开 App
2. 首次打开会弹出应用列表，选择要重启的目标 App
3. 之后每次打开 App 会显示当前目标，点击「重启目标应用」即可
4. 点击「重新选择应用」可更换目标

## 外部调用

通过 adb 或其他自动化工具发送广播即可触发重启：

```bash
am start -a com.fyt.restarthelper.ACTION_RUN
```

## 编译方法

### 方式一：Android Studio

1. 用 Android Studio 打开本项目
2. 等待 Gradle 同步完成
3. 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)

### 方式二：命令行

```bash
# Debug 版
./gradlew assembleDebug

# Release 版（未签名）
./gradlew assembleRelease
```

APK 输出路径：
- Debug: `app/build/outputs/apk/debug/`
- Release: `app/build/outputs/apk/release/`

### 方式三：GitHub Actions 自动构建

将代码推送到 GitHub 仓库后，会自动触发 `.github/workflows/build.yml` 中的构建流程，构建完成后可在 Actions 页面下载 APK。

## 项目结构

```
RestartHelper/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fyt/restarthelper/
│       │   ├── MainActivity.java      # 主界面
│       │   ├── AppListActivity.java   # 应用选择列表
│       │   ├── AppInfo.java           # 应用信息模型
│       │   └── AppAdapter.java        # 列表适配器
│       └── res/
│           ├── layout/                # 布局文件
│           ├── values/                # 字符串、颜色、主题
│           └── mipmap/                # 应用图标
├── gradle/wrapper/                    # Gradle Wrapper
├── build.gradle                       # 项目级构建配置
├── settings.gradle                    # 项目设置
└── .github/workflows/build.yml        # GitHub Actions 配置
```

## 权限说明

- `KILL_BACKGROUND_PROCESSES`：用于杀掉目标应用的后台进程
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`：避免被系统电池优化限制

## 注意事项

- 本应用使用 `killBackgroundProcesses` 杀掉后台进程，对于正在前台运行的应用可能无法完全杀掉，需要系统签名或 Root 权限才能强制停止
- 目标应用必须有可启动的 Launcher Activity 才能被重启
