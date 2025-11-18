package com.github.yun531.climate.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PopDailySeries7 {
    private final List<DailyPop> days; // size 7

    @Getter
    @AllArgsConstructor
    public static class DailyPop {
        private final int am;
        private final int pm;
    }

    public DailyPop get(int dayOffset) {
        return days.get(dayOffset);
    }
}
