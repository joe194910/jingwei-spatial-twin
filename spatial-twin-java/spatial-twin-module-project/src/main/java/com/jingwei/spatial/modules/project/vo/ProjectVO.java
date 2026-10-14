package com.jingwei.spatial.modules.project.vo;

import java.time.LocalDateTime;

/**
 * 园区项目列表与详情输出。
 */
public record ProjectVO(
        String id,
        String projectCode,
        String name,
        String description,
        String locationName,
        String status,
        String currentMemberRole,
        boolean editable,
        boolean archivable,
        boolean deletable,
        ProjectStartupVO startup,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
