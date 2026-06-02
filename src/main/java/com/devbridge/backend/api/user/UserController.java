package com.devbridge.backend.api.user;

import com.devbridge.backend.domain.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserAPI {

    @Override
    public ResponseEntity<UserResponse> getUser(String id) {
        UserResponse response = UserResponse.builder()
                .id(id)
                .employeeId("EMP-12345")
                .email("user@devbridge.com")
                .name("홍길동")
                .department("개발팀")
                .position("선임")
                .systemRole("USER")
                .build();
        return ResponseEntity.ok(response);
    }
}
