package com.github.yun531.climate.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WarningLevel {
    WATCH("예비특보"),
    ADVISORY("주의보"),
    WARNING("경보");

    private final String label;

    WarningLevel(String label) {
        this.label = label;
    }

    @JsonValue   // JSON 응답 시 '주의보', '경보' 형태로 직렬화
    public String getLabel() {
        return label;
    }

}
