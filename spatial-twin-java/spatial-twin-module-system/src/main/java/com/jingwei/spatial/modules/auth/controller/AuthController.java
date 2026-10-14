package com.jingwei.spatial.modules.auth.controller;

import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.auth.dto.LoginRequest;
import com.jingwei.spatial.modules.auth.service.AuthService;
import com.jingwei.spatial.modules.auth.vo.CurrentUserVO;
import com.jingwei.spatial.modules.auth.vo.LoginVO;
import com.jingwei.spatial.modules.system.service.SystemMenuService;
import com.jingwei.spatial.modules.system.vo.MenuVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录、退出和当前用户接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final SystemMenuService systemMenuService;

    public AuthController(AuthService authService, SystemMenuService systemMenuService) {
        this.authService = authService;
        this.systemMenuService = systemMenuService;
    }

    /**
     * 使用账号密码建立登录会话。
     *
     * @param request 登录信息
     * @return 登录令牌和当前用户权限
     */
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    /**
     * 注销当前登录会话。
     *
     * @return 空响应
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("已安全退出", null);
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户及角色权限
     */
    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> currentUser() {
        return ApiResponse.success(authService.currentUser());
    }

    /**
     * 查询当前登录用户可见的导航菜单。
     *
     * @return 已按显隐和角色权限过滤的菜单树
     */
    @GetMapping("/menus")
    public ApiResponse<List<MenuVO>> currentMenus() {
        return ApiResponse.success(systemMenuService.listCurrentUserTree());
    }
}
