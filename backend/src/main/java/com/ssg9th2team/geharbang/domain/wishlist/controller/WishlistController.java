package com.ssg9th2team.geharbang.domain.wishlist.controller;

import com.ssg9th2team.geharbang.global.common.annotation.CurrentUser;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.wishlist.dto.WishlistDto;
import com.ssg9th2team.geharbang.domain.wishlist.service.WishlistService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 위시리스트 추가
    @PostMapping
    public ResponseEntity<?> addWishlist(@RequestBody WishlistDto wishlistDto, @CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        wishlistService.addWishlist(user.getId(), wishlistDto.getAccommodationsId());
        return ResponseEntity.ok("Wishlist added.");
    }

    // 위시리스트 삭제
    @DeleteMapping("/{accommodationId}")
    public ResponseEntity<?> removeWishlist(@PathVariable Long accommodationId, @CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        wishlistService.removeWishlist(user.getId(), accommodationId);
        return ResponseEntity.ok("Wishlist removed.");
    }

    // 내 위시리스트 조회 (마이페이지용 - 상세 정보 포함)
    @GetMapping
    public ResponseEntity<?> getMyWishlist(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(wishlistService.getMyWishlist(user.getId()));
    }

    // 내 위시리스트 ID 조회 (메인페이지용 - 하트 표시용)
    @GetMapping("/ids")
    public ResponseEntity<?> getMyWishlistIds(@CurrentUser User user) {
        if (user == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        return ResponseEntity.ok(wishlistService.getMyWishlistAccommodationIds(user.getId()));
    }
}
