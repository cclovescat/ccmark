package com.example.watermarkapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ResultViewHolder> {

    private final Context context;
    private final List<ProcessingCache.ResultItem> resultItems;

    public ResultAdapter(Context context) {
        this.context = context;
        this.resultItems = new ArrayList<>();
    }

    public void setResultItems(List<ProcessingCache.ResultItem> items) {
        this.resultItems.clear();
        if (items != null) {
            this.resultItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    public List<ProcessingCache.ResultItem> getResultItems() {
        return resultItems;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        ProcessingCache.ResultItem item = resultItems.get(position);
        if (item.file != null && item.file.exists()) {
            Glide.with(context)
                    .load(item.file)
                    .centerCrop()
                    .into(holder.ivResult);
        } else {
            Glide.with(context).clear(holder.ivResult);
        }
        holder.tvResultInfo.setText(item.info);
    }

    @Override
    public int getItemCount() {
        return resultItems.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        ImageView ivResult;
        TextView tvResultInfo;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            ivResult = itemView.findViewById(R.id.ivResult);
            tvResultInfo = itemView.findViewById(R.id.tvResultInfo);
        }
    }
}
