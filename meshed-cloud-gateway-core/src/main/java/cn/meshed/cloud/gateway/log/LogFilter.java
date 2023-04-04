package cn.meshed.cloud.gateway.log;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * <h1></h1>
 *
 * @author Vincent Vic
 * @version 1.0
 */
@Component("LogFilter")
public class LogFilter implements GlobalFilter, Ordered {

    Logger logger = LoggerFactory.getLogger(this.getClass());


    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取用户传来的数据类型
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        ServerRequest serverRequest = ServerRequest.create(exchange, messageReaders);
        // 如果是json格式，将body内容转化为object or map 都可
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            Mono<Object> modifiedBody = serverRequest.bodyToMono(Object.class)
                    .flatMap(body -> {
                        recordLog(exchange, body);
                        return Mono.just(body);
                    });

            return getVoidMono(exchange, chain, Object.class, modifiedBody);
        }
        // 如果是表单请求
        else if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
            Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                    // .log("modify_request_mono", Level.INFO)
                    .flatMap(body -> {
                        recordLog(exchange, body);
                        return Mono.just(body);
                    });

            return getVoidMono(exchange, chain, String.class, modifiedBody);
        }
        // 无法兼容的请求，则不读取body，像Get请求这种
        recordLog(exchange, "");
        return chain.filter(exchange.mutate().request(exchange.getRequest()).build());
    }

    /**
     * 参照 ModifyRequestBodyGatewayFilterFactory.java 截取的方法
     *
     * @param exchange
     * @param chain
     * @param outClass
     * @param modifiedBody
     * @return
     */
    private Mono<Void> getVoidMono(ServerWebExchange exchange, GatewayFilterChain chain, Class outClass,
                                   Mono<?> modifiedBody) {
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, outClass);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());

        // the new content type will be computed by bodyInserter
        // and then set in the request decorator
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext())
                // .log("modify_request", Level.INFO)
                .then(Mono.defer(() -> {
                    //由于httpRequest的body 体只能读取一次，所以需要重新构建一个httpRequest保证后续获取body不报错
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                            exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            long contentLength = headers.getContentLength();
                            HttpHeaders httpHeaders = new HttpHeaders();
                            httpHeaders.putAll(super.getHeaders());
                            if (contentLength > 0) {
                                httpHeaders.setContentLength(contentLength);
                            } else {
                                // TODO: this causes a 'HTTP/1.1 411 Length Required' on httpbin.org
                                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                            }
                            return httpHeaders;
                        }

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return outputMessage.getBody();
                        }
                    };
                    return chain.filter(exchange.mutate().request(decorator).build());
                }));
    }

    /**
     * 记录到请求日志中去
     *
     * @param exchange request
     * @param body    请求的body内容
     */
    private void recordLog(ServerWebExchange exchange, Object body) {
        ServerHttpRequest request = exchange.getRequest();
        TreeMap<String,Object> params = new TreeMap<>();
        // 记录要访问的url
        StringBuilder builder = new StringBuilder(" request url: ");
        builder.append(request.getURI().getRawPath());

        // 记录访问的方法
        HttpMethod method = request.getMethod();
        if (null != method) {
            builder.append(", method: ").append(method.name());
        }

        // 记录头部信息
        builder.append(", header { ");
        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            builder.append(entry.getKey()).append(":").append(String.join(",", entry.getValue())).append(",");
        }

        // 记录参数
        builder.append("} param: ");
        // 处理get的请求
        if (null != method && HttpMethod.GET.matches(method.name())) {
            // 记录请求的参数信息 针对GET 请求
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                builder.append(entry.getKey()).append("=").append(String.join(",", entry.getValue())).append(",");
            }
        } else {
            //因为参数后续可能会参与校验   为了后续获取方便，从body中读取参数  将参数保存到exchange的attributes属性中
            builder.append(body);
            try {
                JSONObject jsonObject = (JSONObject)JSONObject.toJSON(body);
                params.putAll(jsonObject.getInnerMap());
            } catch (Exception e){
                //忽略
            }

            exchange.getAttributes().put("CacheRequestFilter",params);
        }
        logger.info(builder.toString());
    }

    @Override
    public int getOrder() {
        //order 配置的是拦截器触发的顺序 ，需要该全局的拦截器在其他拦截器之前，数值越小越靠前
        return 10;
    }
}
