package com.jingwei.spatial.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证系统治理实体所需的 MyBatis-Plus 插件配置。
 */
class MybatisPlusConfigTest {

    @Test
    void shouldRegisterOptimisticLockerInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusConfig().mybatisPlusInterceptor();

        boolean registered = interceptor.getInterceptors().stream()
                .anyMatch(OptimisticLockerInnerInterceptor.class::isInstance);

        assertTrue(registered, "使用 @Version 的实体必须注册乐观锁拦截器");
    }
}
