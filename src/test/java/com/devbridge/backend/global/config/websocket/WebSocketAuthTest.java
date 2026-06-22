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
