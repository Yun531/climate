package com.github.yun531.climate.service.rule;

import lombok.Getter;

public enum RainThresholdEnum {
    RAIN(60);

    @Getter
    private final int threshold;

    RainThresholdEnum(int threshold) {
        this.threshold = threshold;
    }
}

