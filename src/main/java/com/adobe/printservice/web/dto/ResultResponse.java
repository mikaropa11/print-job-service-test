package com.adobe.printservice.web.dto;

public class ResultResponse {

    private String result;

    public ResultResponse() {
    }

    public ResultResponse(String result) {
        this.result = result;
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
