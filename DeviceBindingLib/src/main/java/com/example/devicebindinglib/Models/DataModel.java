package com.example.devicebindinglib.Models;

public class DataModel {

    private final String deviceId;
    private Object responseData;
    private String token;

    public DataModel(String device , String ticket) {
        this.deviceId = device;
        this.token = ticket;
    }

    public Object getResponseData() {
        return responseData;
    }

}
