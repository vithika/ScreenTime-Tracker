package com.example.screentimetracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HeatmapAdapter extends RecyclerView.Adapter<HeatmapAdapter.ViewHolder> {

    private final long[] hourlyData; // 24 values in ms
    private final long   maxMs;      // max value for scaling

    public HeatmapAdapter(long[] hourlyData) {
        this.hourlyData = hourlyData;
        long max = 0;
        for (long val : hourlyData) if (val > max) max = val;
        this.maxMs = max;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_heatmap_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        long ms      = hourlyData[position];
        long minutes = ms / (1000 * 60);

        // Hour label e.g. "9 AM"
        String hourLabel;
        if (position == 0)       hourLabel = "12 AM";
        else if (position < 12)  hourLabel = position + " AM";
        else if (position == 12) hourLabel = "12 PM";
        else                     hourLabel = (position - 12) + " PM";

        holder.tvHourLabel.setText(hourLabel);

        // Time text
        if (minutes == 0) {
            holder.tvHourTime.setText("");
        } else if (minutes < 60) {
            holder.tvHourTime.setText(minutes + "m");
        } else {
            long h = minutes / 60;
            long m = minutes % 60;
            holder.tvHourTime.setText(h + "h " + m + "m");
        }

        // Bar width as fraction of max
        float fraction = maxMs > 0 ? (float) ms / maxMs : 0f;

        ViewGroup.LayoutParams params = holder.viewBar.getLayoutParams();
        // Get parent width — use weight trick via margins instead
        holder.viewBar.setScaleX(fraction == 0 ? 0.01f : fraction);
        holder.viewBar.setPivotX(0f); // scale from left

        // Color based on usage intensity
        int color;
        if (fraction == 0) {
            color = Color.parseColor("#E0E0E0"); // grey — no usage
        } else if (fraction < 0.33f) {
            color = Color.parseColor("#A5D6A7"); // light green
        } else if (fraction < 0.66f) {
            color = Color.parseColor("#FFA726"); // orange
        } else {
            color = Color.parseColor("#EF5350"); // red — peak hour
        }

        holder.viewBar.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() { return 24; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHourLabel, tvHourTime;
        View     viewBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvHourLabel = itemView.findViewById(R.id.tvHourLabel);
            tvHourTime  = itemView.findViewById(R.id.tvHourTime);
            viewBar     = itemView.findViewById(R.id.viewBar);
        }
    }
}