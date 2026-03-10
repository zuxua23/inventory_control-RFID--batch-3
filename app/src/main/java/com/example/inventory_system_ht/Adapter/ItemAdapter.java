package com.example.inventory_system_ht.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.R;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<ItemModels.ItemModel> itemList;
    private OnItemClickListener listener;

    private int lastScannedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(ItemModels.ItemModel item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setLastScannedPosition(int position) {
        this.lastScannedPosition = position;
    }

    public ItemAdapter(List<ItemModels.ItemModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemModels.ItemModel item = itemList.get(position);
        CardView cardView = (CardView) holder.itemView;

        if (position == lastScannedPosition) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue_theme));
        }

        holder.tvTagId.setText(item.getEpcTag());
        holder.tvProductName.setText(item.getItemName());
        holder.tvQty.setText("Qty: " + item.getQty());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagId, tvProductName, tvQty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagId = itemView.findViewById(R.id.tvTagId);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQty = itemView.findViewById(R.id.tvQty);
        }
    }
}