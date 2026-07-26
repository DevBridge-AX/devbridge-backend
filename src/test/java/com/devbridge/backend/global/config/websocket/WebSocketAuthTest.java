package com.devbridge.backend.global.config.websocket;

import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketAuthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 계약 검증 테스트.
     *
     * <p>기대값을 {@link WebSocketContract} 상수가 아니라 <b>리터럴로 직접</b> 적는다.
     * 상수를 참조하면 값을 바꿔도 테스트가 같이 따라 바뀌어 아무것도 못 잡는다.
     * 이 값들은 운영 nginx의 location 블록과 프론트엔드에도 사본이 있어, 백엔드만 바꾸면
     * 로컬은 전부 통과하면서 운영만 끊긴다(2026-06-25 장애 사례).
     */
    @Test
    @DisplayName("불변 계약: WS 경로·close code·토큰 파라미터명이 변경되지 않았다")
    void webSocketContract_shouldMatchExternalContract() {
        assertThat(WebSocketContract.CHAT_ENDPOINT).isEqualTo("/ws/chat");
        assertThat(WebSocketContract.PATH_PATTERN).isEqualTo("/ws/**");
        assertThat(WebSocketContract.TOKEN_QUERY_PARAM).isEqualTo("token");
        assertThat(WebSocketContract.CLOSE_CODE_AUTH_FAILED).isEqualTo(4401);
        assertThat(WebSocketContract.CLOSE_REASON_AUTH_FAILED).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("유효하지 않은 토큰 → 연결 후 close(4401) 수신")
    void connectWithInvalidToken_shouldReceiveClose4401() throws Exception {
        CompletableFuture<Integer> closeCodeFuture = new CompletableFuture<>();
        CompletableFuture<String> closeReasonFuture = new CompletableFuture<>();

        URI uri = URI.create("ws://localhost:" + port + "/ws/chat?token=invalid-token-value");

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        closeCodeFuture.complete(statusCode);
                        closeReasonFuture.complete(reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        closeCodeFuture.completeExceptionally(error);
                    }
                })
                .get(5, TimeUnit.SECONDS);

        Integer closeCode = closeCodeFuture.get(5, TimeUnit.SECONDS);
        String closeReason = closeReasonFuture.get(5, TimeUnit.SECONDS);

        assertThat(closeCode).isEqualTo(4401);
        assertThat(closeReason).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("토큰 없이 연결 → close(4401) 수신")
    void connectWithoutToken_shouldReceiveClose4401() throws Exception {
        CompletableFuture<Integer> closeCodeFuture = new CompletableFuture<>();
        CompletableFuture<String> closeReasonFuture = new CompletableFuture<>();

        URI uri = URI.create("ws://localhost:" + port + "/ws/chat");

        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        closeCodeFuture.complete(statusCode);
                        closeReasonFuture.complete(reason);
                        return CompletableFuture.completedFuture(null);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        closeCodeFuture.completeExceptionally(error);
                    }
                })
                .get(5, TimeUnit.SECONDS);

        Integer closeCode = closeCodeFuture.get(5, TimeUnit.SECONDS);
        String closeReason = closeReasonFuture.get(5, TimeUnit.SECONDS);

        assertThat(closeCode).isEqualTo(4401);
        assertThat(closeReason).isEqualTo("Authentication required");
    }

    @Test
    @DisplayName("유효한 토큰 → 연결 성공, 정상 종료")
    void connectWithValidToken_shouldSucceed() throws Exception {
        String validToken = jwtTokenProvider.createAccessToken("EMP001", "USER");
        URI uri = URI.create("ws://localhost:" + port + "/ws/chat?token=" + validToken);

        CompletableFuture<Boolean> openFuture = new CompletableFuture<>();

        WebSocket ws = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(uri, new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        openFuture.complete(true);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        openFuture.completeExceptionally(error);
                    }
                })
                .get(5, TimeUnit.SECONDS);

        Boolean opened = openFuture.get(5, TimeUnit.SECONDS);
        assertThat(opened).isTrue();

        ws.sendClose(WebSocket.NORMAL_CLOSURE, "test done").get(5, TimeUnit.SECONDS);
    }
}
