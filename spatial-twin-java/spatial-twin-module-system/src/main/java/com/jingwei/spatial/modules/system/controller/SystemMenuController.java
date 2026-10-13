package com.jingwei.spatial.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.system.dto.MenuSaveRequest;
import com.jingwei.spatial.modules.system.service.SystemMenuService;
import com.jingwei.spatial.modules.system.vo.MenuVO;
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
 * 菜单树和权限码管理接口。
 */
@RestController
@RequestMapping("/api/v1/system/menus")
public class SystemMenuController {

    private final SystemMenuService menuService;

    public SystemMenuController(SystemMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @SaCheckPermission("system:menu:list")
    public ApiResponse<List<MenuVO>> listTree() {
        return ApiResponse.success(menuService.listTree());
    }

    @PostMapping
    @SaCheckPermission("system:menu:add")
    public ApiResponse<Long> create(@Valid @RequestBody MenuSaveRequest request) {
        return ApiResponse.success("菜单创建成功", menuService.create(request));
    }

    @PutMapping("/{menuId}")
    @SaCheckPermission("system:menu:edit")
    public ApiResponse<Void> update(
            @PathVariable long menuId,
            @Valid @RequestBody MenuSaveRequest request
    ) {
        menuService.update(menuId, request);
        return ApiResponse.success("菜单修改成功", null);
    }

    @DeleteMapping("/{menuId}")
    @SaCheckPermission("system:menu:delete")
    public ApiResponse<Void> delete(@PathVariable long menuId) {
        menuService.delete(menuId);
        return ApiResponse.success("菜单删除成功", null);
    }
}
