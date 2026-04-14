package com.example.inventory_system_ht.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventory_system_ht.Models.ItemModels;
import com.example.inventory_system_ht.R;

import java.util.List;

public class SumProductAdapter extends RecyclerView.Adapter<SumProductAdapter.ViewHolder> {

    private List<ItemModels.SumProductModel> dataList;

    public SumProductAdapter(List<ItemModels.SumProductModel> dataList) {
        this.dataList = dataList;
    }

    public void updateData(List<ItemModels.SumProductModel> newData) {
        this.dataList = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sum_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemModels.SumProductModel item = dataList.get(position);
        holder.tvProductName.setText(item.getItemName());
        holder.tvLocation.setText(item.getItemId());         // nanti ganti ke lokasi kalau API udh ready
        holder.tvQty.setText(item.getCount() + " pcs");
    }

    @Override
    public int getItemCount() {
        return dataList == null ? 0 : dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvLocation, tvQty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvLocation    = itemView.findViewById(R.id.tvLocation);
            tvQty         = itemView.findViewById(R.id.tvQty);
        }
    }
}