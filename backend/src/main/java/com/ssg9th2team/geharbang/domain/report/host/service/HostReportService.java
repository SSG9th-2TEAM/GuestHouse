package com.ssg9th2team.geharbang.domain.report.host.service;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportResponse;
import com.ssg9th2team.geharbang.domain.report.host.repository.mybatis.HostReportMapper;
import com.ssg9th2team.geharbang.domain.report.host.ai.AiSummaryClient;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRatingRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportRecentRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTagRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportTrendRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastBaseline;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastDaily;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostForecastSummary;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostDemandDailyRow;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostThemeReportRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HostReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_RECENT_DAYS = 30;

    private final HostReportMapper hostReportMapper;
    private final AiSummaryClient aiSummaryClient;

    public HostReportService(
            HostReportMapper hostReportMapper,
            @Qualifier("aiSummaryClientFacade") AiSummaryClient aiSummaryClient
    ) {
        this.hostReportMapper = hostReportMapper;
        this.aiSummaryClient = aiSummaryClient;
    }

    public HostReviewReportSummaryResponse getReviewSummary(Long hostId, Long accommodationId, LocalDate from, LocalDate to) {
        validateOwnership(hostId, accommodationId);

        LocalDate today = LocalDate.now(KST);
        LocalDate safeTo = to != null ? to : today;
        LocalDate safeFrom = from != null ? from : safeTo.minusDays(DEFAULT_RECENT_DAYS);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        HostReviewReportSummaryResponse summary = hostReportMapper.selectReviewSummary(hostId, accommodationId, start, end);
        if (summary == null) {
            summary = new HostReviewReportSummaryResponse();
            summary.setAvgRating(0.0);
            summary.setReviewCount(0);
        }

        List<HostReviewReportRatingRow> ratingRows = hostReportMapper.selectReviewRatingDistribution(hostId, accommodationId, start, end);
        Map<Integer, Integer> ratingDistribution = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            ratingDistribution.put(rating, 0);
        }
        if (ratingRows != null) {
            for (HostReviewReportRatingRow row : ratingRows) {
                if (row.getRating() == null) continue;
                ratingDistribution.put(row.getRating(), row.getCount() != null ? row.getCount() : 0);
            }
        }

        List<HostReviewReportTagRow> tags = hostReportMapper.selectTopReviewTags(hostId, accommodationId, start, end);
        List<HostReviewReportRecentRow> recentReviews = hostReportMapper.selectRecentReviews(hostId, accommodationId, start, end, 5);

        summary.setFrom(safeFrom);
        summary.setTo(safeTo);
        summary.setAccommodationId(accommodationId);
        summary.setRatingDistribution(ratingDistribution);
        summary.setTopTags(tags == null ? List.of() : tags);
        summary.setRecentReviews(recentReviews == null ? List.of() : recentReviews);

        return summary;
    }

    public List<HostReviewReportTrendRow> getReviewTrend(Long hostId, Long accommodationId, int months) {
        validateOwnership(hostId, accommodationId);

        int safeMonths = months > 0 ? months : 6;
        LocalDate today = LocalDate.now(KST);
        LocalDate startMonth = today.minusMonths(safeMonths - 1L).withDayOfMonth(1);
        LocalDate endMonth = today.plusMonths(1).withDayOfMonth(1);
        LocalDateTime start = startMonth.atStartOfDay();
        LocalDateTime end = endMonth.atStartOfDay();

        List<HostReviewReportTrendRow> rows = hostReportMapper.selectReviewTrend(hostId, accommodationId, start, end);
        Map<String, HostReviewReportTrendRow> rowMap = new LinkedHashMap<>();
        if (rows != null) {
            for (HostReviewReportTrendRow row : rows) {
                if (row.getPeriod() != null) {
                    rowMap.put(row.getPeriod(), row);
                }
            }
        }

        List<HostReviewReportTrendRow> result = new ArrayList<>();
        LocalDate cursor = startMonth;
        while (cursor.isBefore(endMonth)) {
            String key = String.format(Locale.KOREA, "%d-%02d", cursor.getYear(), cursor.getMonthValue());
            HostReviewReportTrendRow row = rowMap.getOrDefault(key, null);
            if (row == null) {
                row = new HostReviewReportTrendRow();
                row.setPeriod(key);
                row.setReviewCount(0);
                row.setAvgRating(0.0);
            }
            result.add(row);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    public HostThemeReportResponse getThemePopularity(Long hostId, Long accommodationId, LocalDate from, LocalDate to, String metric) {
        validateOwnership(hostId, accommodationId);

        LocalDate today = LocalDate.now(KST);
        LocalDate safeTo = to != null ? to : today;
        LocalDate safeFrom = from != null ? from : safeTo.minusDays(DEFAULT_RECENT_DAYS);
        LocalDateTime start = safeFrom.atStartOfDay();
        LocalDateTime end = safeTo.plusDays(1).atStartOfDay();

        String safeMetric = "revenue".equalsIgnoreCase(metric) ? "revenue" : "reservations";
        List<HostThemeReportRow> rows = hostReportMapper.selectThemePopularity(hostId, accommodationId, start, end, safeMetric);

        HostThemeReportResponse response = new HostThemeReportResponse();
        response.setFrom(safeFrom);
        response.setTo(safeTo);
        response.setAccommodationId(accommodationId);
        response.setMetric(safeMetric);
        response.setRows(rows == null ? List.of() : rows);
        return response;
    }

    public HostForecastResponse getDemandForecast(Long hostId, Long accommodationId, String target, int horizonDays, int historyDays) {
        validateOwnership(hostId, accommodationId);

        int safeHistoryDays = historyDays > 0 ? historyDays : 180;
        int safeHorizonDays = horizonDays > 0 ? horizonDays : 30;
        String safeTarget = "revenue".equalsIgnoreCase(target) ? "revenue" : "reservations";

        LocalDate today = LocalDate.now(KST);
        LocalDate historyStart = today.minusDays(safeHistoryDays);
        LocalDate historyEnd = today.plusDays(1);

        List<HostDemandDailyRow> rows = hostReportMapper.selectDemandDaily(
                hostId,
                accommodationId,
                historyStart.atStartOfDay(),
                historyEnd.atStartOfDay()
        );

        Map<LocalDate, Double> dailyValues = new LinkedHashMap<>();
        for (int i = 0; i <= safeHistoryDays; i++) {
            LocalDate date = historyStart.plusDays(i);
            dailyValues.put(date, 0.0);
        }
        if (rows != null) {
            for (HostDemandDailyRow row : rows) {
                LocalDate date = row.getStatDate();
                if (date == null || !dailyValues.containsKey(date)) continue;
                double value = "revenue".equals(safeTarget)
                        ? row.getRevenue() != null ? row.getRevenue() : 0
                        : row.getReservationCount() != null ? row.getReservationCount() : 0;
                dailyValues.put(date, value);
            }
        }

        double recentAvg7 = averageForLastDays(dailyValues, 7);
        double recentAvg28 = averageForLastDays(dailyValues, 28);
        double baseAverage = (recentAvg7 * 0.6) + (recentAvg28 * 0.4);

        Map<DayOfWeek, Double> weekdayAverages = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            weekdayAverages.put(day, 0.0);
        }
        Map<DayOfWeek, List<Double>> grouped = dailyValues.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getDayOfWeek(), LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Map.Entry<DayOfWeek, List<Double>> entry : grouped.entrySet()) {
            weekdayAverages.put(entry.getKey(), average(entry.getValue()));
        }

        double overallAvg = average(new ArrayList<>(dailyValues.values()));
        Map<String, Double> weekdayFactors = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            double factor = overallAvg > 0 ? weekdayAverages.getOrDefault(day, 0.0) / overallAvg : 1.0;
            weekdayFactors.put(day.name(), roundOneDecimal(factor));
        }

        List<HostForecastDaily> forecastDaily = new ArrayList<>();
        double predictedTotal = 0.0;
        for (int i = 1; i <= safeHorizonDays; i++) {
            LocalDate date = today.plusDays(i);
            DayOfWeek day = date.getDayOfWeek();
            double factor = weekdayFactors.getOrDefault(day.name(), 1.0);
            double predicted = baseAverage * factor;
            if ("reservations".equals(safeTarget)) {
                predicted = Math.max(0, Math.round(predicted));
            } else {
                predicted = Math.max(0, Math.round(predicted));
            }
            predictedTotal += predicted;

            HostForecastDaily daily = new HostForecastDaily();
            daily.setDate(date);
            daily.setPredictedValue(predicted);
            forecastDaily.add(daily);
        }

        HostForecastBaseline baseline = new HostForecastBaseline();
        baseline.setRecentAvg7(roundOneDecimal(recentAvg7));
        baseline.setRecentAvg28(roundOneDecimal(recentAvg28));
        baseline.setWeekdayFactors(weekdayFactors);

        HostForecastSummary summary = new HostForecastSummary();
        summary.setPredictedTotal(roundOneDecimal(predictedTotal));
        summary.setPredictedAvgPerDay(roundOneDecimal(predictedTotal / safeHorizonDays));

        HostForecastResponse response = new HostForecastResponse();
        response.setTarget(safeTarget);
        response.setHorizonDays(safeHorizonDays);
        response.setHistoryDays(safeHistoryDays);
        response.setFrom(historyStart);
        response.setTo(today);
        response.setAccommodationId(accommodationId);
        response.setBaseline(baseline);
        response.setForecastDaily(forecastDaily);
        response.setForecastSummary(summary);
        return response;
    }

    public HostReviewAiSummaryResponse getReviewAiSummary(Long hostId, Long accommodationId, LocalDate from, LocalDate to) {
        HostReviewReportSummaryResponse summary = getReviewSummary(hostId, accommodationId, from, to);
        Integer reviewCount = summary.getReviewCount() != null ? summary.getReviewCount() : 0;
        if (reviewCount == 0) {
            HostReviewAiSummaryResponse empty = new HostReviewAiSummaryResponse();
            empty.setAccommodationId(accommodationId);
            empty.setFrom(summary.getFrom());
            empty.setTo(summary.getTo());
            empty.setGeneratedAt(java.time.ZonedDateTime.now(KST).toString());
            empty.setOverview(List.of());
            empty.setPositives(List.of());
            empty.setNegatives(List.of());
            empty.setActions(List.of());
            empty.setRisks(List.of());
            return empty;
        }
        HostReviewAiSummaryRequest request = new HostReviewAiSummaryRequest();
        request.setAccommodationId(accommodationId);
        request.setFrom(summary.getFrom());
        request.setTo(summary.getTo());
        return aiSummaryClient.generate(summary, request);
    }

    private void validateOwnership(Long hostId, Long accommodationId) {
        if (accommodationId == null) return;
        Long id = hostReportMapper.selectHostAccommodationId(hostId, accommodationId);
        if (id == null) {
            throw new AccessDeniedException("Not allowed to access this accommodation");
        }
    }

    private double averageForLastDays(Map<LocalDate, Double> values, int days) {
        if (values.isEmpty()) return 0.0;
        List<Double> list = new ArrayList<>(values.values());
        int start = Math.max(0, list.size() - days);
        return average(list.subList(start, list.size()));
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
