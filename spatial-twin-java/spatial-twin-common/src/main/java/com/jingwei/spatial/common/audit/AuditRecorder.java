package com.jingwei.spatial.common.audit;

/**
 * 跨业务模块统一使用的审计记录端口。
 *
 * <p>业务模块只描述审计事实，具体存储、请求来源和当前用户解析由系统治理模块实现。</p>
 */
public interface AuditRecorder {

    /**
     * 记录一次关键业务操作。
     *
     * @param objectType   业务对象类型
     * @param objectId     业务对象标识
     * @param projectId    所属项目标识，无项目归属时为空
     * @param action       操作动作
     * @param result       操作结果
     * @param detailsMasked 已脱敏的中文摘要
     */
    void record(
            String objectType,
            String objectId,
            Long projectId,
            String action,
            String result,
            String detailsMasked
    );
}
