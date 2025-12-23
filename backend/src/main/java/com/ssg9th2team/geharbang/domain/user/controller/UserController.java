package com.ssg9th2team.geharbang.domain.user.controller;

import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;
import com.ssg9th2team.geharbang.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(Authentication authentication, @RequestBody DeleteAccountRequest deleteAccountRequest) {
        String email = authentication.getName();
        userService.deleteUser(email, deleteAccountRequest);
        return ResponseEntity.noContent().build();
    }
}
