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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RestartHelperPrefs";
    private static final String KEY_PACKAGE = "target_package";
    private static final String KEY_AUTO_RESTART = "auto_restart";
    private static final int REQUEST_SELECT_APP = 1001;

    private TextView tvTargetName;
    private TextView tvTargetPackage;
    private TextView tvStatus;
    private Button btnRestart;
    private Button btnReselect;
    private Button btnClose;
    private Button btnToggleAuto;

    private String targetPackage;
    private boolean autoRestart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTargetName = findViewById(R.id.tv_target_name);
        tvTargetPackage = findViewById(R.id.tv_target_package);
        tvStatus = findViewById(R.id.tv_status);
        btnRestart = findViewById(R.id.btn_restart);
        btnReselect = findViewById(R.id.btn_reselect);
        btnClose = findViewById(R.id.btn_close);
        btnToggleAuto = findViewById(R.id.btn_toggle_auto);

        loadSettings();
        loadTargetApp();
        updateAutoButton();

        // 检查是否通过 ACTION_RUN 外部调用启动
        Intent intent = getIntent();
        boolean isExternalCall = intent != null && "com.fyt.restarthelper.ACTION_RUN".equals(intent.getAction());

        if (isExternalCall) {
            // 外部调用：直接重启并关闭
            if (targetPackage != null && !targetPackage.isEmpty()) {
                restartTargetApp();
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                return;
            } else {
                Toast.makeText(this, "还未选择目标应用", Toast.LENGTH_SHORT).show();
            }
        } else if (autoRestart && targetPackage != null && !targetPackage.isEmpty()) {
            // 正常启动且开启了自动重启：延迟1秒后自动重启
            tvStatus.setText("即将自动重启...");
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                restartTargetApp();
                new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
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

        // 如果还没有选择目标应用，自动弹出选择
        if (targetPackage == null || targetPackage.isEmpty()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent selectIntent = new Intent(MainActivity.this, AppListActivity.class);
                startActivityForResult(selectIntent, REQUEST_SELECT_APP);
            }, 300);
        }
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

        try {
            // 方式1：杀掉后台进程
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(targetPackage);
            }

            // 方式2：尝试通过 ActivityManager 强制停止（需要系统权限，普通app可能无效）
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

            // 等待一下再启动
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
                    } else {
                        tvStatus.setText("无法启动该应用");
                        Toast.makeText(MainActivity.this, "无法启动该应用（可能无启动界面）", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    tvStatus.setText("启动失败: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, 800);

        } catch (Exception e) {
            tvStatus.setText("重启失败: " + e.getMessage());
            Toast.makeText(this, "重启失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
