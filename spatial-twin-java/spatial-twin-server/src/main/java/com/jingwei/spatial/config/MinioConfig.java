package com.jingwei.spatial.config;

import io.minio.MinioClient;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * MinIO 客户端及启动校验装配。
 */
@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    @ConditionalOnProperty(prefix = "jingwei.storage.minio", name = "enabled", havingValue = "true")
    public MinioClient minioClient(MinioProperties properties) {
        validate(properties);
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "jingwei.storage.minio", name = "enabled", havingValue = "true")
    public ApplicationRunner minioBucketInitializer(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        return new MinioBucketInitializer(minioClient, properties);
    }

    private void validate(MinioProperties properties) {
        requireText(properties.getEndpoint(), "JW_MINIO_ENDPOINT");
        requireText(properties.getBucket(), "JW_MINIO_BUCKET");
        requireText(properties.getAccessKey(), "JW_MINIO_ACCESS_KEY");
        requireText(properties.getSecretKey(), "JW_MINIO_SECRET_KEY");

        URI endpoint;
        try {
            endpoint = URI.create(properties.getEndpoint());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("MinIO endpoint 格式无效，请检查 JW_MINIO_ENDPOINT", exception);
        }
        if (!"http".equalsIgnoreCase(endpoint.getScheme())
                && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalStateException("MinIO endpoint 必须使用 http 或 https 协议");
        }
    }

    private void requireText(String value, String environmentName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("已启用 MinIO，但未配置 " + environmentName);
        }
    }
}
