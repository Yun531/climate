package com.github.yun531.climate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PopSeries24 {
    private final List<Integer> values; // size 24

    public int get(int hour) { return values.get(hour); }

    public int size() { return values.size(); }

    public int max() {
        int max = 0;
        for (Integer v : values) {
            if (v != null && v > max) {
                max = v;
            }
        }
        return max;
    }
}
