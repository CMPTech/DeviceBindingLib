package com.example.devicebindinglib.Models;

public class Model {
    private String deviceId;
    private Object responseData;

    public Model(String device) {
        this.deviceId = device;
    }

    public Object getResponseData() {
        return responseData;
    }
}
