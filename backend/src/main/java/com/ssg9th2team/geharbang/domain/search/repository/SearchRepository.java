package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.main.repository.ListDtoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchRepository extends JpaRepository<Accommodation, Long> {
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
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl
            FROM accommodation a
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
            ORDER BY a.accommodations_id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM accommodation a
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicList(
            @Param("keyword") String keyword,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
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
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
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
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByTheme(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
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
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl
            FROM accommodation a
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            WHERE a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
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
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByBounds(
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
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
                COALESCE(rmax.maxGuests, 0) AS maxGuests,
                ai.image_url AS imageUrl
            FROM accommodation a
            JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id
            LEFT JOIN accommodation_image ai
              ON ai.accommodations_id = a.accommodations_id
             AND ai.sort_order = 0
             AND ai.image_type = 'banner'
            LEFT JOIN (
                SELECT accommodations_id, MAX(max_guests) AS maxGuests
                FROM room
                WHERE room_status = 1
                GROUP BY accommodations_id
            ) rmax ON rmax.accommodations_id = a.accommodations_id
            WHERE at.theme_id IN (:themeIds)
              AND a.accommodation_status = 1
              AND a.approval_status = 'APPROVED'
              AND a.latitude IS NOT NULL
              AND a.longitude IS NOT NULL
              AND a.latitude BETWEEN :minLat AND :maxLat
              AND a.longitude BETWEEN :minLng AND :maxLng
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(CONCAT_WS(' ', a.accommodations_name, a.city, a.district, a.township)) LIKE CONCAT('%', LOWER(:keyword), '%'))
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
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
              AND (:guestCount IS NULL OR :guestCount = 0
                   OR EXISTS (
                      SELECT 1
                      FROM room rcap
                      WHERE rcap.accommodations_id = a.accommodations_id
                        AND rcap.room_status = 1
                        AND COALESCE(rcap.max_guests, 0) >= :guestCount
                   ))
              AND (:checkin IS NULL OR :checkout IS NULL
                   OR EXISTS (
                      SELECT 1
                      FROM room r
                      WHERE r.accommodations_id = a.accommodations_id
                        AND r.room_status = 1
                        AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                        AND NOT EXISTS (
                            SELECT 1
                            FROM reservation res
                            WHERE res.room_id = r.room_id
                              AND res.is_deleted = 0
                              AND res.reservation_status IN (2, 3)
                              AND res.checkin < :checkout
                              AND res.checkout > :checkin
                        )
                   ))
            """,
            nativeQuery = true)
    Page<ListDtoProjection> searchPublicListByThemeAndBounds(
            @Param("themeIds") List<Long> themeIds,
            @Param("keyword") String keyword,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("checkin") LocalDateTime checkin,
            @Param("checkout") LocalDateTime checkout,
            @Param("guestCount") Integer guestCount,
            Pageable pageable
    );
}
