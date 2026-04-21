package com.example.inventory_system_ht.Adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.R;

import java.util.List;

public class SumProductAdapter extends RecyclerView.Adapter<SumProductAdapter.ViewHolder> {

    private List<ItemModels.SumProductModel> list;

    public SumProductAdapter(List<ItemModels.SumProductModel> list) {
        this.list = list;
    }

    public void updateData(List<ItemModels.SumProductModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ItemModels.SumProductModel item = list.get(position);

        int count = item.getCount();
        int required = item.getRequired();

        String name = item.getItemName();
        if (name == null || name.trim().isEmpty()) {
            name = "Item tidak diketahui";
        }

        String itemId = item.getItemId();
        if (itemId == null) itemId = "-";

        if (h.tvProductName != null) h.tvProductName.setText(name);
        if (h.tvLocation != null) h.tvLocation.setText("Kode: " + itemId);

        if (h.tvQty != null) {
            if (required > 0) {
                h.tvQty.setText(count + "/" + required);
            } else {
                h.tvQty.setText(count + "/?");
            }
        }

        boolean fulfilled = required > 0 && count >= required;
        boolean over = required > 0 && count > required;

        int bgColor;
        if (over) {
            bgColor = Color.parseColor("#D32F2F");
        } else if (fulfilled) {
            bgColor = Color.parseColor("#2E7D32");
        } else {
            bgColor = Color.parseColor("#1976D2");
        }
        if (h.cardRoot != null) h.cardRoot.setCardBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardRoot;
        TextView tvProductName, tvLocation, tvQty;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            if (itemView instanceof CardView) {
                cardRoot = (CardView) itemView;
            }
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvQty = itemView.findViewById(R.id.tvQty);
        }
    }
}