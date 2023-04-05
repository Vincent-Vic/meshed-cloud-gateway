package cn.meshed.cloud.gateway.security.permission.impl;

import cn.meshed.cloud.gateway.security.permission.PermissionService;
import cn.meshed.cloud.iam.account.UserRpc;
import cn.meshed.cloud.iam.account.query.GrantedAuthorityQry;
import cn.meshed.cloud.iam.rbac.PermissionRpc;
import cn.meshed.cloud.iam.rbac.data.IdentityAuthenticationDTO;
import com.alibaba.cola.dto.MultiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
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

    @DubboReference
    private UserRpc userRpc;

    @DubboReference
    private PermissionRpc permissionRpc;

    /**
     * 鉴权MAP
     *
     * @return 权限MAP
     */
    @Override
    public List<IdentityAuthenticationDTO> getIdentityAuthentications() {
        MultiResponse<IdentityAuthenticationDTO> identityAuthentications = permissionRpc.getIdentityAuthentications();
        System.out.println("getIdentityAuthentications");
        if (identityAuthentications.isSuccess()) {
            return identityAuthentications.getData();
        }
        return Collections.emptyList();
    }

    /**
     * 返回指定账号id所拥有的权限码集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 该账号id具有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Long loginId, String loginType) {
        GrantedAuthorityQry grantedAuthorityQry = new GrantedAuthorityQry();
        grantedAuthorityQry.setAccountId(loginId);
        MultiResponse<String> userGrantedAuthority = userRpc.getUserGrantedAuthority(grantedAuthorityQry);
        if (userGrantedAuthority.isSuccess()){
            return userGrantedAuthority.getData();
        }
        return Collections.emptyList();
    }

    /**
     * 返回指定账号id所拥有的角色标识集合
     *
     * @param loginId   账号id
     * @param loginType 账号类型
     * @return 该账号id具有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Long loginId, String loginType) {
        GrantedAuthorityQry grantedAuthorityQry = new GrantedAuthorityQry();
        grantedAuthorityQry.setAccountId(loginId);
        MultiResponse<String> userGrantedRole = userRpc.getUserGrantedRole(grantedAuthorityQry);
        if (userGrantedRole.isSuccess()) {
            return userGrantedRole.getData();
        }
        return Collections.emptyList();
    }
}
