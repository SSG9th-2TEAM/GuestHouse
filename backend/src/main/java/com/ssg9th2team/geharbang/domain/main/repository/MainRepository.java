package com.ssg9th2team.geharbang.domain.main.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MainRepository extends JpaRepository<Accommodation, Long> {
        @Query(value = """
                        SELECT DISTINCT
                            a.accommodations_id AS accommodationsId,
                            a.accommodations_name AS accommodationsName,
                            a.short_description AS shortDescription,
                            a.accommodations_description AS accommodationsDescription,
                            a.city AS city,
                            a.district AS district,
                            a.township AS township,
                            a.latitude AS latitude,
                            a.longitude AS longitude,
                            a.min_price AS minPrice,
                            a.rating AS rating,
                            a.review_count AS reviewCount
                        FROM accommodation a
                        JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                        WHERE at.theme_id IN (:themeIds)
                        AND a.accommodation_status = 1
                        AND a.approval_status = 'APPROVED'
                        """, nativeQuery = true)
        List<AccommodationListProjection> findByThemeIds(@Param("themeIds") List<Long> themeIds);

        // JPA Method optimization replacement
        @Query(value = """
                        SELECT
                            a.accommodations_id AS accommodationsId,
                            a.accommodations_name AS accommodationsName,
                            a.short_description AS shortDescription,
                            a.accommodations_description AS accommodationsDescription,
                            a.city AS city,
                            a.district AS district,
                            a.township AS township,
                            a.latitude AS latitude,
                            a.longitude AS longitude,
                            a.min_price AS minPrice,
                            a.rating AS rating,
                            a.review_count AS reviewCount
                        FROM accommodation a
                        WHERE a.accommodation_status = :accommodationStatus
                          AND a.approval_status = :#{#approvalStatus.name()}
                        """, nativeQuery = true)
        List<AccommodationListProjection> findAllProjectedByAccommodationStatusAndApprovalStatus(
                        @Param("accommodationStatus") Integer accommodationStatus,
                        @Param("approvalStatus") ApprovalStatus approvalStatus);

        @Query(value = """
                        SELECT
                            a.accommodations_id AS accommodationsId,
                            a.accommodations_name AS accommodationsName,
                            a.short_description AS shortDescription,
                            a.accommodations_description AS accommodationsDescription,
                            a.city AS city,
                            a.district AS district,
                            a.township AS township,
                            a.latitude AS latitude,
                            a.longitude AS longitude,
                            a.min_price AS minPrice,
                            a.rating AS rating,
                            a.review_count AS reviewCount
                        FROM accommodation a
                        WHERE a.accommodation_status = 1
                          AND a.approval_status = 'APPROVED'
                          AND LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%')
                        """, nativeQuery = true)
        List<AccommodationListProjection> findApprovedByKeyword(@Param("keyword") String keyword);

        @Query(value = """
                        SELECT DISTINCT
                            a.accommodations_id AS accommodationsId,
                            a.accommodations_name AS accommodationsName,
                            a.short_description AS shortDescription,
                            a.accommodations_description AS accommodationsDescription,
                            a.city AS city,
                            a.district AS district,
                            a.township AS township,
                            a.latitude AS latitude,
                            a.longitude AS longitude,
                            a.min_price AS minPrice,
                            a.rating AS rating,
                            a.review_count AS reviewCount
                        FROM accommodation a
                        JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
                        WHERE at.theme_id IN (:themeIds)
                          AND a.accommodation_status = 1
                          AND a.approval_status = 'APPROVED'
                          AND LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%')
                        """, nativeQuery = true)
        List<AccommodationListProjection> findByThemeIdsAndKeyword(
                        @Param("themeIds") List<Long> themeIds,
                        @Param("keyword") String keyword);

        @Query(value = """
                        SELECT
                            ai.accommodations_id AS accommodationsId,
                            ai.image_url AS imageUrl
                        FROM accommodation_image ai
                        WHERE ai.sort_order = 0
                          AND ai.image_type = 'banner'
                          AND ai.accommodations_id IN (:accommodationIds)
                        """, nativeQuery = true)
        List<AccommodationImageProjection> findRepresentativeImages(
                        @Param("accommodationIds") List<Long> accommodationIds);
}
