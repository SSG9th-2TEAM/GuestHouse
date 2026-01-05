package com.ssg9th2team.geharbang.domain.admin.service;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import com.ssg9th2team.geharbang.domain.accommodation.entity.ApprovalStatus;
import com.ssg9th2team.geharbang.domain.accommodation.repository.jpa.AccommodationJpaRepository;
import com.ssg9th2team.geharbang.domain.auth.entity.User;
import com.ssg9th2team.geharbang.domain.auth.entity.UserRole;
import com.ssg9th2team.geharbang.domain.auth.repository.UserRepository;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationDetail;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationSummary;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminAccommodationMetrics;
import com.ssg9th2team.geharbang.domain.admin.repository.mybatis.AdminAccommodationMapper;
import com.ssg9th2team.geharbang.domain.admin.dto.AdminPageResponse;
import com.ssg9th2team.geharbang.domain.admin.log.AdminLogConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomJpaRepository;
import com.ssg9th2team.geharbang.domain.room.repository.jpa.RoomStats;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAccommodationService {

    private final AccommodationJpaRepository accommodationRepository;
    private final UserRepository userRepository;
    private final AdminAccommodationMapper accommodationMapper;
    private final RoomJpaRepository roomRepository;
    private final AdminLogService adminLogService;

    public AdminPageResponse<AdminAccommodationSummary> getAccommodations(
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        Sort sorting = "oldest".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        List<Accommodation> filtered = accommodationRepository.findAll(sorting).stream()
                .filter(accommodation -> matchesStatus(accommodation, status))
                .filter(accommodation -> matchesKeyword(accommodation, keyword))
                .toList();

        int totalElements = filtered.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 1;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Accommodation> pageItems = filtered.subList(fromIndex, toIndex);
        Map<Long, AdminAccommodationMetrics> metricsMap = loadMetrics(pageItems);
        List<AdminAccommodationSummary> items = pageItems.stream()
                .map(item -> toSummary(item, metricsMap.get(item.getAccommodationsId())))
                .toList();

        return AdminPageResponse.of(items, page, size, totalElements, totalPages);
    }

    public AdminAccommodationDetail getAccommodationDetail(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        AdminAccommodationMetrics metrics = loadMetrics(List.of(accommodation))
                .get(accommodation.getAccommodationsId());
        return toDetail(accommodation, metrics);
    }

    public AdminAccommodationDetail approveAccommodation(Long adminUserId, Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.updateApprovalStatus(ApprovalStatus.APPROVED, null);
        promoteUserToHost(accommodation.getUserId());
        Accommodation saved = accommodationRepository.save(accommodation);
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.ACTION_APPROVE,
                AdminLogConstants.TARGET_ACCOMMODATION,
                accommodationId,
                null,
                null
        );
        AdminAccommodationMetrics metrics = loadMetrics(List.of(saved))
                .get(saved.getAccommodationsId());
        return toDetail(saved, metrics);
    }

    public AdminAccommodationDetail rejectAccommodation(Long adminUserId, Long accommodationId, String reason) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Accommodation not found"));
        accommodation.reject(reason);
        Accommodation saved = accommodationRepository.save(accommodation);
        adminLogService.writeLog(
                adminUserId,
                AdminLogConstants.ACTION_REJECT,
                AdminLogConstants.TARGET_ACCOMMODATION,
                accommodationId,
                reason,
                null
        );
        AdminAccommodationMetrics metrics = loadMetrics(List.of(saved))
                .get(saved.getAccommodationsId());
        return toDetail(saved, metrics);
    }

    private ApprovalStatus parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "WAIT", "REQUEST" -> ApprovalStatus.PENDING;
            case "APPROVED", "APPROVE" -> ApprovalStatus.APPROVED;
            case "REJECTED", "REJECT", "DENIED" -> ApprovalStatus.REJECTED;
            default -> null;
        };
    }

    private boolean matchesStatus(Accommodation accommodation, String status) {
        ApprovalStatus approvalStatus = parseStatus(status);
        return approvalStatus == null || approvalStatus == accommodation.getApprovalStatus();
    }

    private boolean matchesKeyword(Accommodation accommodation, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalized = keyword.toLowerCase();
        String name = accommodation.getAccommodationsName() != null
                ? accommodation.getAccommodationsName().toLowerCase()
                : "";
        String city = accommodation.getCity() != null ? accommodation.getCity().toLowerCase() : "";
        String district = accommodation.getDistrict() != null ? accommodation.getDistrict().toLowerCase() : "";
        return name.contains(normalized) || city.contains(normalized) || district.contains(normalized);
    }

    private AdminAccommodationSummary toSummary(Accommodation accommodation, AdminAccommodationMetrics metrics) {
        MetricsSnapshot snapshot = MetricsSnapshot.from(metrics);
        return new AdminAccommodationSummary(
                accommodation.getAccommodationsId(),
                accommodation.getUserId(),
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getRejectionReason(),
                accommodation.getCreatedAt(),
                accommodation.getRating(),
                accommodation.getReviewCount(),
                null,
                null,
                null,
                snapshot.reservationCount(),
                snapshot.occupancyRate(),
                snapshot.cancellationRate(),
                snapshot.totalRevenue()
        );
    }

    private AdminAccommodationDetail toDetail(Accommodation accommodation, AdminAccommodationMetrics metrics) {
        MetricsSnapshot snapshot = MetricsSnapshot.from(metrics);
        RoomStats roomStats = roomRepository.findRoomStats(accommodation.getAccommodationsId());
        User host = accommodation.getUserId() != null
                ? userRepository.findById(accommodation.getUserId()).orElse(null)
                : null;

        Integer minPrice = Optional.ofNullable(roomStats)
                .map(RoomStats::getMinPrice)
                .orElse(accommodation.getMinPrice());
        Integer roomCount = Optional.ofNullable(roomStats)
                .map(stats -> stats.getRoomCount() != null ? stats.getRoomCount().intValue() : null)
                .orElse(null);
        Integer maxGuests = Optional.ofNullable(roomStats)
                .map(RoomStats::getMaxGuests)
                .orElse(null);

        return new AdminAccommodationDetail(
                accommodation.getAccommodationsId(),
                accommodation.getUserId(),
                host != null ? host.getName() : null,
                host != null ? host.getPhone() : null,
                accommodation.getAccommodationsName(),
                accommodation.getAccommodationsCategory() != null ? accommodation.getAccommodationsCategory().name() : null,
                accommodation.getCity(),
                accommodation.getDistrict(),
                accommodation.getTownship(),
                accommodation.getAddressDetail(),
                accommodation.getApprovalStatus() != null ? accommodation.getApprovalStatus().name() : null,
                accommodation.getAccommodationStatus(),
                accommodation.getRejectionReason(),
                accommodation.getCreatedAt(),
                minPrice,
                roomCount,
                maxGuests,
                accommodation.getRating(),
                accommodation.getReviewCount(),
                snapshot.reservationCount(),
                snapshot.occupancyRate(),
                snapshot.cancellationRate(),
                snapshot.totalRevenue()
        );
    }

    private Map<Long, AdminAccommodationMetrics> loadMetrics(List<Accommodation> accommodations) {
        List<Long> ids = accommodations.stream()
                .map(Accommodation::getAccommodationsId)
                .filter(id -> id != null)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return accommodationMapper.selectAccommodationMetrics(ids).stream()
                .collect(Collectors.toMap(AdminAccommodationMetrics::accommodationsId, metric -> metric));
    }

    private record MetricsSnapshot(
            int reservationCount,
            Double occupancyRate,
            Double cancellationRate,
            long totalRevenue
    ) {
        static MetricsSnapshot from(AdminAccommodationMetrics metrics) {
            if (metrics == null) {
                return new MetricsSnapshot(0, 0.0, 0.0, 0L);
            }
            int reservationCount = Optional.ofNullable(metrics.reservationCount()).orElse(0);
            int canceledCount = Optional.ofNullable(metrics.canceledCount()).orElse(0);
            int totalCount = Optional.ofNullable(metrics.totalCount()).orElse(0);
            long totalRevenue = Optional.ofNullable(metrics.totalRevenue()).orElse(0L);
            Double occupancyRate = totalCount > 0 ? (reservationCount * 100.0) / totalCount : 0.0;
            Double cancellationRate = totalCount > 0 ? (canceledCount * 100.0) / totalCount : 0.0;
            return new MetricsSnapshot(reservationCount, occupancyRate, cancellationRate, totalRevenue);
        }
    }

    private void promoteUserToHost(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() != UserRole.HOST) {
            user.updateRole(UserRole.HOST);
        }
        user.updateHostApproved(true);
        userRepository.save(user);
    }
}
