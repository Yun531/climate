package com.github.yun531.climate.service.rule;

import com.github.yun531.climate.dto.PopSeries;
import com.github.yun531.climate.dto.PopSeries24;
import com.github.yun531.climate.service.ClimateService;
import com.github.yun531.climate.util.CacheEntry;
import com.github.yun531.climate.util.RegionCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RainOnsetChangeRule implements AlertRule {
    private static final int RAIN_TH = RainThresholdEnum.RAIN.getThreshold();
    /** since 기준으로 해당 분(시간) 보다 오래된 계산 결과면 재계산 */
    private static final int RECOMPUTE_THRESHOLD_MINUTES = 165;

    private final ClimateService climateService;

    /** 지역별 캐시: 계산결과 + 계산시각 */
    private final RegionCache<List<AlertEvent>> cache = new RegionCache<>();

    @Override
    public AlertTypeEnum supports() {
        return AlertTypeEnum.RAIN_ONSET;
    }

    @Override
    public List<AlertEvent> evaluate(List<Integer> regionIds, LocalDateTime since) {
        if (regionIds == null || regionIds.isEmpty()) {
            return List.of();
        }

        List<AlertEvent> out = new ArrayList<>();

        for (int regionId : regionIds) {
            CacheEntry<List<AlertEvent>> entry =
                    cache.getOrComputeSinceBased(
                            regionId,
                            since,
                            RECOMPUTE_THRESHOLD_MINUTES,
                            () -> computeForRegion(regionId)    // 람다(함수 객체)
                    );

            if (entry == null || entry.value() == null || entry.value().isEmpty()) {
                continue;
            }
            out.addAll(entry.value());
        }
        return out;
    }

    // 한 지역에 대한 비 시작 시점 계산
    private CacheEntry<List<AlertEvent>> computeForRegion(int regionId) {
        PopSeries series = climateService.loadDefaultPopSeries(regionId);

        if (!isValidSeries(series)) {
            return createEmptyCacheEntry();
        }

        LocalDateTime computedAt = series.curReportTime();
        List<AlertEvent> events = detectRainOnsetEvents(regionId, series, computedAt);

        return new CacheEntry<>(List.copyOf(events), computedAt);
    }

    private boolean isValidSeries(PopSeries series) {
        return series != null
                && series.current() != null
                && series.previous() != null;
    }

    private CacheEntry<List<AlertEvent>> createEmptyCacheEntry() {
        return new CacheEntry<>(List.of(), null);
    }

    // 시계열 비교 및 이벤트 생성
    private List<AlertEvent> detectRainOnsetEvents(int regionId,
                                                   PopSeries series,
                                                   LocalDateTime computedAt) {

        PopSeries24 cur = series.current();
        PopSeries24 prv = series.previous();
        int gap = series.reportTimeGap();  // 이전 스냅 → 현재 스냅까지 시간 차 (시간 단위)

        int maxH = computeMaxComparableHour(cur, prv, gap);
        if (maxH < 0) {
            return List.of();
        }

        List<AlertEvent> events = new ArrayList<>();
        for (int h = 0; h <= maxH; h++) {
            if (isRainOnset(cur, prv, gap, h)) {
                events.add(createRainOnsetEvent(regionId, computedAt, h, cur.get(h)));
            }
        }
        return events;
    }

    /** 현재/이전 시계열과 gap 으로 비교 가능한 최대 시간 인덱스 계산 */
    private int computeMaxComparableHour(PopSeries24 cur, PopSeries24 prv, int gap) {
        if (gap < 0) return -1;

        int curSize = cur.size();
        int prvSize = prv.size();
        if (curSize <= 0 || prvSize <= 0) return -1;

        return Math.min(curSize - 1, prvSize - 1 - gap);
    }

    /** h 시점에서 “비가 새로 시작했는지” 여부 판단 */
    private boolean isRainOnset(PopSeries24 cur, PopSeries24 prv, int gap, int h) {
        int prevIdx = h + gap;
        int prevPop = prv.get(prevIdx);
        int curPop  = cur.get(h);

        boolean wasNotRain = prevPop < RAIN_TH;
        boolean nowRain    = curPop  >= RAIN_TH;

        return wasNotRain && nowRain;
    }

    private AlertEvent createRainOnsetEvent(int regionId,
                                            LocalDateTime computedAt,
                                            int hour,
                                            int pop) {
        Map<String, Object> payload = Map.of(
                "_srcRule", "RainOnsetChangeRule",
                "hour", hour,
                "pop",  pop
        );
        return new AlertEvent(
                AlertTypeEnum.RAIN_ONSET,
                regionId,
                computedAt,
                payload
        );
    }

    /** 캐시 무효화 */
    public void invalidate(int regionId) { cache.invalidate(regionId); }
    public void invalidateAll() { cache.invalidateAll(); }
}
