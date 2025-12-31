package com.ssg9th2team.geharbang.domain.main.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MainRepository extends JpaRepository<Accommodation, Long> {
    @Query(value = """
            SELECT DISTINCT a.*
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
            AND a.accommodation_status = 1
            AND a.approval_status = 'APPROVED'
            """, nativeQuery = true)
    List<Accommodation> findByThemeIds(@Param("themeIds") List<Long> themeIds);

    List<Accommodation> findByAccommodationStatusAndApprovalStatus(
            Integer accommodationStatus, ApprovalStatus approvalStatus);

    @Query(value = """
            SELECT a.*
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%')
            """, nativeQuery = true)
    List<Accommodation> findApprovedByKeyword(@Param("keyword") String keyword);

    @Query(value = """
            SELECT DISTINCT a.*
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%')
            """, nativeQuery = true)
    List<Accommodation> findByThemeIdsAndKeyword(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword
    );

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
            @Param("accommodationIds") List<Long> accommodationIds
    );

    @Query(value = """
            SELECT
                a.accommodations_id AS accomodationsId,
                a.accommodations_name AS accomodationsName,
                a.short_description AS shortDescription,
                a.city AS city,
                a.district AS district,
                a.township AS township,
                a.latitude AS latitude,
                a.longitude AS longitude,
                a.min_price AS minPrice,
                a.rating AS rating,
                a.review_count AS reviewCount,
                ai.image_url AS imageUrl
            FROM accommodation a
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY a.accommodations_id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicList(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT
                a.accommodations_id AS accomodationsId,
                a.accommodations_name AS accomodationsName,
                a.short_description AS shortDescription,
                a.city AS city,
                a.district AS district,
                a.township AS township,
                a.latitude AS latitude,
                a.longitude AS longitude,
                a.min_price AS minPrice,
                a.rating AS rating,
                a.review_count AS reviewCount,
                ai.image_url AS imageUrl
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY a.accommodations_id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.accommodations_id)
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByTheme(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                a.accommodations_id AS accomodationsId,
                a.accommodations_name AS accomodationsName,
                a.short_description AS shortDescription,
                a.city AS city,
                a.district AS district,
                a.township AS township,
                a.latitude AS latitude,
                a.longitude AS longitude,
                a.min_price AS minPrice,
                a.rating AS rating,
                a.review_count AS reviewCount,
                ai.image_url AS imageUrl
            FROM accommodation a
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY a.accommodations_id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByBounds(
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT
                a.accommodations_id AS accomodationsId,
                a.accommodations_name AS accomodationsName,
                a.short_description AS shortDescription,
                a.city AS city,
                a.district AS district,
                a.township AS township,
                a.latitude AS latitude,
                a.longitude AS longitude,
                a.min_price AS minPrice,
                a.rating AS rating,
                a.review_count AS reviewCount,
                ai.image_url AS imageUrl
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            ORDER BY a.accommodations_id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a.accommodations_id)
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByThemeAndBounds(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            Pageable pageable
    );
}
