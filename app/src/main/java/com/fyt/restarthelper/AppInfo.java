package com.fyt.restarthelper;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String appName;
    private String packageName;
    private Drawable icon;
    private boolean systemApp;

    public AppInfo(String appName, String packageName, Drawable icon) {
        this(appName, packageName, icon, false);
    }

    public AppInfo(String appName, String packageName, Drawable icon, boolean systemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.icon = icon;
        this.systemApp = systemApp;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isSystemApp() {
        return systemApp;
    }
}
