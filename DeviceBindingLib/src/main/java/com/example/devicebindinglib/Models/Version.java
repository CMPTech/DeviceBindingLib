package com.example.devicebindinglib.Models;

public class Version {

    private Integer versionCode;
    private Object responseData;
    private String versionName;

    public Version(Integer code , String Name) {
        this.versionCode = code;
        this.versionName = Name;
    }

    public Object getResponseData() {
        return responseData;
    }
}