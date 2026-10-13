package com.jingwei.spatial.modules.auth.service;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.auth.dto.LoginRequest;
import com.jingwei.spatial.modules.auth.vo.CurrentUserVO;
import com.jingwei.spatial.modules.auth.vo.LoginVO;
import com.jingwei.spatial.modules.system.entity.UserEntity;
import com.jingwei.spatial.modules.system.mapper.UserMapper;
import com.jingwei.spatial.modules.system.service.AccessMetadataService;
import com.jingwei.spatial.modules.system.service.SystemAuditService;
import com.jingwei.spatial.modules.system.service.SystemSettingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 密码登录与当前会话服务，支持账号密码和管理员配置的万能密码。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccessMetadataService accessMetadataService;
    private final SystemAuditService auditService;
    private final SystemSettingService settingService;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AccessMetadataService accessMetadataService,
            SystemAuditService auditService,
            SystemSettingService settingService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.accessMetadataService = accessMetadataService;
        this.auditService = auditService;
        this.settingService = settingService;
    }

    /**
     * 使用账号自身密码登录，连续失败达到阈值后临时锁定。
     *
     * @param request 登录凭据
     * @return Sa-Token 会话和权限摘要
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public LoginVO login(LoginRequest request) {
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, request.username().trim()));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        boolean normalPasswordMatched = user != null
                && passwordEncoder.matches(request.password(), user.getPasswordHash());
        boolean masterPasswordMatched = user != null
                && !normalPasswordMatched
                && settingService.matchesMasterPassword(request.password());
        if (user == null || (!normalPasswordMatched && !masterPasswordMatched)) {
            handleLoginFailure(user, now);
            auditService.record("AUTH", request.username().trim(), "LOGIN", "FAILED", "用户名或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            auditService.record("AUTH", String.valueOf(user.getId()), "LOGIN", "FAILED", "账号已停用");
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账号暂时锁定，请稍后再试");
        }

        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        List<String> roles = accessMetadataService.getRoleCodes(user.getId());
        List<String> permissions = accessMetadataService.getPermissionCodes(user.getId());
        auditService.record(
                "AUTH",
                String.valueOf(user.getId()),
                "LOGIN",
                "SUCCESS",
                masterPasswordMatched ? "管理员万能密码登录" : user.getUsername()
        );
        return new LoginVO(
                tokenInfo.getTokenName(),
                tokenInfo.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                roles,
                permissions
        );
    }

    /**
     * 查询当前登录用户及即时权限集合。
     *
     * @return 当前用户信息
     */
    public CurrentUserVO currentUser() {
        long userId = currentUserId();
        UserEntity user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号不可用，请重新登录");
        }
        return new CurrentUserVO(
                userId,
                user.getUsername(),
                user.getNickname(),
                accessMetadataService.getRoleCodes(userId),
                accessMetadataService.getPermissionCodes(userId)
        );
    }

    /**
     * 注销当前会话并记录审计。
     */
    public void logout() {
        String loginId = String.valueOf(StpUtil.getLoginId());
        auditService.record("AUTH", loginId, "LOGOUT", "SUCCESS", "用户主动退出");
        StpUtil.logout();
    }

    private void handleLoginFailure(UserEntity user, LocalDateTime now) {
        if (user == null) {
            return;
        }
        int failures = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        int maxFailures = settingService.getInt(SystemSettingService.LOGIN_MAX_FAILURES, 5);
        user.setLoginFailCount(failures);
        if (failures >= maxFailures) {
            int lockMinutes = settingService.getInt(SystemSettingService.LOGIN_LOCK_MINUTES, 30);
            user.setLockedUntil(now.plusMinutes(lockMinutes));
            user.setLoginFailCount(0);
        }
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }
}
