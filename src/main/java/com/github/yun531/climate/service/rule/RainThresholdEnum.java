package com.github.yun531.climate.service.rule;

public enum RainThresholdEnum {
    RAIN(60);

    private final int threshold;

    RainThresholdEnum(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }
}

