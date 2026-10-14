package com.jingwei.spatial.modules.project.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingwei.spatial.common.api.PageResult;
import com.jingwei.spatial.common.audit.AuditRecorder;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.project.dto.ProjectSaveRequest;
import com.jingwei.spatial.modules.project.dto.ProjectStartupSaveRequest;
import com.jingwei.spatial.modules.project.entity.ProjectEntity;
import com.jingwei.spatial.modules.project.entity.ProjectMemberEntity;
import com.jingwei.spatial.modules.project.entity.ProjectStartupEntity;
import com.jingwei.spatial.modules.project.mapper.ProjectMapper;
import com.jingwei.spatial.modules.project.mapper.ProjectMemberMapper;
import com.jingwei.spatial.modules.project.mapper.ProjectStartupMapper;
import com.jingwei.spatial.modules.project.vo.ProjectStartupVO;
import com.jingwei.spatial.modules.project.vo.ProjectVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 园区项目服务，统一处理项目资料、默认地图参数和项目成员数据范围。
 */
@Service
public class ProjectService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String MAP_PROVIDER_AMAP = "AMAP";

    private final ProjectMapper projectMapper;
    private final ProjectStartupMapper startupMapper;
    private final ProjectMemberMapper memberMapper;
    private final AuditRecorder auditRecorder;

    public ProjectService(
            ProjectMapper projectMapper,
            ProjectStartupMapper startupMapper,
            ProjectMemberMapper memberMapper,
            AuditRecorder auditRecorder
    ) {
        this.projectMapper = projectMapper;
        this.startupMapper = startupMapper;
        this.memberMapper = memberMapper;
        this.auditRecorder = auditRecorder;
    }

    /**
     * 分页查询当前用户可访问的项目。
     *
     * @param pageNo   页码，从1开始
     * @param pageSize 每页数量，最大100
     * @param keyword  项目编码、名称或位置关键词
     * @param status   可选状态：ACTIVE或ARCHIVED
     * @return 当前用户可访问的项目分页
     */
    public PageResult<ProjectVO> page(long pageNo, long pageSize, String keyword, String status) {
        validatePage(pageNo, pageSize);
        String normalizedStatus = normalizeStatus(status);
        LambdaQueryWrapper<ProjectEntity> query = Wrappers.<ProjectEntity>lambdaQuery()
                .orderByDesc(ProjectEntity::getUpdatedAt);

        if (normalizedStatus != null) {
            query.eq(ProjectEntity::getStatus, normalizedStatus);
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalizedKeyword = keyword.trim();
            query.and(wrapper -> wrapper.like(ProjectEntity::getProjectCode, normalizedKeyword)
                    .or()
                    .like(ProjectEntity::getName, normalizedKeyword)
                    .or()
                    .like(ProjectEntity::getLocationName, normalizedKeyword));
        }

        if (!isSuperAdmin()) {
            List<Long> projectIds = memberMapper.selectList(
                            Wrappers.<ProjectMemberEntity>lambdaQuery()
                                    .eq(ProjectMemberEntity::getUserId, currentUserId()))
                    .stream()
                    .map(ProjectMemberEntity::getProjectId)
                    .toList();
            if (projectIds.isEmpty()) {
                return new PageResult<>(pageNo, pageSize, 0, List.of());
            }
            query.in(ProjectEntity::getId, projectIds);
        }

        IPage<ProjectEntity> projectPage = projectMapper.selectPage(new Page<>(pageNo, pageSize), query);
        List<Long> projectIds = projectPage.getRecords().stream().map(ProjectEntity::getId).toList();
        Map<Long, ProjectStartupEntity> startupMap = loadStartupMap(projectIds);
        Map<Long, String> memberRoleMap = loadCurrentMemberRoleMap(projectIds);
        List<ProjectVO> records = projectPage.getRecords().stream()
                .map(project -> toVO(project, startupMap.get(project.getId()), memberRoleMap.get(project.getId())))
                .toList();
        return new PageResult<>(projectPage.getCurrent(), projectPage.getSize(), projectPage.getTotal(), records);
    }

    /**
     * 查询当前用户可访问的项目详情。
     *
     * @param projectId 项目标识
     * @return 项目详情
     */
    public ProjectVO get(long projectId) {
        ProjectEntity project = requireAccessibleProject(projectId);
        ProjectStartupEntity startup = requireStartup(projectId);
        return toVO(project, startup, currentMemberRole(projectId));
    }

    /**
     * 创建项目、默认地图参数，并将创建人设置为项目负责人。
     *
     * @param request 项目资料与默认地图参数
     * @return 新项目标识
     */
    @Transactional
    public long create(ProjectSaveRequest request) {
        String projectCode = normalizeProjectCode(request.projectCode());
        ensureProjectCodeAvailable(projectCode, null);
        long operatorId = currentUserId();
        LocalDateTime now = utcNow();

        ProjectEntity project = new ProjectEntity();
        project.setProjectCode(projectCode);
        applyProjectRequest(project, request);
        project.setStatus(STATUS_ACTIVE);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        project.setCreatedBy(operatorId);
        project.setUpdatedBy(operatorId);
        project.setDeleted(0);
        project.setVersion(0);

        try {
            projectMapper.insert(project);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目编码已存在，请更换后重试");
        }

        insertStartup(project.getId(), request.startup(), operatorId, now);
        ProjectMemberEntity member = new ProjectMemberEntity();
        member.setProjectId(project.getId());
        member.setUserId(operatorId);
        member.setProjectRole(ROLE_OWNER);
        member.setCreatedAt(now);
        member.setCreatedBy(operatorId);
        memberMapper.insert(member);

        auditRecorder.record(
                "PROJECT",
                String.valueOf(project.getId()),
                project.getId(),
                "CREATE",
                "SUCCESS",
                project.getProjectCode() + " " + project.getName()
        );
        return project.getId();
    }

    /**
     * 修改进行中项目的资料和默认地图参数。
     *
     * @param projectId 项目标识
     * @param request   项目资料与默认地图参数
     */
    @Transactional
    public void update(long projectId, ProjectSaveRequest request) {
        ProjectEntity project = requireManageableProject(projectId);
        requireActive(project);
        String projectCode = normalizeProjectCode(request.projectCode());
        ensureProjectCodeAvailable(projectCode, projectId);
        long operatorId = currentUserId();
        LocalDateTime now = utcNow();

        project.setProjectCode(projectCode);
        applyProjectRequest(project, request);
        project.setUpdatedAt(now);
        project.setUpdatedBy(operatorId);
        if (projectMapper.updateById(project) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目已被其他用户修改，请刷新后重试");
        }
        updateStartup(projectId, request.startup(), operatorId, now);
        auditRecorder.record(
                "PROJECT",
                String.valueOf(projectId),
                projectId,
                "UPDATE",
                "SUCCESS",
                project.getProjectCode() + " " + project.getName()
        );
    }

    /**
     * 将进行中的项目归档为只读状态。
     *
     * @param projectId 项目标识
     */
    @Transactional
    public void archive(long projectId) {
        ProjectEntity project = requireManageableProject(projectId);
        requireActive(project);
        project.setStatus(STATUS_ARCHIVED);
        project.setUpdatedAt(utcNow());
        project.setUpdatedBy(currentUserId());
        if (projectMapper.updateById(project) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目状态已发生变化，请刷新后重试");
        }
        auditRecorder.record(
                "PROJECT",
                String.valueOf(projectId),
                projectId,
                "ARCHIVE",
                "SUCCESS",
                project.getProjectCode() + " " + project.getName()
        );
    }

    /**
     * 逻辑删除项目，保留项目关联数据和审计记录，便于后续追溯。
     *
     * @param projectId 项目标识
     */
    @Transactional
    public void delete(long projectId) {
        ProjectEntity project = requireManageableProject(projectId);
        if (projectMapper.deleteById(projectId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目状态已发生变化，请刷新后重试");
        }
        auditRecorder.record(
                "PROJECT",
                String.valueOf(projectId),
                projectId,
                "DELETE",
                "SUCCESS",
                project.getProjectCode() + " " + project.getName()
        );
    }

    /**
     * 查询项目默认地图启动参数。
     *
     * @param projectId 项目标识
     * @return 默认地图启动参数
     */
    public ProjectStartupVO getStartup(long projectId) {
        requireAccessibleProject(projectId);
        return toStartupVO(requireStartup(projectId));
    }

    /**
     * 单独修改进行中项目的默认地图启动参数。
     *
     * @param projectId 项目标识
     * @param request   默认地图启动参数
     */
    @Transactional
    public void updateStartup(long projectId, ProjectStartupSaveRequest request) {
        ProjectEntity project = requireManageableProject(projectId);
        requireActive(project);
        updateStartup(projectId, request, currentUserId(), utcNow());
        auditRecorder.record(
                "PROJECT_STARTUP",
                String.valueOf(projectId),
                projectId,
                "UPDATE",
                "SUCCESS",
                project.getProjectCode() + " 默认地图参数"
        );
    }

    private void applyProjectRequest(ProjectEntity project, ProjectSaveRequest request) {
        project.setName(request.name().trim());
        project.setDescription(trimToNull(request.description()));
        project.setLocationName(request.locationName().trim());
    }

    private void insertStartup(
            long projectId,
            ProjectStartupSaveRequest request,
            long operatorId,
            LocalDateTime now
    ) {
        ProjectStartupEntity startup = new ProjectStartupEntity();
        startup.setProjectId(projectId);
        startup.setMapProvider(MAP_PROVIDER_AMAP);
        applyStartupRequest(startup, request);
        startup.setCreatedAt(now);
        startup.setUpdatedAt(now);
        startup.setCreatedBy(operatorId);
        startup.setUpdatedBy(operatorId);
        startup.setVersion(0);
        startupMapper.insert(startup);
    }

    private void updateStartup(
            long projectId,
            ProjectStartupSaveRequest request,
            long operatorId,
            LocalDateTime now
    ) {
        ProjectStartupEntity startup = requireStartup(projectId);
        applyStartupRequest(startup, request);
        startup.setUpdatedAt(now);
        startup.setUpdatedBy(operatorId);
        if (startupMapper.updateById(startup) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "默认地图参数已被其他用户修改，请刷新后重试");
        }
    }

    private void applyStartupRequest(ProjectStartupEntity startup, ProjectStartupSaveRequest request) {
        startup.setMapMode(request.mapMode().toUpperCase(Locale.ROOT));
        startup.setCenterLng(request.centerLng());
        startup.setCenterLat(request.centerLat());
        startup.setZoomLevel(request.zoomLevel());
        startup.setPitch(request.pitch());
        startup.setRotation(request.rotation());
    }

    private ProjectEntity requireAccessibleProject(long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或已删除");
        }
        if (!isSuperAdmin() && currentMemberRole(projectId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在或无权访问");
        }
        return project;
    }

    private ProjectEntity requireManageableProject(long projectId) {
        ProjectEntity project = requireAccessibleProject(projectId);
        if (!isSuperAdmin()) {
            String role = currentMemberRole(projectId);
            if (!ROLE_OWNER.equals(role) && !ROLE_MANAGER.equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "当前项目角色不允许修改项目");
            }
        }
        return project;
    }

    private ProjectStartupEntity requireStartup(long projectId) {
        ProjectStartupEntity startup = startupMapper.selectById(projectId);
        if (startup == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目默认地图参数不存在");
        }
        return startup;
    }

    private void requireActive(ProjectEntity project) {
        if (!STATUS_ACTIVE.equals(project.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已归档项目为只读状态，不能继续修改");
        }
    }

    private void ensureProjectCodeAvailable(String projectCode, Long excludedProjectId) {
        LambdaQueryWrapper<ProjectEntity> query = Wrappers.<ProjectEntity>lambdaQuery()
                .eq(ProjectEntity::getProjectCode, projectCode);
        if (excludedProjectId != null) {
            query.ne(ProjectEntity::getId, excludedProjectId);
        }
        if (projectMapper.selectCount(query) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目编码已存在，请更换后重试");
        }
    }

    private Map<Long, ProjectStartupEntity> loadStartupMap(Collection<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return startupMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectStartupEntity::getProjectId, Function.identity()));
    }

    private Map<Long, String> loadCurrentMemberRoleMap(Collection<Long> projectIds) {
        if (projectIds.isEmpty() || isSuperAdmin()) {
            return Map.of();
        }
        return memberMapper.selectList(Wrappers.<ProjectMemberEntity>lambdaQuery()
                        .eq(ProjectMemberEntity::getUserId, currentUserId())
                        .in(ProjectMemberEntity::getProjectId, projectIds))
                .stream()
                .collect(Collectors.toMap(ProjectMemberEntity::getProjectId, ProjectMemberEntity::getProjectRole));
    }

    private String currentMemberRole(long projectId) {
        if (isSuperAdmin()) {
            return "SUPER_ADMIN";
        }
        ProjectMemberEntity member = memberMapper.selectOne(Wrappers.<ProjectMemberEntity>lambdaQuery()
                .eq(ProjectMemberEntity::getProjectId, projectId)
                .eq(ProjectMemberEntity::getUserId, currentUserId()));
        return member == null ? null : member.getProjectRole();
    }

    private ProjectVO toVO(ProjectEntity project, ProjectStartupEntity startup, String memberRole) {
        boolean canManageObject = isSuperAdmin()
                || ROLE_OWNER.equals(memberRole)
                || ROLE_MANAGER.equals(memberRole);
        boolean active = STATUS_ACTIVE.equals(project.getStatus());
        boolean editable = active
                && canManageObject
                && (isSuperAdmin() || StpUtil.hasPermission("project:project:edit"));
        boolean archivable = active
                && canManageObject
                && (isSuperAdmin() || StpUtil.hasPermission("project:project:archive"));
        boolean deletable = canManageObject
                && (isSuperAdmin() || StpUtil.hasPermission("project:project:delete"));
        return new ProjectVO(
                String.valueOf(project.getId()),
                project.getProjectCode(),
                project.getName(),
                project.getDescription(),
                project.getLocationName(),
                project.getStatus(),
                memberRole,
                editable,
                archivable,
                deletable,
                startup == null ? null : toStartupVO(startup),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectStartupVO toStartupVO(ProjectStartupEntity startup) {
        return new ProjectStartupVO(
                startup.getMapProvider(),
                startup.getMapMode(),
                startup.getCenterLng(),
                startup.getCenterLat(),
                startup.getZoomLevel(),
                startup.getPitch(),
                startup.getRotation()
        );
    }

    private String normalizeProjectCode(String projectCode) {
        return projectCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_ACTIVE.equals(normalized) && !STATUS_ARCHIVED.equals(normalized)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "项目状态只能为ACTIVE或ARCHIVED");
        }
        return normalized;
    }

    private void validatePage(long pageNo, long pageSize) {
        if (pageNo < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "页码不能小于1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "每页数量必须为1至100");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isSuperAdmin() {
        return StpUtil.hasRole("SUPER_ADMIN");
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
