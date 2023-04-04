package cn.meshed.cloud.gateway.security.permission;

import java.util.List;
import java.util.Map;

/**
 * <h1></h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
public interface PermissionService {

    /**
     * 鉴权MAP
     * @return 权限MAP
     */
    Map<String,String> getPermissionMap();

    /**
     * 匿名URL
     * @return 匿名URL
     */
    List<String> getAnonymousUrls();

    /**
     * 返回指定账号id所拥有的权限码集合
     *
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return 该账号id具有的权限码集合
     */
    List<String> getPermissionList(Object loginId, String loginType);

    /**
     * 返回指定账号id所拥有的角色标识集合
     *
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return 该账号id具有的角色标识集合
     */
    List<String> getRoleList(Object loginId, String loginType);
}
