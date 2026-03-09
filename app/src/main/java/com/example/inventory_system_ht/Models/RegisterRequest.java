package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RegisterRequest {
    @SerializedName("tagIds")
    private List<String> tagIds;

    public RegisterRequest(List<String> tagIds) {
        this.tagIds = tagIds;
    }

    public List<String> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<String> tagIds) {
        this.tagIds = tagIds;
    }
}