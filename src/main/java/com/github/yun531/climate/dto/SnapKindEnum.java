package com.github.yun531.climate.dto;

import lombok.Getter;

@Getter
public enum SnapKindEnum {
    SNAP_CURRENT(1),
    SNAP_PREVIOUS(10);

    private final int code;

    SnapKindEnum(int code) {
        this.code = code;
    }
}

