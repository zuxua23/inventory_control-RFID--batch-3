package com.example.inventory_system_ht.Models;

public class UserModel {
    private String usr_id;
    private String usr_name;
    private String usr_fullname;
    private String usr_password;
    private int role_id;
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