package com.jingwei.spatial.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 校验公共配置与开发环境配置的关键约定，避免环境切换和本机连接参数被意外改回。
 */
class ApplicationYamlConfigTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void shouldUseDevProfileAndKeepLocalInfrastructureDefaults() throws IOException {
        ConfigurableEnvironment environment = new StandardEnvironment();
        addFirst(environment, "application.yml");
        addFirst(environment, "application-dev.yml");

        String configuredProfile = environment.getProperty("spring.profiles.active");
        assertEquals("dev", environment.resolvePlaceholders(configuredProfile));
        assertEquals("root", environment.getProperty("spring.datasource.username"));
        assertEquals("abc123456", environment.getProperty("spring.datasource.password"));
        assertEquals("123456", environment.getProperty("spring.data.redis.password"));
        assertEquals("5", environment.getProperty("spring.data.redis.database"));
        assertEquals("http://127.0.0.1:9000",
                environment.getProperty("jingwei.storage.minio.endpoint"));
        assertEquals("spatial-twin", environment.getProperty("jingwei.storage.minio.bucket"));
        assertEquals("nexora_admin", environment.getProperty("jingwei.storage.minio.access-key"));
        assertEquals("NexoraMinio2026", environment.getProperty("jingwei.storage.minio.secret-key"));
        assertEquals("false", environment.getProperty("jingwei.storage.minio.enabled"));
    }

    @Test
    void shouldSwitchToProdProfileWithConfigActiveProperty() throws IOException {
        ConfigurableEnvironment environment = new StandardEnvironment();
        addFirst(environment, "application.yml");
        environment.getPropertySources().addFirst(
                new SystemEnvironmentPropertySource(
                        "profileOverride",
                        Map.of("CONFIG_ACTIVE", "prod")
                )
        );

        String configuredProfile = environment.getProperty("spring.profiles.active");
        assertEquals("prod", environment.resolvePlaceholders(configuredProfile));
    }

    private void addFirst(ConfigurableEnvironment environment, String resourceName) throws IOException {
        List<PropertySource<?>> propertySources = loader.load(
                resourceName,
                new ClassPathResource(resourceName)
        );
        for (PropertySource<?> propertySource : propertySources) {
            environment.getPropertySources().addFirst(propertySource);
        }
    }
}
