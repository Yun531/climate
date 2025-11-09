package com.github.yun531.climate.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WarningKind {
    HEAT("폭염"),
    COLDWAVE("한파"),
    HEAVY_SNOW("대설"),
    RAIN("호우"),
    DRY("건조"),
    WIND("강풍"),
    HIGH_WAVE("풍랑"),
    TYPHOON("태풍"),
    TSUNAMI("해일"),
    EARTHQUAKE_TSUNAMI("지진해일");

    private final String label;
    WarningKind(String label){ this.label = label; }

    @JsonValue   // JSON 응답에 한글 라벨을 내보내고 싶을 때 (직렬화)
    public String getLabel(){ return label; }

    @JsonCreator  // JSON 입력에서 한글 라벨을 Enum으로 변환할 때 (역직렬화)
    public static WarningKind fromLabel(String label){
        for (var k : values()) if (k.label.equals(label)) return k;
        throw new IllegalArgumentException("Unknown kind: " + label);
    }
}
