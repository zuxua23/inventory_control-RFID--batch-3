package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

public class LocationModels {
    public static class LocationModel {
        @SerializedName("id")
        private String id;

        @SerializedName("locId")
        private String locId;

        @SerializedName("name")
        private String name;

        public String getId() { return id; }
        public String getLocId() { return locId; }
        public String getName() { return name; }
    }
}