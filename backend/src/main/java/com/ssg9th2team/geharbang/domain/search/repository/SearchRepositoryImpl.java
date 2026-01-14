package com.ssg9th2team.geharbang.domain.search.repository;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public Page<ListDto> searchDynamic(
            List<Long> themeIds,
            String keyword,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            LocalDateTime checkin,
            LocalDateTime checkout,
            Integer guestCount,
            Integer minPrice,
            Integer maxPrice,
            boolean includeUnavailable,
            Pageable pageable) {
        boolean hasStayDates = checkin != null && checkout != null;
        boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;
        boolean hasThemes = themeIds != null && !themeIds.isEmpty();
        boolean hasGuestCount = guestCount != null && guestCount > 0;

        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();

        // 1. Common CTEs
        String ctes = buildCtes(hasStayDates, hasGuestCount);

        sql.append(ctes);
        countSql.append(ctes);

        // 2. Select Clauses
        String selectClause = """
                SELECT DISTINCT
                    a.accommodations_id,
                    a.accommodations_name,
                    CASE
                        WHEN a.short_description IS NOT NULL AND TRIM(a.short_description) <> ''
                        THEN a.short_description
                        ELSE a.accommodations_description
                    END AS short_description,
                    a.city,
                    a.district,
                    a.township,
                    a.latitude,
                    a.longitude,
                    """ + getPriceColumn(hasStayDates, hasGuestCount)
                + """
                             AS effective_price,
                            a.rating,
                            a.review_count,
                            COALESCE(rmax.maxGuests, 0) AS maxGuests,
                            ai.image_url,
                            (COALESCE(a.review_count, 0) * COALESCE(a.rating, 0.0) + 40.0) / (COALESCE(a.review_count, 0) + 10.0) AS bayesianScore
                        """;

        String countSelectClause = "SELECT COUNT(DISTINCT a.accommodations_id)";

        // 3. From & Joins
        StringBuilder fromClause = new StringBuilder();
        fromClause.append(" FROM accommodation a\n");

        if (hasThemes) {
            fromClause.append(" JOIN accommodation_theme at ON at.accommodations_id = a.accommodations_id\n");
        }

        // Join for Max Guests / Stats
        fromClause.append("""
                    LEFT JOIN (
                        SELECT accommodations_id, MAX(max_guests) AS maxGuests, count(*) as activeRoomCount
                        FROM room
                        WHERE room_status = 1
                        GROUP BY accommodations_id
                    ) rmax ON rmax.accommodations_id = a.accommodations_id
                """);

        // Join for Image (only needed for main query, not count usually, but keeping
        // simple)
        // Actually count query doesn't need image join.

        // Join for Min Prices (if dates)
        if (hasStayDates) {
            fromClause.append(" LEFT JOIN min_prices mp ON mp.accommodations_id = a.accommodations_id\n");
        } else if (hasGuestCount) {
            // If no dates but guest count, we might need filtered min price?
            // The original query used `room_stats` with minPriceForGuest.
            // For simplicity, we'll stick to a simpler model or replicate `min_prices`
            // logic if strictly needed.
            // Original: `COALESCE(rs.minPriceForGuest, a.min_price)`
            fromClause.append("""
                       LEFT JOIN (
                           SELECT accommodations_id, MIN(price) as minPriceForGuest
                           FROM room
                           WHERE room_status = 1 AND max_guests >= :guestCount
                           GROUP BY accommodations_id
                       ) rguest ON rguest.accommodations_id = a.accommodations_id
                    """);
        }

        // 4. Where Clause
        StringBuilder whereClause = new StringBuilder();
        whereClause.append(" WHERE a.accommodation_status = 1 AND a.approval_status = 'APPROVED'\n");

        if (hasThemes) {
            whereClause.append(" AND at.theme_id IN (:themeIds)\n");
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            whereClause.append(
                    " AND (LOWER(a.accommodations_name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(CONCAT_WS(' ', a.city, a.district, a.township)) LIKE LOWER(CONCAT('%', :keyword, '%')))\n");
        }

        if (hasBounds) {
            whereClause.append(" AND a.latitude BETWEEN :south AND :north AND a.longitude BETWEEN :west AND :east\n");
        }

        // Availability / Guest Capacity Logic
        if (hasStayDates) {
            // Must exist in available_rooms OR (includeUnavailable AND (no rooms OR no
            // valid guests))
            // This logic creates a hard filter for availability unless includeUnavailable
            // is true
            if (includeUnavailable) {
                // Check if it HAS rooms but none available
                // Complex logic. Replicating loosely:
                // Show if available OR (it has no active rooms or maxGuests=0)
                whereClause
                        .append("""
                                   AND (
                                       EXISTS (SELECT 1 FROM available_rooms ar WHERE ar.accommodations_id = a.accommodations_id)
                                       OR (COALESCE(rmax.activeRoomCount, 0) = 0 OR COALESCE(rmax.maxGuests, 0) = 0)
                                   )
                                """);
            } else {
                whereClause.append(
                        " AND EXISTS (SELECT 1 FROM available_rooms ar WHERE ar.accommodations_id = a.accommodations_id)\n");
            }
        } else if (hasGuestCount) {
            // Just check static capacity
            whereClause.append(" AND (COALESCE(rmax.maxGuests, 0) >= :guestCount)\n");
        }

        // Price Filter (on effective_price)
        String col = getPriceColumn(hasStayDates, hasGuestCount);
        if (minPrice != null) {
            whereClause.append(" AND (").append(col).append(" >= :minPrice)\n");
        }
        if (maxPrice != null) {
            whereClause.append(" AND (").append(col).append(" <= :maxPrice)\n");
        }

        // Assemble Main Query
        sql.append(selectClause);
        sql.append(fromClause);
        // Main query needs Image Join
        sql.append("""
                    LEFT JOIN accommodation_image ai
                      ON ai.accommodations_id = a.accommodations_id
                     AND ai.sort_order = 0
                     AND ai.image_type = 'banner'
                """);
        sql.append(whereClause);

        // Assemble Count Query
        countSql.append(countSelectClause);
        countSql.append(fromClause);
        countSql.append(whereClause);

        // Sorting
        // Map Pageable sort to SQL
        if (pageable.getSort().isSorted()) {
            sql.append(" ORDER BY ");
            String orders = pageable.getSort().stream().map(order -> {
                String prop = order.getProperty();
                String dir = order.getDirection().name();
                if (prop.equals("minPrice"))
                    return "effective_price " + dir;
                if (prop.equals("rating"))
                    return "a.rating " + dir;
                if (prop.equals("reviewCount"))
                    return "a.review_count " + dir;
                if (prop.equals("recommend"))
                    return "bayesianScore DESC"; // Custom logic
                if (prop.equals("id"))
                    return "a.accommodations_id " + dir;
                return "a.accommodations_id DESC"; // Default
            }).collect(Collectors.joining(", "));
            sql.append(orders);
        } else {
            sql.append(" ORDER BY bayesianScore DESC");
        }

        // Pagination
        sql.append(" LIMIT :limit OFFSET :offset");

        // Create Queries
        Query q = em.createNativeQuery(sql.toString());
        Query c = em.createNativeQuery(countSql.toString());

        // Bind Parameters
        bindParams(q, themeIds, keyword, minLat, maxLat, minLng, maxLng, checkin, checkout, guestCount, minPrice,
                maxPrice);
        bindParams(c, themeIds, keyword, minLat, maxLat, minLng, maxLng, checkin, checkout, guestCount, minPrice,
                maxPrice);

        q.setParameter("limit", pageable.getPageSize());
        q.setParameter("offset", pageable.getOffset());

        // Execute
        @SuppressWarnings("unchecked")
        List<Object[]> results = q.getResultList();
        long total = ((Number) c.getSingleResult()).longValue();

        List<ListDto> content = results.stream().map(this::mapToDto).toList();

        return new PageImpl<>(content, pageable, total);
    }

    private String getPriceColumn(boolean hasStayDates, boolean hasGuestCount) {
        if (hasStayDates) {
            return "COALESCE(mp.min_price, a.min_price)";
        } else if (hasGuestCount) {
            return "COALESCE(rguest.minPriceForGuest, a.min_price)";
        } else {
            return "a.min_price";
        }
    }

    private void bindParams(Query q, List<Long> themeIds, String keyword, Double minLat, Double maxLat,
            Double minLng, Double maxLng, LocalDateTime checkin, LocalDateTime checkout,
            Integer guestCount, Integer minPrice, Integer maxPrice) {
        if (themeIds != null && !themeIds.isEmpty())
            q.setParameter("themeIds", themeIds);
        if (keyword != null && !keyword.trim().isEmpty())
            q.setParameter("keyword", keyword);
        if (minLat != null && maxLat != null) {
            q.setParameter("south", Math.min(minLat, maxLat));
            q.setParameter("north", Math.max(minLat, maxLat));
            q.setParameter("west", Math.min(minLng, maxLng));
            q.setParameter("east", Math.max(minLng, maxLng));
        }
        if (checkin != null && checkout != null) {
            q.setParameter("checkin", checkin);
            q.setParameter("checkout", checkout);
        }
        if (guestCount != null && guestCount > 0)
            q.setParameter("guestCount", guestCount);
        if (minPrice != null)
            q.setParameter("minPrice", minPrice);
        if (maxPrice != null)
            q.setParameter("maxPrice", maxPrice);
    }

    private String buildCtes(boolean hasStayDates, boolean hasGuestCount) {
        if (!hasStayDates)
            return ""; // No CTEs needed if not checking availability (simplification)

        // This is strictly for the "Available Rooms" logic
        return """
                    WITH RECURSIVE stay_dates (stay_date) AS (
                        SELECT CAST(:checkin AS DATE) AS stay_date
                        UNION ALL
                        SELECT CAST(stay_date AS DATE) + INTERVAL '1' DAY
                        FROM stay_dates
                        WHERE CAST(stay_date AS DATE) < CAST(:checkout AS DATE) - INTERVAL '1' DAY
                    ),
                    room_candidates (room_id, accommodations_id, price, max_guests) AS (
                        SELECT r.room_id,
                               r.accommodations_id,
                               r.price,
                               COALESCE(r.max_guests, 0) AS max_guests
                        FROM room r
                        WHERE r.room_status = 1
                          AND (:guestCount IS NULL OR :guestCount = 0 OR COALESCE(r.max_guests, 0) >= :guestCount)
                    ),
                    available_rooms (room_id, accommodations_id, price, max_guests) AS (
                        SELECT rc.room_id,
                               rc.accommodations_id,
                               rc.price,
                               rc.max_guests
                        FROM room_candidates rc
                        WHERE (
                                (:guestCount IS NULL OR :guestCount = 0)
                                AND NOT EXISTS (
                                    SELECT 1
                                    FROM reservation res
                                    WHERE res.room_id = rc.room_id
                                      AND res.is_deleted = 0
                                      AND res.reservation_status IN (2, 3)
                                      AND res.checkin < :checkout
                                      AND res.checkout > :checkin
                                ))
                           OR (
                                (:guestCount IS NOT NULL AND :guestCount > 0)
                                AND NOT EXISTS (
                                    SELECT 1
                                    FROM stay_dates d
                                    LEFT JOIN reservation res
                                      ON res.room_id = rc.room_id
                                     AND res.is_deleted = 0
                                     AND res.reservation_status IN (2, 3)
                                     AND d.stay_date >= CAST(res.checkin AS DATE)
                                     AND d.stay_date < CAST(res.checkout AS DATE)
                                    GROUP BY d.stay_date
                                    HAVING COALESCE(SUM(res.guest_count), 0) + :guestCount > rc.max_guests
                                ))
                    ),
                    min_prices (accommodations_id, min_price) AS (
                        SELECT accommodations_id,
                               MIN(price) AS min_price
                        FROM available_rooms
                        GROUP BY accommodations_id
                    )
                """;
    }

    private ListDto mapToDto(Object[] row) {
        // Order: id, name, desc, city, district, township, lat, lng, price, rating,
        // review_count, max_guests, image_url, score
        return ListDto.builder()
                .accommodationsId(((Number) row[0]).longValue())
                .accommodationsName((String) row[1])
                .shortDescription((String) row[2])
                .city((String) row[3])
                .district((String) row[4])
                .township((String) row[5])
                .latitude(row[6] != null ? ((Number) row[6]).doubleValue() : null)
                .longitude(row[7] != null ? ((Number) row[7]).doubleValue() : null)
                .minPrice(row[8] != null ? ((Number) row[8]).longValue() : null)
                .rating(row[9] != null ? ((Number) row[9]).doubleValue() : null)
                .reviewCount(row[10] != null ? ((Number) row[10]).intValue() : 0)
                .maxGuests(row[11] != null ? ((Number) row[11]).intValue() : 0)
                .imageUrl((String) row[12])
                .build();
    }
}
