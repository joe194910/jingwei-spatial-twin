package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.jingwei.spatial.common.audit.AuditRecorder;
import com.jingwei.spatial.modules.system.entity.AuditLogEntity;
import com.jingwei.spatial.modules.system.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 写入登录和权限变更等关键审计记录。
 */
@Service
public class SystemAuditService implements AuditRecorder {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest request;

    public SystemAuditService(AuditLogMapper auditLogMapper, HttpServletRequest request) {
        this.auditLogMapper = auditLogMapper;
        this.request = request;
    }

    /**
     * 记录安全审计事件，详情必须是脱敏后的简短文本。
     *
     * @param objectType   对象类型
     * @param objectId     对象标识
     * @param action       操作名称
     * @param result       操作结果
     * @param detailsMasked 脱敏详情
     */
    public void record(
            String objectType,
            String objectId,
            String action,
            String result,
            String detailsMasked
    ) {
        record(objectType, objectId, null, action, result, detailsMasked);
    }

    /**
     * 记录带项目归属的审计事件，便于后续按项目追踪关键操作。
     *
     * @param objectType    对象类型
     * @param objectId      对象标识
     * @param projectId     所属项目标识
     * @param action        操作名称
     * @param result        操作结果
     * @param detailsMasked 脱敏详情
     */
    @Override
    public void record(
            String objectType,
            String objectId,
            Long projectId,
            String action,
            String result,
            String detailsMasked
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        if (StpUtil.isLogin()) {
            entity.setUserId(Long.valueOf(String.valueOf(StpUtil.getLoginId())));
        }
        entity.setProjectId(projectId);
        entity.setObjectType(objectType);
        entity.setObjectId(objectId);
        entity.setAction(action);
        entity.setResult(result);
        entity.setRequestId(MDC.get("requestId"));
        entity.setIp(request.getRemoteAddr());
        entity.setUserAgent(normalizeUserAgent(request.getHeader("User-Agent")));
        entity.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setDetailsMasked(detailsMasked);
        auditLogMapper.insert(entity);
    }

    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }
}
