package com.ssg9th2team.geharbang.domain.wishlist.repository.jpa;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface WishlistJpaRepository extends JpaRepository<Accommodation, Integer> {
}
