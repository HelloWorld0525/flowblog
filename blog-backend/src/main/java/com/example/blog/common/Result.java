package com.example.blog.common;

import lombok.Data;

/**
 * 统一响应结果封装类
 */
@Data
public class Result<T> {
    private Integer code; // 200:成功, 400:失败
    private String msg;   // 提示信息
    private T data;       // 返回数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> r = new Result<>();
        r.setCode(400);
        r.setMsg(msg);
        return r;
    }
}