package com.example.inventory_system_ht.Models
;

public class DOModel {
    private String doId;
    private String doNo;
    private String doName;
    private String doDate;

    public DOModel(String doId, String doNo, String doName, String doDate) {
        this.doId = doId;
        this.doNo = doNo;
        this.doName = doName;
        this.doDate = doDate;
    }

    public String getDoNo() { return doNo; }
    public String getDoName() { return doName; }
    public String getDoDate() { return doDate; }
}