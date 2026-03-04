package com.example.inventory_system_ht.Helper;

import com.example.inventory_system_ht.Models.UserModel;

import java.util.ArrayList;
import java.util.List;

public class DummyRepository {
    private static List<UserModel> dummyUsers = new ArrayList<>();

    static {
        dummyUsers.add(new UserModel("U001", "ojan", "Muhammad Nur Fauzan", "ojan123", 1));
        dummyUsers.add(new UserModel("U002", "admin", "Administrator Sato", "admin123", 1));
        dummyUsers.add(new UserModel("U003", "gudang", "Operator Gudang", "gudang123", 2));
    }

    public static UserModel login(String username, String pass) {
        for (UserModel user : dummyUsers) {
            if (user.getUsr_name().equals(username) && user.getUsr_password().equals(pass)) {
                return user;
            }
        }
        return null;
    }
}