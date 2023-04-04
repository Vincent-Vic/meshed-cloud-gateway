package cn.meshed.cloud.gateway.log;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * <h1>全局运行日志</h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalElapsedLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //前置逻辑
        StopWatch sw = StopWatch.createStarted();
        String uri = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange).then(Mono.fromRunnable(()->{
            log.info("[{}] elapsed: [{}ms]",uri,sw.getTime(TimeUnit.MILLISECONDS));
        }));
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
