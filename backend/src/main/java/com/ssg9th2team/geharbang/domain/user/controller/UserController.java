package com.ssg9th2team.geharbang.domain.user.controller;

import com.ssg9th2team.geharbang.domain.auth.dto.UserResponse;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import com.ssg9th2team.geharbang.domain.user.service.UserService;
import com.ssg9th2team.geharbang.domain.user.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 현재 로그인한 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<Void> updateUserProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        String email = authentication.getName();
        userService.updateUserProfile(email, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(Authentication authentication,
            @RequestBody DeleteAccountRequest deleteAccountRequest) {
        String email = authentication.getName();
        userService.deleteUser(email, deleteAccountRequest);
        return ResponseEntity.noContent().build();
    }
}
