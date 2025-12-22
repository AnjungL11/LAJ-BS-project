package com.imagemanager.common;

import lombok.Data;

/**
 * 统一API响应结果封装
 */
@Data
public class Result<T> {
    // 状态码,200成功，其他失败
    private Integer code;
    // 提示信息
    private String message;
    // 返回数据
    private T data;
    public Result() {
    }
    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    // 成功返回，带数据
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    // 成功返回，不带数据
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    // 失败返回，带错误信息
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }
    // 失败返回，带状态码和错误信息
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}