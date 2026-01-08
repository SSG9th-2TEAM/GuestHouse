package com.ssg9th2team.geharbang.domain.report.host.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostAiInsightMeta {
    private String status;
    private boolean canGenerate;
    private String disabledReason;
    private String warningMessage;
    private Integer current;
    private Integer minRequired;
    private Integer recommended;
    private String unitLabel;
}
