package com.example.nudge;

public class UserObject {
    private String name, phone, uid;

    public UserObject(String uid, String name, String phone){
        this.uid = uid;
        this.name = name;
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }
    public String getName() {
        return name;
    }
    public String getUid() { return uid; }
}
