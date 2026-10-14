package com.jingwei.spatial.common.api;

import java.util.List;

/**
 * 统一分页输出，页码从 1 开始。
 *
 * @param pageNo   当前页码
 * @param pageSize 每页数量
 * @param total    符合条件的总记录数
 * @param records  当前页记录
 * @param <T>      记录类型
 */
public record PageResult<T>(
        long pageNo,
        long pageSize,
        long total,
        List<T> records
) {
}
