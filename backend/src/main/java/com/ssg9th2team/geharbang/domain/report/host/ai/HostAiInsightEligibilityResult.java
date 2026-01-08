package com.ssg9th2team.geharbang.domain.report.host.ai;

import lombok.Getter;

@Getter
public class HostAiInsightEligibilityResult {
    private final String status;
    private final boolean canGenerate;
    private final String disabledReason;
    private final String warningMessage;
    private final Integer current;
    private final Integer minRequired;
    private final Integer recommended;
    private final String unitLabel;

    private HostAiInsightEligibilityResult(
            String status,
            boolean canGenerate,
            String disabledReason,
            String warningMessage,
            Integer current,
            Integer minRequired,
            Integer recommended,
            String unitLabel
    ) {
        this.status = status;
        this.canGenerate = canGenerate;
        this.disabledReason = disabledReason;
        this.warningMessage = warningMessage;
        this.current = current;
        this.minRequired = minRequired;
        this.recommended = recommended;
        this.unitLabel = unitLabel;
    }

    public static HostAiInsightEligibilityResult blocked(
            String disabledReason,
            int current,
            int minRequired,
            int recommended,
            String unitLabel
    ) {
        return new HostAiInsightEligibilityResult(
                "BLOCKED",
                false,
                disabledReason,
                "",
                current,
                minRequired,
                recommended,
                unitLabel
        );
    }

    public static HostAiInsightEligibilityResult warn(
            String warningMessage,
            int current,
            int minRequired,
            int recommended,
            String unitLabel
    ) {
        return new HostAiInsightEligibilityResult(
                "WARN",
                true,
                "",
                warningMessage,
                current,
                minRequired,
                recommended,
                unitLabel
        );
    }

    public static HostAiInsightEligibilityResult ok(
            int current,
            int minRequired,
            int recommended,
            String unitLabel
    ) {
        return new HostAiInsightEligibilityResult(
                "OK",
                true,
                "",
                "",
                current,
                minRequired,
                recommended,
                unitLabel
        );
    }
}
