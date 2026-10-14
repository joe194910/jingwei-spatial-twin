package com.jingwei.spatial.modules.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.common.api.PageResult;
import com.jingwei.spatial.modules.project.dto.ProjectSaveRequest;
import com.jingwei.spatial.modules.project.dto.ProjectStartupSaveRequest;
import com.jingwei.spatial.modules.project.service.ProjectService;
import com.jingwei.spatial.modules.project.vo.ProjectStartupVO;
import com.jingwei.spatial.modules.project.vo.ProjectVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 园区项目和默认地图启动参数接口。
 */
@Validated
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 分页查询当前用户可访问的项目。
     *
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @param keyword  可选关键词
     * @param status   可选项目状态
     * @return 项目分页
     */
    @GetMapping
    @SaCheckPermission("project:project:list")
    public ApiResponse<PageResult<ProjectVO>> page(
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "12") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(projectService.page(pageNo, pageSize, keyword, status));
    }

    /**
     * 查询一个可访问项目的详情。
     *
     * @param projectId 项目标识
     * @return 项目详情
     */
    @GetMapping("/{projectId}")
    @SaCheckPermission("project:project:list")
    public ApiResponse<ProjectVO> get(@PathVariable long projectId) {
        return ApiResponse.success(projectService.get(projectId));
    }

    /**
     * 创建项目并保存默认地图参数。
     *
     * @param request 项目资料
     * @return 新项目标识
     */
    @PostMapping
    @SaCheckPermission("project:project:add")
    public ApiResponse<String> create(@Valid @RequestBody ProjectSaveRequest request) {
        return ApiResponse.success("项目创建成功", String.valueOf(projectService.create(request)));
    }

    /**
     * 修改项目资料和默认地图参数。
     *
     * @param projectId 项目标识
     * @param request   项目资料
     * @return 空数据成功响应
     */
    @PutMapping("/{projectId}")
    @SaCheckPermission("project:project:edit")
    public ApiResponse<Void> update(
            @PathVariable long projectId,
            @Valid @RequestBody ProjectSaveRequest request
    ) {
        projectService.update(projectId, request);
        return ApiResponse.success("项目修改成功", null);
    }

    /**
     * 归档项目，归档后项目只读。
     *
     * @param projectId 项目标识
     * @return 空数据成功响应
     */
    @PostMapping("/{projectId}/archive")
    @SaCheckPermission("project:project:archive")
    public ApiResponse<Void> archive(@PathVariable long projectId) {
        projectService.archive(projectId);
        return ApiResponse.success("项目归档成功", null);
    }

    /**
     * 逻辑删除项目，审计记录继续保留。
     *
     * @param projectId 项目标识
     * @return 空数据成功响应
     */
    @DeleteMapping("/{projectId}")
    @SaCheckPermission("project:project:delete")
    public ApiResponse<Void> delete(@PathVariable long projectId) {
        projectService.delete(projectId);
        return ApiResponse.success("项目删除成功", null);
    }

    /**
     * 查询项目默认地图启动参数。
     *
     * @param projectId 项目标识
     * @return 默认地图启动参数
     */
    @GetMapping("/{projectId}/startup")
    @SaCheckPermission("project:project:list")
    public ApiResponse<ProjectStartupVO> getStartup(@PathVariable long projectId) {
        return ApiResponse.success(projectService.getStartup(projectId));
    }

    /**
     * 单独修改项目默认地图启动参数。
     *
     * @param projectId 项目标识
     * @param request   默认地图启动参数
     * @return 空数据成功响应
     */
    @PutMapping("/{projectId}/startup")
    @SaCheckPermission("project:project:edit")
    public ApiResponse<Void> updateStartup(
            @PathVariable long projectId,
            @Valid @RequestBody ProjectStartupSaveRequest request
    ) {
        projectService.updateStartup(projectId, request);
        return ApiResponse.success("默认地图参数修改成功", null);
    }
}
