package cn.meshed.cloud.gateway.security.satoken;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.meshed.cloud.gateway.security.permission.PermissionService;
import cn.meshed.cloud.iam.rbac.data.IdentityAuthenticationDTO;
import cn.meshed.cloud.iam.rbac.enums.AccessModeEnum;
import com.alibaba.cola.dto.Response;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.router.SaHttpMethod;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <h1>[Sa-Token 权限认证] 配置类 </h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@RequiredArgsConstructor
@Configuration
public class SaTokenConfigure {

    private final PermissionService permissionService;

    // 注册 Sa-Token全局过滤器
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        List<IdentityAuthenticationDTO> identityAuthentications = permissionService.getIdentityAuthentications();
        List<String> anonymousUrls = identityAuthentications.stream()
                .filter(identity -> AccessModeEnum.ANONYMOUS.equals(identity.getAccessMode()))
                .map(IdentityAuthenticationDTO::getUri).collect(Collectors.toList());
        Map<String, String> permissionMap = identityAuthentications.stream()
                .filter(identity -> AccessModeEnum.EMPOWER.equals(identity.getAccessMode()))
                .collect(Collectors.toMap(IdentityAuthenticationDTO::getUri, IdentityAuthenticationDTO::getAccess));

        return new SaReactorFilter()
                // 拦截地址
                .addInclude("/**")
                // 开放地址
                .addExclude("/favicon.ico","/iam/login/**","/iam/oauth2/**","/actuator/","/actuator/**")
                // 鉴权方法：每次访问进入
                .setAuth(obj -> {
                    System.out.println("xxx");
                    // 登录校验 -- 拦截所有路由，并排除/user/doLogin 用于开放登录
                    if (CollectionUtils.isNotEmpty(anonymousUrls)){
                        SaRouter.match("/**").notMatch(anonymousUrls).check(r -> StpUtil.checkLogin());
                    } else {
                        SaRouter.match("/**").check(r -> StpUtil.checkLogin());
                    }
                    // 权限认证 -- 不同模块, 校验不同权限
                    permissionMap.forEach((key, value) -> SaRouter.match(key, r -> StpUtil.checkPermission(value)));
                })
                // 异常处理方法：每次setAuth函数出现异常时进入
                .setError(e -> {
                    // 设置错误返回格式为JSON
                    ServerWebExchange exchange = SaReactorSyncHolder.getContext();
                    exchange.getResponse().getHeaders().set("Content-Type", "application/json; charset=utf-8");
                    e.printStackTrace();
                    //未登入401，无权限403

                    if (e instanceof NotLoginException){
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return JSONObject.toJSONString(Response.buildFailure("NOT_LOGIN",e.getMessage()));
                    }
                    if (e instanceof NotPermissionException){
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return JSONObject.toJSONString(Response.buildFailure("NOT_PERMISSION",e.getMessage()));
                    }
                    if (e instanceof NotRoleException){
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return JSONObject.toJSONString(Response.buildFailure("NOT_ROLE",e.getMessage()));
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    if (e instanceof RedisConnectionFailureException) {
                        return JSONObject.toJSONString(Response.buildFailure("INTERNAL_SERVER_ERROR",e.getMessage()));
                    }
                    return JSONObject.toJSONString(Response.buildFailure("FORBIDDEN",e.getMessage()));
                })
                .setBeforeAuth(obj -> {
                    // ---------- 设置跨域响应头 ----------
                    SaHolder.getResponse()
                            // 允许指定域访问跨域资源
                            .setHeader("Access-Control-Allow-Origin", "*")
                            // 允许所有请求方式
                            .setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
                            // 有效时间
                            .setHeader("Access-Control-Max-Age", "3600")
                            // 允许的header参数
                            .setHeader("Access-Control-Allow-Headers", "*");

                    // 如果是预检请求，则立即返回到前端
                    SaRouter.match(SaHttpMethod.OPTIONS)
                            .free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
                            .back();
                });
    }


}