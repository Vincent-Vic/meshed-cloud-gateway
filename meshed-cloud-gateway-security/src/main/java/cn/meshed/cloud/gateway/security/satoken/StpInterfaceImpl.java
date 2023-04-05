package cn.meshed.cloud.gateway.security.satoken;

/**
 * <h1></h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */

import cn.dev33.satoken.stp.StpInterface;
import cn.meshed.cloud.gateway.security.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <h1>自定义权限验证接口扩展</h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@RequiredArgsConstructor
@Component
public class StpInterfaceImpl implements StpInterface {

    private final PermissionService permissionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        System.out.println(loginType);
        // 返回此 loginId 拥有的权限列表
        return permissionService.getPermissionList(Long.parseLong(String.valueOf(loginId)), loginType);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 返回此 loginId 拥有的角色列表
        return permissionService.getRoleList(Long.parseLong(String.valueOf(loginId)), loginType);
    }

}
