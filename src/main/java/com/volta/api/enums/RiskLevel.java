package com.volta.api.enums;

import java.util.Collection;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH;

    public static boolean isHazardous(String riskLevel) {
        return HIGH.name().equals(riskLevel);
    }

    public static String highest(Collection<String> riskLevels) {
        RiskLevel highest = null;

        for (String riskLevel : riskLevels) {
            RiskLevel current = valueOf(riskLevel);

            if (highest == null || current.ordinal() > highest.ordinal()) {
                highest = current;
            }
        }

        return highest != null ? highest.name() : null;
    }
}
