package com.jingwei.spatial.common.api;

import org.slf4j.MDC;

import java.time.Instant;

/**
 * 所有 HTTP 接口统一使用的响应体。
 *
 * @param code      业务状态码，0 表示成功
 * @param message   中文结果或错误信息
 * @param data      业务数据，失败时通常为空
 * @param requestId 请求链路标识
 * @param timestamp 服务端响应时间
 * @param <T>       业务数据类型
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String requestId,
        Instant timestamp
) {

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "操作成功", data, currentRequestId(), Instant.now());
    }

    /**
     * 构造带自定义提示的成功响应。
     *
     * @param message 中文成功提示
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 统一成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data, currentRequestId(), Instant.now());
    }

    /**
     * 构造失败响应。
     *
     * @param code    业务错误码
     * @param message 中文错误信息
     * @return 统一失败响应
     */
    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null, currentRequestId(), Instant.now());
    }

    private static String currentRequestId() {
        return MDC.get("requestId");
    }
}
