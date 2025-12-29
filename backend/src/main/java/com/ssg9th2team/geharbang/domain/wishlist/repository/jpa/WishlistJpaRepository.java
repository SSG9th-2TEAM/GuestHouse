package com.ssg9th2team.geharbang.domain.wishlist.repository.jpa;

import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface WishlistJpaRepository extends JpaRepository<Wishlist, Long> {

    // 위시리스트 저장
    // save()

    // 중복 추가 방지 확인용
    boolean existsByUserIdAndAccommodationsId(Long userId, Long accommodationsId);

    // 특정 숙소 위시리스트 삭제 용
    void deleteByUserIdAndAccommodationsId(Long userId, Long accommodationsId);
}
