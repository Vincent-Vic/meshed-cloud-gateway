package cn.meshed.cloud.gateway.security.transmit;

import cn.dev33.satoken.stp.StpUtil;
import cn.meshed.cloud.exception.security.SysSecurityException;
import cn.meshed.cloud.gateway.constant.GatewayConstant;
import cn.meshed.cloud.security.AccessTokenService;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;

/**
 * <h1>向微服务传递数据</h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@RequiredArgsConstructor
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransmitFilter implements GlobalFilter, Ordered {

    private final AccessTokenService accessTokenService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String payloadStr = null;
        if (StpUtil.isLogin()){
            //签发用户信息安全口令
            payloadStr = JSONObject.toJSONString(StpUtil.getSession().get("user"));
        } else {
            //签发无信息安全口令
            payloadStr = JSONObject.toJSONString(new HashMap<>());
        }
        String sign = accessTokenService.generateToken(payloadStr);
        if (StringUtils.isBlank(sign)){
            throw new SysSecurityException("颁发签名失败");
        }
        //传递信息
        ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                .header(GatewayConstant.SIGN, sign) //签名含登入信息
                .build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();
        return chain.filter(mutableExchange);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }
}

