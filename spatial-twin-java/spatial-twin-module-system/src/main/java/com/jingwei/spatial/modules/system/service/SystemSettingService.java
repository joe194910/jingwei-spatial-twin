package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.system.entity.SettingEntity;
import com.jingwei.spatial.modules.system.mapper.SettingMapper;
import com.jingwei.spatial.modules.system.vo.SettingVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 管理全局唯一的系统单值配置，并对安全参数执行白名单校验。
 */
@Service
public class SystemSettingService {

    public static final String LOGIN_MAX_FAILURES = "security.login.max-failures";
    public static final String LOGIN_LOCK_MINUTES = "security.login.lock-minutes";
    public static final String LOGIN_MASTER_PASSWORD = "security.login.master-password";

    private static final Map<String, Predicate<String>> VALIDATORS = Map.of(
            LOGIN_MAX_FAILURES, value -> integerBetween(value, 3, 20),
            LOGIN_LOCK_MINUTES, value -> integerBetween(value, 1, 1440),
            "security.password.min-length", value -> integerBetween(value, 12, 128),
            "system.registration.enabled", value -> "true".equals(value) || "false".equals(value),
            LOGIN_MASTER_PASSWORD, value -> value.length() >= 12 && value.length() <= 128
    );

    private final SettingMapper settingMapper;
    private final SystemAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public SystemSettingService(
            SettingMapper settingMapper,
            SystemAuditService auditService,
            PasswordEncoder passwordEncoder
    ) {
        this.settingMapper = settingMapper;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 查询全部系统配置。
     *
     * @return 按配置键排序的配置列表
     */
    public List<SettingVO> list() {
        return settingMapper.selectList(Wrappers.<SettingEntity>lambdaQuery()
                        .orderByAsc(SettingEntity::getSettingKey))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 更新已登记的唯一配置，不允许通过接口创建任意安全开关。
     *
     * @param key   配置键
     * @param value 新配置值
     */
    @Transactional
    public void update(String key, String value) {
        SettingEntity entity = findByKey(key);
        String normalized = value.trim();
        Predicate<String> validator = VALIDATORS.get(key);
        if (validator == null || !validator.test(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "配置值不符合允许范围");
        }
        entity.setSettingValue(LOGIN_MASTER_PASSWORD.equals(key)
                ? passwordEncoder.encode(normalized)
                : normalized);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUserId());
        settingMapper.updateById(entity);
        auditService.record("SETTING", key, "UPDATE", "SUCCESS", "系统安全配置已更新");
    }

    /**
     * 读取整数配置，数据库值异常时使用安全默认值。
     *
     * @param key          配置键
     * @param defaultValue 安全默认值
     * @return 有效整数值
     */
    public int getInt(String key, int defaultValue) {
        SettingEntity entity = settingMapper.selectOne(Wrappers.<SettingEntity>lambdaQuery()
                .eq(SettingEntity::getSettingKey, key));
        if (entity == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(entity.getSettingValue());
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    /**
     * 校验管理员配置的万能登录密码。数据库只保存哈希，未设置时始终返回失败。
     *
     * @param rawPassword 登录请求中的明文密码
     * @return 是否匹配已启用的万能密码
     */
    public boolean matchesMasterPassword(String rawPassword) {
        SettingEntity entity = settingMapper.selectOne(Wrappers.<SettingEntity>lambdaQuery()
                .eq(SettingEntity::getSettingKey, LOGIN_MASTER_PASSWORD));
        if (entity == null || "!".equals(entity.getSettingValue())) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, entity.getSettingValue());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private SettingEntity findByKey(String key) {
        SettingEntity entity = settingMapper.selectOne(Wrappers.<SettingEntity>lambdaQuery()
                .eq(SettingEntity::getSettingKey, key));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "系统配置不存在");
        }
        return entity;
    }

    private SettingVO toVO(SettingEntity entity) {
        boolean sensitive = Integer.valueOf(1).equals(entity.getSensitive());
        return new SettingVO(
                entity.getSettingKey(),
                sensitive ? "******" : entity.getSettingValue(),
                entity.getValueType(),
                entity.getDescription(),
                sensitive
        );
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }

    private static boolean integerBetween(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= min && parsed <= max;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
