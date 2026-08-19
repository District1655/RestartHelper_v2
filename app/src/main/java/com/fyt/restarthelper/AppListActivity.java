package com.fyt.restarthelper;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etSearch;
    private ProgressBar progressBar;
    private ToggleButton btnToggleSystem;
    private AppAdapter adapter;
    private List<AppInfo> allApps;
    private boolean showSystemApps = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        recyclerView = findViewById(R.id.recycler_view);
        etSearch = findViewById(R.id.et_search);
        progressBar = findViewById(R.id.progress_bar);
        btnToggleSystem = findViewById(R.id.btn_toggle_system);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter(new AppAdapter.OnAppClickListener() {
            @Override
            public void onAppClick(AppInfo appInfo) {
                Intent result = new Intent();
                result.putExtra("package_name", appInfo.getPackageName());
                setResult(RESULT_OK, result);
                finish();
            }
        });
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnToggleSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showSystemApps = isChecked;
            filterApps(etSearch.getText().toString());
        });

        new LoadAppsTask().execute();
    }

    private void filterApps(String keyword) {
        if (allApps == null) return;
        List<AppInfo> filtered = new ArrayList<>();
        String lower = keyword.toLowerCase();
        for (AppInfo app : allApps) {
            if (!showSystemApps && app.isSystemApp()) {
                continue;
            }
            if (app.getAppName().toLowerCase().contains(lower)
                    || app.getPackageName().toLowerCase().contains(lower)) {
                filtered.add(app);
            }
        }
        adapter.setData(filtered);
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfo>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> appList = new ArrayList<>();
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : packages) {
                // 不过滤系统应用，全部显示，只排除自己
                if (appInfo.packageName.equals(getPackageName())) {
                    continue;
                }
                String appName = pm.getApplicationLabel(appInfo).toString();
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                AppInfo info = new AppInfo(appName, appInfo.packageName,
                        pm.getApplicationIcon(appInfo), isSystem);
                appList.add(info);
            }

            // 排序：非系统应用排前面，同类型按名称排序
            Collections.sort(appList, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo o1, AppInfo o2) {
                    if (o1.isSystemApp() != o2.isSystemApp()) {
                        return o1.isSystemApp() ? 1 : -1;
                    }
                    return o1.getAppName().compareToIgnoreCase(o2.getAppName());
                }
            });

            return appList;
        }

        @Override
        protected void onPostExecute(List<AppInfo> result) {
            allApps = result;
            filterApps("");
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
