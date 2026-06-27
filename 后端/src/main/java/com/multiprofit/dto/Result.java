package com.multiprofit.dto;

import lombok.Data;

/**
 * 统一响应结果封装
 */
@Data
public class Result<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(String message, T data) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
}
