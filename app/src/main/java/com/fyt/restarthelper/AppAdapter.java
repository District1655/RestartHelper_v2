package com.fyt.restarthelper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private List<AppInfo> data = new ArrayList<>();
    private OnAppClickListener listener;

    public interface OnAppClickListener {
        void onAppClick(AppInfo appInfo);
    }

    public AppAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<AppInfo> data) {
        this.data = data != null ? data : new ArrayList<AppInfo>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final AppInfo appInfo = data.get(position);
        holder.tvName.setText(appInfo.getAppName());
        holder.tvPackage.setText(appInfo.getPackageName());
        if (appInfo.getIcon() != null) {
            holder.ivIcon.setImageDrawable(appInfo.getIcon());
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onAppClick(appInfo);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        TextView tvPackage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPackage = itemView.findViewById(R.id.tv_package);
        }
    }
}
