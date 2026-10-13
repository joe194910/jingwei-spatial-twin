package com.jingwei.spatial.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 对象存储配置。
 */
@ConfigurationProperties(prefix = "jingwei.storage.minio")
public class MinioProperties {

    private boolean enabled;
    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private boolean createBucketIfAbsent;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isCreateBucketIfAbsent() {
        return createBucketIfAbsent;
    }

    public void setCreateBucketIfAbsent(boolean createBucketIfAbsent) {
        this.createBucketIfAbsent = createBucketIfAbsent;
    }
}
