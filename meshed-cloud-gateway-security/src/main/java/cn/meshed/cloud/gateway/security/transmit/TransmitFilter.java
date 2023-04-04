package cn.meshed.cloud.gateway.security.transmit;

import cn.dev33.satoken.stp.StpUtil;
import cn.meshed.cloud.gateway.constant.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h1>向微服务传递数据</h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransmitFilter implements GlobalFilter, Ordered {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange mutableExchange = exchange.mutate().build();
        if (StpUtil.isLogin()){
            ServerHttpRequest mutableReq = exchange.getRequest().mutate()
                    .header(GatewayConstant.LOGIN_ID, (String) StpUtil.getLoginId())
                    .build();
            mutableExchange = exchange.mutate().request(mutableReq).build();
        }

        return chain.filter(mutableExchange);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }
}

