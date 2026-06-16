package com.devbridge.backend.api.internal;

import com.devbridge.backend.domain.user.dto.UserLookupResponse;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.global.common.exception.UserNotFoundException;
import com.devbridge.backend.global.auth.jwt.JwtTokenProvider;
import com.devbridge.backend.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalUserController.class)
@Import(SecurityConfig.class)
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserInternalService userInternalService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Test
    @DisplayName("1. X-Internal-Api-Key가 유효하고 유저가 존재하면 200 OK와 user_id 반환")
    void lookupUser_Success() throws Exception {
        // given
        String email = "test@ssafy.com";
        UserLookupResponse response = new UserLookupResponse("user-uuid-1234");
        when(userInternalService.lookupUserByEmail(email)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/internal/users/lookup")
                        .header("X-Internal-Api-Key", internalApiKey)
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value("user-uuid-1234"));
    }

    @Test
    @DisplayName("2. X-Internal-Api-Key가 유효하지만 유저가 존재하지 않으면 404 NOT FOUND 반환")
    void lookupUser_NotFound() throws Exception {
        // given
        String email = "notfound@ssafy.com";
        when(userInternalService.lookupUserByEmail(email))
                .thenThrow(new UserNotFoundException("User not found with email: " + email));

        // when & then
        mockMvc.perform(get("/internal/users/lookup")
                        .header("X-Internal-Api-Key", internalApiKey)
                        .param("email", email))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("3. X-Internal-Api-Key가 누락되거나 틀리면 403 Forbidden 반환")
    void lookupUser_InvalidApiKey() throws Exception {
        // when & then (틀린 키)
        mockMvc.perform(get("/internal/users/lookup")
                        .header("X-Internal-Api-Key", "invalid-key")
                        .param("email", "test@ssafy.com"))
                .andExpect(status().isForbidden());

        // when & then (누락)
        mockMvc.perform(get("/internal/users/lookup")
                        .param("email", "test@ssafy.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("4. 올바르지 않은 이메일 형식일 경우 400 Bad Request 반환")
    void lookupUser_InvalidEmailFormat() throws Exception {
        // when & then
        mockMvc.perform(get("/internal/users/lookup")
                        .header("X-Internal-Api-Key", internalApiKey)
                        .param("email", "invalid-email-format"))
                .andExpect(status().isBadRequest());
    }
}
