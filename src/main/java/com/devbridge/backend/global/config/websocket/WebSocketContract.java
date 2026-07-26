package com.devbridge.backend.global.config.websocket;

import org.springframework.web.socket.CloseStatus;

/**
 * WebSocket 연결 규약 상수.
 *
 * <p>여기 정의된 값은 이 애플리케이션 밖(운영 nginx의 location 블록, 프론트엔드 환경변수)에도
 * 사본이 존재한다. 즉 백엔드만 바꾸면 로컬 테스트는 전부 통과하면서 운영만 조용히 끊긴다.
 * 실제로 2026-06-25 운영 배포에서 nginx에 {@code /ws/} 프록시 설정이 없어 연결이 실패한 사례가 있다.
 *
 * <p>따라서 이 클래스의 값은 <b>임의로 변경해서는 안 된다.</b> 변경이 필요하면 아래를 동시에 반영해야 한다.
 * <ul>
 *   <li>운영 nginx의 {@code location} 블록 (레포 밖, EC2 {@code /etc/nginx/})</li>
 *   <li>프론트엔드 {@code .env}의 {@code VITE_WS_URL} (레포 밖)</li>
 *   <li>프론트엔드 {@code src/composables/useWebSocket.ts}의 close code 분기</li>
 * </ul>
 *
 * <p>값이 바뀌면 {@code WebSocketAuthTest}의 계약 검증 테스트가 실패한다.
 */
public final class WebSocketContract {

    /**
     * WebSocket 경로 접두사. nginx {@code location /ws/} 및 Security 허용 패턴의 기준이다.
     */
    public static final String PATH_PREFIX = "/ws";

    /**
     * 채팅 WebSocket 엔드포인트. 프론트 {@code VITE_WS_URL}이 가리키는 경로다.
     */
    public static final String CHAT_ENDPOINT = PATH_PREFIX + "/chat";

    /**
     * Security 설정에서 WebSocket 경로를 인증 예외로 허용할 때 사용하는 패턴.
     * 인증은 핸드셰이크 인터셉터와 핸들러가 담당한다.
     */
    public static final String PATH_PATTERN = PATH_PREFIX + "/**";

    /**
     * 핸드셰이크 시 JWT를 전달받는 쿼리 파라미터명. 프론트에서 동일한 이름으로 붙여 보낸다.
     */
    public static final String TOKEN_QUERY_PARAM = "token";

    /**
     * 인증 실패 시 사용하는 WebSocket close code. 프론트가 이 값으로 재로그인 분기를 태운다.
     */
    public static final int CLOSE_CODE_AUTH_FAILED = 4401;

    /**
     * 인증 실패 close 사유 문구.
     */
    public static final String CLOSE_REASON_AUTH_FAILED = "Authentication required";

    /**
     * 인증 실패 시 세션 종료에 사용하는 {@link CloseStatus}.
     */
    public static final CloseStatus CLOSE_AUTH_FAILED =
            new CloseStatus(CLOSE_CODE_AUTH_FAILED, CLOSE_REASON_AUTH_FAILED);

    private WebSocketContract() {
    }
}
