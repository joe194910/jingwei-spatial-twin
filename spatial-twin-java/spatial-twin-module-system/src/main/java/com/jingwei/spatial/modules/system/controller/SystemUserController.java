package com.jingwei.spatial.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.system.dto.PasswordResetRequest;
import com.jingwei.spatial.modules.system.dto.UserSaveRequest;
import com.jingwei.spatial.modules.system.service.SystemUserService;
import com.jingwei.spatial.modules.system.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户与多角色关联管理接口。
 */
@RestController
@RequestMapping("/api/v1/system/users")
public class SystemUserController {

    private final SystemUserService userService;

    public SystemUserController(SystemUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @SaCheckPermission("system:user:list")
    public ApiResponse<List<UserVO>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(userService.list(keyword));
    }

    @PostMapping
    @SaCheckPermission("system:user:add")
    public ApiResponse<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return ApiResponse.success("用户创建成功", userService.create(request));
    }

    @PutMapping("/{userId}")
    @SaCheckPermission("system:user:edit")
    public ApiResponse<Void> update(
            @PathVariable long userId,
            @Valid @RequestBody UserSaveRequest request
    ) {
        userService.update(userId, request);
        return ApiResponse.success("用户修改成功", null);
    }

    @PostMapping("/{userId}/reset-password")
    @SaCheckPermission("system:user:edit")
    public ApiResponse<Void> resetPassword(
            @PathVariable long userId,
            @Valid @RequestBody PasswordResetRequest request
    ) {
        userService.resetPassword(userId, request);
        return ApiResponse.success("密码重置成功，原会话已失效", null);
    }

    @DeleteMapping("/{userId}")
    @SaCheckPermission("system:user:delete")
    public ApiResponse<Void> delete(@PathVariable long userId) {
        userService.delete(userId);
        return ApiResponse.success("用户删除成功", null);
    }
}
