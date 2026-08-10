package com.microservices.gateway.filter;

import com.microservices.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayTokenRelayFilter implements GlobalFilter, Ordered {

    public static final String GATEWAY_HEADER = "X-Gateway-Token";

    private final GatewayProperties gatewayProperties;

    public GatewayTokenRelayFilter(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = gatewayProperties.internalToken();
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(GATEWAY_HEADER, token)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
