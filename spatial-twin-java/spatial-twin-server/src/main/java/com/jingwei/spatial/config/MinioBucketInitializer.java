package com.jingwei.spatial.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 启动时检查 MinIO 目标桶，避免文件业务运行到上传阶段才暴露配置问题。
 */
public class MinioBucketInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioBucketInitializer(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists && !properties.isCreateBucketIfAbsent()) {
                throw new IllegalStateException("MinIO 目标桶不存在，且当前未允许自动创建");
            }
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.getBucket())
                        .build());
                log.info("MinIO 目标桶已创建，bucket={}", properties.getBucket());
                return;
            }
            log.info("MinIO 连接和目标桶检查通过，bucket={}", properties.getBucket());
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "MinIO 初始化失败，请检查服务状态、访问凭证和目标桶配置",
                    exception
            );
        }
    }
}
