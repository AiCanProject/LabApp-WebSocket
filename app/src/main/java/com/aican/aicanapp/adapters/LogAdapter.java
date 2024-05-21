package com.aican.aicanapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aican.aicanapp.R;
import com.aican.aicanapp.dataClasses.phData;

import java.util.List;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

    Context context;
    List<phData> logs_list;

    public LogAdapter(Context context, List<phData> logs_list) {
        this.context = context;
        this.logs_list = logs_list;
    }

    @NonNull
    @Override
    public LogAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.item_log, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull LogAdapter.ViewHolder holder, int position) {
        if (logs_list != null && logs_list.size() > 0) {
            holder.sNo.setText((position + 1) + "");
            holder.ph.setText(logs_list.get(position).getpH());
            holder.temp.setText(logs_list.get(position).getmV());
            holder.dt.setText(logs_list.get(position).getDate());
            holder.time.setText(logs_list.get(position).getTime());
            holder.batchnum.setText(logs_list.get(position).getBatchnum());
            holder.arnum.setText(logs_list.get(position).getArnum());
            holder.compound_name.setText(logs_list.get(position).getCompound_name());
            holder.unknown1.setText(logs_list.get(position).getUnknown1());
            holder.unknown2.setText(logs_list.get(position).getUnknown2());
        }
    }

    @Override
    public int getItemCount() {
        return logs_list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sNo, ph, temp, dt, time, batchnum, arnum, compound_name, unknown1, unknown2;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sNo = itemView.findViewById(R.id.sNo);
            ph = itemView.findViewById(R.id.phLog);
            temp = itemView.findViewById(R.id.tempLog);
            time = itemView.findViewById(R.id.time);
            dt = itemView.findViewById(R.id.date);
            batchnum = itemView.findViewById(R.id.batchnum);
            arnum = itemView.findViewById(R.id.arnum);
            compound_name = itemView.findViewById(R.id.compound);
            unknown1 = itemView.findViewById(R.id.unknown1);
            unknown2 = itemView.findViewById(R.id.unknown2);
        }
    }
}

