package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class LocationModels {

    public static class LocationModel {
        @SerializedName("id") private String id;
        @SerializedName("name") private String name;

        public String getId() { return id; }
        public String getName() { return name; }
    }
}