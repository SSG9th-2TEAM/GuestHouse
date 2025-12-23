package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;

public interface UserService {
    void deleteUser(String email, DeleteAccountRequest deleteAccountRequest);
}
