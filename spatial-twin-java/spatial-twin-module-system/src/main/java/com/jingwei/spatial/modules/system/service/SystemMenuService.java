package com.jingwei.spatial.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingwei.spatial.common.exception.BusinessException;
import com.jingwei.spatial.common.exception.ErrorCode;
import com.jingwei.spatial.modules.system.dto.MenuSaveRequest;
import com.jingwei.spatial.modules.system.entity.MenuEntity;
import com.jingwei.spatial.modules.system.entity.PermissionEntity;
import com.jingwei.spatial.modules.system.mapper.MenuMapper;
import com.jingwei.spatial.modules.system.mapper.PermissionMapper;
import com.jingwei.spatial.modules.system.vo.MenuVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台菜单树管理服务。
 */
@Service
public class SystemMenuService {

    private final MenuMapper menuMapper;
    private final PermissionMapper permissionMapper;
    private final SystemAuditService auditService;
    private final AccessMetadataService accessMetadataService;

    public SystemMenuService(
            MenuMapper menuMapper,
            PermissionMapper permissionMapper,
            SystemAuditService auditService,
            AccessMetadataService accessMetadataService
    ) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.auditService = auditService;
        this.accessMetadataService = accessMetadataService;
    }

    /**
     * 查询完整菜单树。
     *
     * @return 按排序号组织的菜单树
     */
    public List<MenuVO> listTree() {
        List<MenuEntity> menus = menuMapper.selectList(Wrappers.<MenuEntity>lambdaQuery()
                .orderByAsc(MenuEntity::getSortOrder, MenuEntity::getId));
        return buildTree(0L, groupByParent(menus));
    }

    /**
     * 查询当前登录用户可在导航中看到的菜单树。
     *
     * @return 已按可见状态和当前用户权限过滤的菜单树
     */
    public List<MenuVO> listCurrentUserTree() {
        long userId = currentUserId();
        boolean superAdmin = accessMetadataService.getRoleCodes(userId).contains("SUPER_ADMIN");
        Set<String> permissionCodes = new HashSet<>(accessMetadataService.getPermissionCodes(userId));
        List<MenuEntity> visibleMenus = menuMapper.selectList(Wrappers.<MenuEntity>lambdaQuery()
                .eq(MenuEntity::getVisible, 1)
                .orderByAsc(MenuEntity::getSortOrder, MenuEntity::getId));
        return buildAuthorizedTree(0L, groupByParent(visibleMenus), superAdmin, permissionCodes);
    }

    /**
     * 创建菜单，并确保其权限码存在于权限表。
     *
     * @param request 菜单数据
     * @return 新菜单标识
     */
    @Transactional
    public long create(MenuSaveRequest request) {
        validateMenuRequest(null, request);
        MenuEntity entity = new MenuEntity();
        apply(entity, request);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(currentUserId());
        entity.setUpdatedBy(currentUserId());
        entity.setDeleted(0);
        entity.setVersion(0);
        menuMapper.insert(entity);
        ensurePermission(entity);
        auditService.record("MENU", String.valueOf(entity.getId()), "CREATE", "SUCCESS", entity.getName());
        return entity.getId();
    }

    /**
     * 修改菜单，禁止形成父子循环。
     *
     * @param menuId  菜单标识
     * @param request 菜单数据
     */
    @Transactional
    public void update(long menuId, MenuSaveRequest request) {
        MenuEntity entity = requireMenu(menuId);
        validateMenuRequest(menuId, request);
        apply(entity, request);
        entity.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        entity.setUpdatedBy(currentUserId());
        menuMapper.updateById(entity);
        ensurePermission(entity);
        auditService.record("MENU", String.valueOf(menuId), "UPDATE", "SUCCESS", entity.getName());
    }

    /**
     * 删除没有子节点的菜单。
     *
     * @param menuId 菜单标识
     */
    @Transactional
    public void delete(long menuId) {
        MenuEntity entity = requireMenu(menuId);
        Long childCount = menuMapper.selectCount(Wrappers.<MenuEntity>lambdaQuery()
                .eq(MenuEntity::getParentId, menuId));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先删除当前菜单的子节点");
        }
        menuMapper.deleteById(menuId);
        auditService.record("MENU", String.valueOf(menuId), "DELETE", "SUCCESS", entity.getName());
    }

    private void validateMenuRequest(Long menuId, MenuSaveRequest request) {
        if (request.visible() != 0 && request.visible() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "菜单可见状态只能为0或1");
        }
        long parentId = normalizeParentId(request.parentId());
        if (menuId != null && menuId == parentId) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "菜单不能选择自身作为上级");
        }
        if (parentId != 0) {
            requireMenu(parentId);
        }
        if (menuId != null && containsDescendant(menuId, parentId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "不能将菜单移动到自己的子节点下");
        }
    }

    private boolean containsDescendant(long menuId, long candidateParentId) {
        long currentId = candidateParentId;
        while (currentId != 0) {
            if (currentId == menuId) {
                return true;
            }
            MenuEntity current = menuMapper.selectById(currentId);
            if (current == null) {
                return false;
            }
            currentId = normalizeParentId(current.getParentId());
        }
        return false;
    }

    private void apply(MenuEntity entity, MenuSaveRequest request) {
        entity.setParentId(normalizeParentId(request.parentId()));
        entity.setName(request.name().trim());
        entity.setPath(request.path().trim());
        entity.setComponent(trimToNull(request.component()));
        entity.setIcon(trimToNull(request.icon()));
        entity.setSortOrder(request.sortOrder());
        entity.setPermissionCode(trimToNull(request.permissionCode()));
        entity.setVisible(request.visible());
    }

    private void ensurePermission(MenuEntity menu) {
        if (menu.getPermissionCode() == null) {
            return;
        }
        PermissionEntity existing = permissionMapper.selectOne(Wrappers.<PermissionEntity>lambdaQuery()
                .eq(PermissionEntity::getCode, menu.getPermissionCode()));
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PermissionEntity permission = new PermissionEntity();
        permission.setCode(menu.getPermissionCode());
        permission.setName(menu.getName());
        permission.setResourceType("MENU");
        permission.setDescription("由菜单管理自动创建");
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        permission.setCreatedBy(currentUserId());
        permission.setUpdatedBy(currentUserId());
        permission.setDeleted(0);
        permission.setVersion(0);
        permissionMapper.insert(permission);
    }

    private MenuEntity requireMenu(long menuId) {
        MenuEntity entity = menuMapper.selectById(menuId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜单不存在或已删除");
        }
        return entity;
    }

    private List<MenuVO> buildTree(long parentId, Map<Long, List<MenuEntity>> childrenByParent) {
        return childrenByParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(MenuEntity::getSortOrder).thenComparing(MenuEntity::getId))
                .map(menu -> toMenuVO(menu, buildTree(menu.getId(), childrenByParent)))
                .toList();
    }

    private List<MenuVO> buildAuthorizedTree(
            long parentId,
            Map<Long, List<MenuEntity>> childrenByParent,
            boolean superAdmin,
            Set<String> permissionCodes
    ) {
        return childrenByParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(MenuEntity::getSortOrder).thenComparing(MenuEntity::getId))
                .map(menu -> {
                    List<MenuEntity> childEntities = childrenByParent.getOrDefault(menu.getId(), List.of());
                    List<MenuVO> children = buildAuthorizedTree(
                            menu.getId(),
                            childrenByParent,
                            superAdmin,
                            permissionCodes
                    );
                    if (!canAccess(menu, superAdmin, permissionCodes)
                            || (!childEntities.isEmpty() && children.isEmpty())) {
                        return null;
                    }
                    return toMenuVO(menu, children);
                })
                .filter(menu -> menu != null)
                .toList();
    }

    private Map<Long, List<MenuEntity>> groupByParent(List<MenuEntity> menus) {
        Map<Long, List<MenuEntity>> childrenByParent = new LinkedHashMap<>();
        for (MenuEntity menu : menus) {
            childrenByParent.computeIfAbsent(normalizeParentId(menu.getParentId()), key -> new ArrayList<>()).add(menu);
        }
        return childrenByParent;
    }

    private boolean canAccess(MenuEntity menu, boolean superAdmin, Set<String> permissionCodes) {
        return menu.getPermissionCode() == null
                || superAdmin
                || permissionCodes.contains(menu.getPermissionCode());
    }

    private MenuVO toMenuVO(MenuEntity menu, List<MenuVO> children) {
        return new MenuVO(
                menu.getId(),
                normalizeParentId(menu.getParentId()),
                menu.getName(),
                menu.getPath(),
                menu.getComponent(),
                menu.getIcon(),
                menu.getSortOrder(),
                menu.getPermissionCode(),
                menu.getVisible(),
                children
        );
    }

    private long currentUserId() {
        return Long.parseLong(String.valueOf(StpUtil.getLoginId()));
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
