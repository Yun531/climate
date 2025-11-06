package com.github.yun531.climate.service.rule;

import com.github.yun531.climate.service.ClimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RainOnsetChangeRule implements AlertRule {

    private static final int RAIN_TH = RainThresholdEnum.RAIN.getThreshold();
    private final ClimateService climateService;

    @Override public AlertTypeEnum supports() { return AlertTypeEnum.RAIN_ONSET; }

    @Override
    public List<AlertEvent> evaluate(List<Long> regionIds, Instant since) {
        List<AlertEvent> out = new ArrayList<>();
        for (Long regionId : regionIds) {
            var series = climateService.loadDefaultPopSeries(regionId);
            if (series.current() == null || series.previousShifted() == null) continue;

            int[] cur = series.current();          // 24개 (0~23시)
            int[] prvShift = series.previousShifted(); // 23개 (0~22 비교용)

            for (int h = 0; h <= 22; h++) {
                boolean wasNotRain = prvShift[h] < RAIN_TH;
                boolean nowRain    = cur[h] >= RAIN_TH;
                if (wasNotRain && nowRain) {
                    // todo: occurredAt: 스냅/슬롯→Instant 변환 로직 보간 필요, since 반영 고민중
                    Instant occurredAt = Instant.now();

                    out.add(new AlertEvent(
                            AlertTypeEnum.RAIN_ONSET,
                            regionId,
                            occurredAt,
                            Map.of(
                                    "_srcRule", "RainOnsetChangeRule",
                                    "hour", h,
                                    "pop", cur[h])
                    ));
                }
            }
        }
//        // since 필터 없어도 되지 않나?
//        if (since != null) {
//            out = out.stream().filter(e -> e.occurredAt().isAfter(since)).toList();
//        }
        return out;
    }
}
