package com.jingwei.spatial.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.system.dto.RoleSaveRequest;
import com.jingwei.spatial.modules.system.service.SystemRoleService;
import com.jingwei.spatial.modules.system.vo.RoleVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色与菜单权限勾选管理接口。
 */
@RestController
@RequestMapping("/api/v1/system/roles")
public class SystemRoleController {

    private final SystemRoleService roleService;

    public SystemRoleController(SystemRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @SaCheckPermission("system:role:list")
    public ApiResponse<List<RoleVO>> list() {
        return ApiResponse.success(roleService.list());
    }

    @PostMapping
    @SaCheckPermission("system:role:add")
    public ApiResponse<Long> create(@Valid @RequestBody RoleSaveRequest request) {
        return ApiResponse.success("角色创建成功", roleService.create(request));
    }

    @PutMapping("/{roleId}")
    @SaCheckPermission("system:role:edit")
    public ApiResponse<Void> update(
            @PathVariable long roleId,
            @Valid @RequestBody RoleSaveRequest request
    ) {
        roleService.update(roleId, request);
        return ApiResponse.success("角色修改成功", null);
    }

    @DeleteMapping("/{roleId}")
    @SaCheckPermission("system:role:delete")
    public ApiResponse<Void> delete(@PathVariable long roleId) {
        roleService.delete(roleId);
        return ApiResponse.success("角色删除成功", null);
    }
}
