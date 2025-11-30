package com.ktb.community.util;

import com.ktb.community.entity.CustomUserDetails;
import com.ktb.community.entity.Role;
import com.ktb.community.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class JWTFilter implements WebFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 🔥 WebSocket 연결은 JWT 필터 통과시켜 버린다
        if (path.startsWith("/v1/chat/connect")) {
            return chain.filter(exchange);
        }

        // 헤더에서 access키에 담긴 토큰을 꺼냄
        String accessToken = exchange.getRequest().getHeaders().getFirst("access");

        // 토큰이 없다면 다음 필터로 넘김
        if (accessToken == null) {
            return chain.filter(exchange);
        }

        // 토큰 만료 여부 확인, 만료시 다음 필터로 넘기지 않음
        try {
            if (jwtUtil.isExpired(accessToken)) {
                return unauthorized(exchange, "access token expired");
            }
        } catch (ExpiredJwtException e) {
            return unauthorized(exchange, "access token expired");
        }

        // 토큰이 access인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(accessToken);

        if (!"access".equals(category)) {
            return unauthorized(exchange, "invalid access token");
        }

        // userID, role 값을 획득
        Long userId = jwtUtil.getID(accessToken);
        String role = jwtUtil.getRole(accessToken);

        User user = User.builder()
                .id(userId)
                .role(Role.valueOf(role))
                .build();

        CustomUserDetails customUserDetails = new CustomUserDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.TEXT_PLAIN);

        var buffer = response.bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
