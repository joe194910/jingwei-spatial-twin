package com.jingwei.spatial.security;

import cn.dev33.satoken.stp.StpInterface;
import com.jingwei.spatial.modules.system.service.AccessMetadataService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 将数据库中的用户角色与权限提供给 Sa-Token。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final AccessMetadataService accessMetadataService;

    public StpInterfaceImpl(AccessMetadataService accessMetadataService) {
        this.accessMetadataService = accessMetadataService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return accessMetadataService.getPermissionCodes(Long.parseLong(String.valueOf(loginId)));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return accessMetadataService.getRoleCodes(Long.parseLong(String.valueOf(loginId)));
    }
}
