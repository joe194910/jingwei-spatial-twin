package com.jingwei.spatial;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 经纬空间后端应用入口。
 *
 * <p>当前范围包含统一响应、身份认证以及用户、角色、菜单的 RBAC 管理闭环。</p>
 */
@MapperScan("com.jingwei.spatial.modules")
@SpringBootApplication
public class SpatialApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpatialApplication.class, args);
    }
}
