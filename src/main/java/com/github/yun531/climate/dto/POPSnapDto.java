package com.github.yun531.climate.dto;

import com.github.yun531.climate.domain.PopDailySeries7;
import com.github.yun531.climate.domain.PopSeries24;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class POPSnapDto {

    private long snapId;
    private long regionId;
    private LocalDateTime effectiveTime;

    private PopSeries24 hourly;         // ---- 시간대별 POP (0~23시) ---- //
    private PopDailySeries7 daily;      // ---- 일자별 (0~6일) 오전/오후 POP ----
}