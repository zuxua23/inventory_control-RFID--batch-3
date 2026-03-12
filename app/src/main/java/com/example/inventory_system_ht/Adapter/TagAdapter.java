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

import com.example.inventory_system_ht.Models.TagModels;
import com.example.inventory_system_ht.R;
import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {
    private List<TagModels.TagModel> tagList;
    private OnItemClickListener listener;
    private int lastScannedPosition = -1;
    public interface OnItemClickListener {
        void onItemClick(TagModels.TagModel item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public TagAdapter(List<TagModels.TagModel> tagList) {
        this.tagList = tagList;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        TagModels.TagModel tag = tagList.get(position);
        holder.tvTagId.setText(tag.getEpcTag());
        holder.tvProductName.setText(tag.getProductName());
        CardView cardView = (CardView) holder.itemView;
        if (position == lastScannedPosition) {
            cardView.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        } else {
            cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue_theme));
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(tag);
        });
    }


    @Override
    public int getItemCount() { return tagList.size(); }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tvTagId, tvProductName;
        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTagId = itemView.findViewById(R.id.tvTagId);
            tvProductName = itemView.findViewById(R.id.tvProductName);
        }
    }
    public void setLastScannedPosition(int position) {
        this.lastScannedPosition = position;
    }
}