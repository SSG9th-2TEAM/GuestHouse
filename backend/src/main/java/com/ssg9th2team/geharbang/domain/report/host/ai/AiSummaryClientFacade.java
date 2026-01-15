package com.ssg9th2team.geharbang.domain.report.host.ai;

import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryRequest;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewAiSummaryResponse;
import com.ssg9th2team.geharbang.domain.report.host.dto.HostReviewReportSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class AiSummaryClientFacade implements AiSummaryClient {

    private final MockAiSummaryClient mockAiSummaryClient;
    private final OpenAiSummaryClient openAiSummaryClient;

    @Value("${ai.summary.provider:mock}")
    private String provider;

    @Override
    public HostReviewAiSummaryResponse generate(HostReviewReportSummaryResponse summary, HostReviewAiSummaryRequest request) {
        if ("openai".equalsIgnoreCase(provider)) {
            if (!openAiSummaryClient.isConfigured()) {
                log.warn("OpenAI provider selected but not configured. Falling back to mock.");
                return mockAiSummaryClient.generate(summary, request);
            }
            HostReviewAiSummaryResponse response = openAiSummaryClient.generate(summary, request);
            boolean hasContent = response.getOverview() != null && !response.getOverview().isEmpty();
            if (!hasContent) {
                throw new HostReportAiException("OpenAI response empty");
            }
            return response;
        }
        return mockAiSummaryClient.generate(summary, request);
    }
}