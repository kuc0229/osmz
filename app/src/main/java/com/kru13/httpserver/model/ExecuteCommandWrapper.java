package com.kru13.httpserver.model;

public class ExecuteCommandWrapper {

    private String data;
    private int returnCode;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
}
