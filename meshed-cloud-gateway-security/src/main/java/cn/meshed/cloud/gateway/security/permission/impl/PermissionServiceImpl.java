package cn.meshed.cloud.gateway.security.permission.impl;

import cn.meshed.cloud.gateway.security.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>权限、角色查询服务</h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@RequiredArgsConstructor
@Service
public class PermissionServiceImpl implements PermissionService {
    /**
     * 鉴权MAP
     *
     * @return 权限MAP
     */
    @Override
    public Map<String, String> getPermissionMap() {
        Map<String,String> permissionMap = new HashMap<>();
        System.out.println("getPermissionMap");

        permissionMap.put("/iam/account/list","iam:account:list");
        permissionMap.put("/iam/role/list","iam:account:role");
        return permissionMap;
    }

    /**
     * 匿名URL
     *
     * @return 匿名URL
     */
    @Override
    public List<String> getAnonymousUrls() {
        return Arrays.asList("/iam/system/list");
    }

    /**
     * 返回指定账号id所拥有的权限码集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 该账号id具有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Arrays.asList("iam:account:list");
    }

    /**
     * 返回指定账号id所拥有的角色标识集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 该账号id具有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Arrays.asList("admin");
    }
}
