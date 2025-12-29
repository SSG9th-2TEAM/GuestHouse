package com.ssg9th2team.geharbang.domain.wishlist.repository.jpa;

import com.ssg9th2team.geharbang.domain.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface WishlistJpaRepository extends JpaRepository<Wishlist, Long> {

    // 위시리스트 저장
    // save()

    // 중복 추가 방지 확인용
    boolean existsByUserIdAndAccommodationsId(Long userId, Long accommodationsId);

    // 사용자가 상세 페이지에서 찜 버튼을 다시 눌러서 취소(해제)할 때 사용 (내 것만 삭제)
    void deleteByUserIdAndAccommodationsId(Long userId, Long accommodationsId);
}
