package com.example.inventory_system_ht.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Models.ItemModel;
import com.example.inventory_system_ht.R;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<ItemModel> itemList;
    private OnItemClickListener listener;

    // Buat fitur hapus item kalau di-klik
    public interface OnItemClickListener {
        void onItemClick(ItemModel item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ItemAdapter(List<ItemModel> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pastiin nama layout XML lu sesuai ya bre, misal: item_product.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemModel item = itemList.get(position);

        // 1. TAMPILIN TAG ID / EPC DI ATAS (Sesuai ID XML lu yang baru)
        holder.tvTagId.setText(item.getEpcTag());

        // 2. TAMPILIN NAMA BARANG DI BAWAH
        holder.tvProductName.setText(item.getItemName());

        // 3. TAMPILIN QTY
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
            // Binding ID dari XML ke Java
            tvTagId = itemView.findViewById(R.id.tvTagId);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvQty = itemView.findViewById(R.id.tvQty);
        }
    }
}