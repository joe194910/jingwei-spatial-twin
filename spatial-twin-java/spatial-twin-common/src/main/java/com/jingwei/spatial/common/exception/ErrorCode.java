package com.jingwei.spatial.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 平台统一业务错误码。
 *
 * <p>HTTP 状态码表达协议语义，业务码用于前端稳定识别具体错误。</p>
 */
public enum ErrorCode {
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "请求参数不正确"),
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "登录状态已失效，请重新登录"),
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "当前账号没有操作权限"),
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "请求的数据不存在"),
    CONFLICT(40900, HttpStatus.CONFLICT, "数据状态冲突，请刷新后重试"),
    ACCOUNT_LOCKED(42300, HttpStatus.LOCKED, "账号暂时锁定，请稍后再试"),
    VALIDATION_FAILED(42200, HttpStatus.UNPROCESSABLE_ENTITY, "请求数据校验失败"),
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "系统处理失败，请稍后重试");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
