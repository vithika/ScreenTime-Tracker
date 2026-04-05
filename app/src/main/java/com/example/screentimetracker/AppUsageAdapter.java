package com.example.screentimetracker;

import android.graphics.Color;
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
    long totalMs;

    public AppUsageAdapter(List<AppUsageModel> list, long totalMs) {
        this.list = list;
        this.totalMs = totalMs;
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
        holder.percentage.setAlpha(alpha);


        long totalMinutes = model.getTime() / (1000 * 60);
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;
        holder.time.setText(hours > 0 ? hours + "h " + mins + "m" : mins + "m");


        if (totalMs > 0) {
            double pct = (model.getTime() * 100.0) / totalMs;
            holder.percentage.setText(String.format("%.1f%%", pct));

            // Color code
            if (pct >= 30) {
                holder.percentage.setTextColor(Color.parseColor("#F44336")); // red
            } else if (pct >= 15) {
                holder.percentage.setTextColor(Color.parseColor("#FFC107")); // yellow
            } else {
                holder.percentage.setTextColor(Color.parseColor("#4CAF50")); // green
            }
        } else {
            holder.percentage.setText("0%");
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, time,percentage;

        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivAppIcon);
            name = itemView.findViewById(R.id.appName);
            time = itemView.findViewById(R.id.time);
            percentage = itemView.findViewById(R.id.tvPercentage); // ← add this
        }
    }
}
