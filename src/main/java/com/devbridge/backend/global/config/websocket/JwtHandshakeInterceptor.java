package com.devbridge.backend.global.config.websocket;

import com.devbridge.backend.domain.user.entity.JobRole;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst(WebSocketContract.TOKEN_QUERY_PARAM);

        if (token == null || !jwtTokenProvider.validateToken(token) || isBlacklisted(token)) {
            return true;
        }

        String employeeId = jwtTokenProvider.extractEmployeeId(token);
        attributes.put("employeeId", employeeId);

        String jobRole = userRepository.findByEmployeeId(employeeId)
                .map(User::getJobRole)
                .map(JobRole::name)
                .map(String::toLowerCase)
                .orElse(JobRole.NEWCOMER.name().toLowerCase());
        attributes.put("jobRole", jobRole);

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
        } catch (RedisConnectionFailureException e) {
            return false;
        }
    }
}
