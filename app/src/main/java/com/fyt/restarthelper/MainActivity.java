package com.fyt.restarthelper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataOutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RestartHelperPrefs";
    private static final String KEY_PACKAGE = "target_package";
    private static final String KEY_AUTO_RESTART = "auto_restart";
    private static final int REQUEST_SELECT_APP = 1001;

    private TextView tvTargetName;
    private TextView tvTargetPackage;
    private TextView tvStatus;
    private TextView tvRootStatus;
    private Button btnRestart;
    private Button btnReselect;
    private Button btnClose;
    private Button btnToggleAuto;

    private String targetPackage;
    private boolean autoRestart = true;
    private boolean hasRoot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTargetName = findViewById(R.id.tv_target_name);
        tvTargetPackage = findViewById(R.id.tv_target_package);
        tvStatus = findViewById(R.id.tv_status);
        tvRootStatus = findViewById(R.id.tv_root_status);
        btnRestart = findViewById(R.id.btn_restart);
        btnReselect = findViewById(R.id.btn_reselect);
        btnClose = findViewById(R.id.btn_close);
        btnToggleAuto = findViewById(R.id.btn_toggle_auto);

        loadSettings();
        loadTargetApp();
        updateAutoButton();

        // 检测 Root 权限
        checkRootPermission();

        // 检查是否通过 ACTION_RUN 外部调用启动
        Intent intent = getIntent();
        boolean isExternalCall = intent != null && "com.fyt.restarthelper.ACTION_RUN".equals(intent.getAction());

        if (isExternalCall) {
            if (targetPackage != null && !targetPackage.isEmpty()) {
                restartTargetApp();
                // 兜底：4秒后强制关闭，防止退到后台后finish不执行
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 4000);
            } else {
                Toast.makeText(this, "还未选择目标应用", Toast.LENGTH_SHORT).show();
            }
        } else if (autoRestart && targetPackage != null && !targetPackage.isEmpty()) {
            tvStatus.setText("即将自动重启...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                restartTargetApp();
                // 兜底：4秒后强制关闭
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 4000);
            }, 1000);
        }

        btnRestart.setOnClickListener(v -> {
            if (targetPackage != null && !targetPackage.isEmpty()) {
                restartTargetApp();
            } else {
                Toast.makeText(MainActivity.this, "请先选择目标应用", Toast.LENGTH_SHORT).show();
            }
        });

        btnReselect.setOnClickListener(v -> {
            Intent selectIntent = new Intent(MainActivity.this, AppListActivity.class);
            startActivityForResult(selectIntent, REQUEST_SELECT_APP);
        });

        btnClose.setOnClickListener(v -> finish());

        btnToggleAuto.setOnClickListener(v -> {
            autoRestart = !autoRestart;
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_AUTO_RESTART, autoRestart).apply();
            updateAutoButton();
            Toast.makeText(this, autoRestart ? "已开启启动自动重启" : "已关闭启动自动重启", Toast.LENGTH_SHORT).show();
        });

        if (targetPackage == null || targetPackage.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent selectIntent = new Intent(MainActivity.this, AppListActivity.class);
                startActivityForResult(selectIntent, REQUEST_SELECT_APP);
            }, 300);
        }
    }

    private void checkRootPermission() {
        new Thread(() -> {
            hasRoot = executeRootCommand("echo root_ok");
            runOnUiThread(() -> {
                if (hasRoot) {
                    tvRootStatus.setText("Root 权限：已获取 ✓");
                    tvRootStatus.setTextColor(0xFF4CAF50);
                } else {
                    tvRootStatus.setText("Root 权限：未获取（将使用普通方式）");
                    tvRootStatus.setTextColor(0xFFFF9800);
                }
            });
        }).start();
    }

    private boolean executeRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (Exception ignored) {}
        }
    }

    private boolean forceStopWithRoot(String packageName) {
        return executeRootCommand("am force-stop " + packageName);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        autoRestart = prefs.getBoolean(KEY_AUTO_RESTART, true);
    }

    private void updateAutoButton() {
        btnToggleAuto.setText(autoRestart ? "自动重启：开" : "自动重启：关");
    }

    private String getTargetPackage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PACKAGE, null);
    }

    private void loadTargetApp() {
        targetPackage = getTargetPackage();
        if (targetPackage != null && !targetPackage.isEmpty()) {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(targetPackage, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                tvTargetName.setText(appName);
                tvTargetPackage.setText(targetPackage);
                btnRestart.setEnabled(true);
            } catch (PackageManager.NameNotFoundException e) {
                tvTargetName.setText("应用未找到");
                tvTargetPackage.setText(targetPackage);
                btnRestart.setEnabled(false);
            }
        } else {
            tvTargetName.setText("未选择");
            tvTargetPackage.setText("请点击下方按钮选择目标应用");
            btnRestart.setEnabled(false);
        }
    }

    private void restartTargetApp() {
        if (targetPackage == null || targetPackage.isEmpty()) {
            return;
        }

        tvStatus.setText("正在重启应用...");

        // 先杀进程
        boolean killed = false;
        if (hasRoot) {
            // Root 方式：强制停止，能杀掉前台应用
            killed = forceStopWithRoot(targetPackage);
            if (killed) {
                tvStatus.setText("Root 强制停止成功，正在启动...");
            } else {
                tvStatus.setText("Root 停止失败，尝试普通方式...");
            }
        }

        if (!killed) {
            // 普通方式：杀后台进程
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    am.killBackgroundProcesses(targetPackage);
                }
                // 尝试杀运行中的进程
                try {
                    if (am != null) {
                        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                        if (processes != null) {
                            for (ActivityManager.RunningAppProcessInfo process : processes) {
                                if (process.processName != null && process.processName.startsWith(targetPackage)) {
                                    android.os.Process.killProcess(process.pid);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            } catch (Exception e) {
                tvStatus.setText("杀进程失败: " + e.getMessage());
            }
        }

        // 等待后启动应用
        int delay = hasRoot && killed ? 600 : 900;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(targetPackage);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(launchIntent);
                    tvStatus.setText("重启成功！");
                    Toast.makeText(MainActivity.this, "已重启应用", Toast.LENGTH_SHORT).show();
                    // 重启成功后立即关闭自身（避免退到后台后Handler被系统限制）
                    new Handler(Looper.getMainLooper()).postDelayed(this::finish, 300);
                } else {
                    tvStatus.setText("无法启动该应用（可能无启动界面）");
                    Toast.makeText(MainActivity.this, "无法启动该应用", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                tvStatus.setText("启动失败: " + e.getMessage());
                Toast.makeText(MainActivity.this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, delay);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_APP && resultCode == RESULT_OK && data != null) {
            String pkg = data.getStringExtra("package_name");
            if (pkg != null && !pkg.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(KEY_PACKAGE, pkg).apply();
                targetPackage = pkg;
                loadTargetApp();
                tvStatus.setText("已选择目标应用");
                Toast.makeText(this, "已选择目标应用", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
