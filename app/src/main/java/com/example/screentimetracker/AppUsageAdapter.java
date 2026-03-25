package com.example.screentimetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.screentimetracker.model.AppUsageModel;

import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.ViewHolder> {

    List<AppUsageModel> list;

    public AppUsageAdapter(List<AppUsageModel> list) {
        this.list = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppUsageModel model = list.get(position);

        holder.name.setText(model.getAppName());

        if (model.icon != null) {
            holder.icon.setImageDrawable(model.icon);
        } else {
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Grey out uninstalled apps
        float alpha = model.isInstalled ? 1.0f : 0.5f;
        holder.icon.setAlpha(alpha);
        holder.name.setAlpha(alpha);
        holder.time.setAlpha(alpha);

        long min = model.getTime() / (1000 * 60);
        holder.time.setText(min + " min");
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, time;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivAppIcon);
            name = itemView.findViewById(R.id.appName);
            time = itemView.findViewById(R.id.time);
        }
    }
}
