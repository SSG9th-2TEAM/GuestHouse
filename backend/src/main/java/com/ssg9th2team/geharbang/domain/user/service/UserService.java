package com.ssg9th2team.geharbang.domain.user.service;

import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.user.dto.DeleteAccountRequest;

public interface UserService {
    void deleteUser(String email, DeleteAccountRequest deleteAccountRequest);

    // 이메일로 사용자 정보 조회
    User getUserByEmail(String email);
}
