package com.multiprofit.dto;

import lombok.Data;

/**
 * 统一API响应封装
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(200);
        resp.setMessage("success");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(500);
        resp.setMessage(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setCode(code);
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }
}
