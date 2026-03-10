package com.example.inventory_system_ht.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Models.DOModels;
import com.example.inventory_system_ht.R;

import java.util.List;

public class DOAdapter extends RecyclerView.Adapter<DOAdapter.DOViewHolder> {
    private List<DOModels.DOModel> doList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DOModels.DOModel doModel);
    }

    public DOAdapter(List<DOModels.DOModel> doList, OnItemClickListener listener) {
        this.doList = doList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DOViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery_order, parent, false);
        return new DOViewHolder(view);
    }

    @Override
    public int getItemCount() { return doList.size(); }

    @Override
    public void onBindViewHolder(@NonNull DOViewHolder holder, int position) {
        DOModels.DOModel doItem = doList.get(position);

        holder.tvDoNo.setText(doItem.getDoNo());

        // 👇 FIX: Diganti jadi getStatus() menyesuaikan PDM Database lu 👇
        holder.tvDoName.setText(doItem.getStatus());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(doItem));
    }

    public static class DOViewHolder extends RecyclerView.ViewHolder {
        TextView tvDoNo, tvDoName;
        public DOViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoNo = itemView.findViewById(R.id.tvDoNo);

            // Biarin aja ID XML-nya tvDoName biar lu gak repot ngedit layout UI lagi
            tvDoName = itemView.findViewById(R.id.tvDoName);
        }
    }
}