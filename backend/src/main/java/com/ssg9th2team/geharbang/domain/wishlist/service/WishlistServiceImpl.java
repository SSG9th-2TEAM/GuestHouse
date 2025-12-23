package com.ssg9th2team.geharbang.domain.wishlist.service;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import com.ssg9th2team.geharbang.domain.wishlist.repository.jpa.WishlistJpaRepository;
import com.ssg9th2team.geharbang.domain.wishlist.repository.mybatis.WishlistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistMapper wishlistMapper;
    private final WishlistJpaRepository wishlistJpaRepository;


    // 위시리스트 추가
    @Override
    public void addWishlist(Long userId, Long accommodationId) {
        // 중복 검증
        boolean exists = wishlistJpaRepository.existsByUserIdAndAccommodationsId(userId, accommodationId);
        if (exists) {
            throw new IllegalArgumentException("Wishlist already exists!");
        }

        Wishlist wishlist = Wishlist.builder()
                .userId(userId)
                .accommodationsId(accommodationId)
                .build();

        wishlistJpaRepository.save(wishlist);
    }

    // 위시리스트 삭제 (특정 숙소 취소)
    @Override
    public void removeWishlist(Long userId, Long accommodationId) {
        wishlistJpaRepository.deleteByUserIdAndAccommodationsId(userId, accommodationId);
    }

    // 내 위시리스트 조회 (마이페이지용)
    @Override
    public List<AccommodationResponseDto> getMyWishlist(Long userId) {
        return wishlistMapper.selectWishlistByUserId(userId);
    }

    // 위시리스트 조회 (메인페이지용)
    @Override
    public List<Long> getMyWishlistAccommodationIds(Long userId) {
        return List.of();
    }
}
   