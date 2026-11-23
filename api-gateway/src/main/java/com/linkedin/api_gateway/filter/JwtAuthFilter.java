package com.linkedin.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NullMarked;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

/**
 * JWT Authentication Filter
 *
 * Applied to all routes except /api/v1/auth/**
 *
 * FLOW:
 * 1. Extract JWT from the Authorization header
 * 2. Validate JWT signature and expiration
 * 3. Extract user ID from token claims
 * 4. Attach user ID to request attributes for downstream services
 * 5. Forward request to downstream service
 *
 *
 * If JWT is invalid or expired, return 401 Unauthorized
 */
@Component
@Slf4j
@NullMarked
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config>{
    private final SecretKey signingKey;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        super(Config.class);
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public GatewayFilter apply(Config config){
        return (exchange, chain)->{
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);
            if(authHeader == null || !authHeader.startsWith("Bearer ")){
                log.warn("Missing or invalid Authorization header");
                return unauthorized(exchange);
            }

            String token = authHeader.substring(7);
            try{
                Claims claims = extractClaims(token);
                String userId = claims.get("userId", String.class);
                String email = claims.getSubject();
                if (userId == null || userId.isBlank() || email == null || email.isBlank()) {
                    return unauthorized(exchange);
                }

                log.info("User {} authenticated successfully", userId);
                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(r->r.header("X-User-Id",userId)
                                .header("X-User-Email",email))
                        .build();
                return chain.filter(modifiedExchange);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return unauthorized(exchange);
            }
        };
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange){
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
