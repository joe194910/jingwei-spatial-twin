package com.jingwei.spatial.modules.auth.controller;

import com.jingwei.spatial.common.api.ApiResponse;
import com.jingwei.spatial.modules.auth.dto.LoginRequest;
import com.jingwei.spatial.modules.auth.service.AuthService;
import com.jingwei.spatial.modules.auth.vo.CurrentUserVO;
import com.jingwei.spatial.modules.auth.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录、退出和当前用户接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success("已安全退出", null);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserVO> currentUser() {
        return ApiResponse.success(authService.currentUser());
    }
}
