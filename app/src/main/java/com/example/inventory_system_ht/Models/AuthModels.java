package com.example.inventory_system_ht.Models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AuthModels {
    public static class LoginRequest {
        private final String username;
        private final String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
    public static class LoginResponse {
        private boolean success;
        private String token;

        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
    }
    public static class RegisterRequest {
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

    public static class UserModel {
        private final String usr_id;
        private final String usr_name;
        private final String usr_fullname;
        private final String usr_password;
        private final int role_id;
        public UserModel(String usr_id, String usr_name, String usr_fullname, String usr_password, int role_id) {
            this.usr_id = usr_id;
            this.usr_name = usr_name;
            this.usr_fullname = usr_fullname;
            this.usr_password = usr_password;
            this.role_id = role_id;
        }

        public String getUsr_id() { return usr_id; }
        public String getUsr_name() { return usr_name; }
        public String getUsr_fullname() { return usr_fullname; }
        public String getUsr_password() { return usr_password; }
        public int getRole_id() { return role_id; }
    }
}
