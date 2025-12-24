package com.ssg9th2team.geharbang.domain.wishlist.controller;

import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.wishlist.dto.WishlistDto;
import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import com.ssg9th2team.geharbang.domain.wishlist.service.WishlistService;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    // 위치리스트 추가
    @PostMapping
    public ResponseEntity<?> addWishlist(@RequestBody WishlistDto wishlistDto, Authentication authentication) {
        String email = authentication.getName();  // 이메일을 username으로 사용하고 있음
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        wishlistService.addWishlist(userId, wishlistDto.getAccommodationsId());
        return ResponseEntity.ok("Wishlist added.");
    }

    // 위시리스트 삭제
    @DeleteMapping("/{accommodationId}")
    public ResponseEntity<?> removeWishlist(@PathVariable Long accommodationId, Authentication authentication) {
        String email = authentication.getName();
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        wishlistService.removeWishlist(userId, accommodationId);
        return ResponseEntity.ok("Wishlist removed.");
    }

    // 내 위시리스트 조회 (마이페이지용 - 상세 정보 포함)
    @GetMapping
    public ResponseEntity<?> getMyWishlist(Authentication authentication) {
        String email = authentication.getName();
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return ResponseEntity.ok(wishlistService.getMyWishlist(userId));
    }

    // 내 위시리스트 ID 조회 (메인페이지용 - 하트 표시용)
    @GetMapping("/accommodation-ids")
    public ResponseEntity<?> getMyWishlistIds(Authentication authentication) {
        String email = authentication.getName();
        Long userId = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")).getId();
        return ResponseEntity.ok(wishlistService.getMyWishlistAccommodationIds(userId));
    }
}
