package com.github.yun531.climate.service.rule;

import com.github.yun531.climate.dto.ForecastSeries;
import com.github.yun531.climate.dto.PopDailySeries7;
import com.github.yun531.climate.dto.PopSeries24;
import com.github.yun531.climate.dto.SnapKindEnum;
import com.github.yun531.climate.service.ClimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RainForecastRule implements AlertRule {

    private final ClimateService climateService;

    private static final int TH = 60;
    private static final int SNAP_CURRENT = SnapKindEnum.SNAP_CURRENT.getCode();

    @Override
    public AlertTypeEnum supports() {
        return AlertTypeEnum.RAIN_FORECAST;
    }

    @Override
    public List<AlertEvent> evaluate(List<Integer> regionIds, LocalDateTime since) {
        if (regionIds == null || regionIds.isEmpty()) return List.of();

        List<AlertEvent> out = new ArrayList<>(regionIds.size());

        for (int regionId : regionIds) {
            ForecastSeries fs = loadForecastSeries(regionId);
            if (fs == null) continue;

            List<List<Integer>> hourlyParts = buildHourlyParts(fs);
            List<List<Integer>> dayParts    = buildDayParts(fs);

            Map<String, Object> payload = createPayload(hourlyParts, dayParts);

            AlertEvent event = new AlertEvent(
                                        AlertTypeEnum.RAIN_FORECAST,
                                        regionId,
                                        LocalDateTime.now(),
                                        payload
                                );
            out.add(event);
        }
        return out;
    }

    private ForecastSeries loadForecastSeries(int regionId) {
        ForecastSeries fs =
                climateService.loadForecastSeries(regionId, SNAP_CURRENT);

        if (fs == null || (fs.hourly() == null && fs.daily() == null)) {
            return null;
        }
        return fs;
    }

    /**
     * 시간대별 POP 24시간에서
     * 연속으로 비가 오는 구간들을 [startIdx, endIdx] 형태로 리턴.
     */
    private List<List<Integer>> buildHourlyParts(ForecastSeries fs) {
        PopSeries24 series = fs.hourly();
        if (series == null) return List.of();

        int size = Math.min(series.size(), 24);
        if (size == 0) return List.of();

        return collectHourlyRainIntervals(series, size);
    }

    private List<List<Integer>> collectHourlyRainIntervals(PopSeries24 series, int size) {
        List<List<Integer>> parts = new ArrayList<>();

        int hourIdx = 0;
        while (hourIdx < size) {
            int startIdx = findNextRainStart(series, hourIdx, size);
            if (startIdx == -1) break;

            int endIdx = findRainEnd(series, startIdx, size);

            parts.add(List.of(startIdx, endIdx));
            hourIdx = endIdx + 1;
        }

        return parts.isEmpty() ? List.of() : List.copyOf(parts);
    }

    /** 다음 강수 시작 인덱스를 찾고, 없으면 -1 */
    private int findNextRainStart(PopSeries24 series, int start, int size) {
        int idx = start;
        while (idx < size && series.get(idx) < TH) {
            idx++;
        }
        return (idx >= size) ? -1 : idx;
    }

    /** start 인덱스에서 시작하는 연속 강수 구간의 끝 인덱스 */
    private int findRainEnd(PopSeries24 series, int start, int size) {
        int idx = start;
        while (idx + 1 < size && series.get(idx + 1) >= TH) {
            idx++;
        }
        return idx;
    }

    /**
     * D+0 ~ (N-1)일에 대해
     * 각 일자를 [amFlag, pmFlag] 로 표현한 2차원 리스트를 리턴.
     * amFlag: 오전 POP >= TH 이면 1, 아니면 0
     * pmFlag: 오후 POP >= TH 이면 1, 아니면 0
     */
    private List<List<Integer>> buildDayParts(ForecastSeries fs) {
        PopDailySeries7 daily = fs.daily();
        if (daily == null || daily.getDays() == null || daily.getDays().isEmpty()) {
            return List.of();
        }

        List<List<Integer>> parts = new ArrayList<>(daily.getDays().size());
        for (PopDailySeries7.DailyPop p : daily.getDays()) {
            parts.add(createDayFlagRow(p));
        }

        return parts.isEmpty() ? List.of() : List.copyOf(parts);
    }

    /** 한 일자의 [amFlag, pmFlag] 생성 */
    private List<Integer> createDayFlagRow(PopDailySeries7.DailyPop p) {
        int amFlag = (p.getAm() >= TH) ? 1 : 0;
        int pmFlag = (p.getPm() >= TH) ? 1 : 0;
        return List.of(amFlag, pmFlag);
    }

    private Map<String, Object> createPayload(List<List<Integer>> hourlyParts,
                                              List<List<Integer>> dayParts) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("_srcRule", "RainForecastRule");
        payload.put("hourlyParts", hourlyParts);
        payload.put("dayParts", dayParts);
        return payload;
    }
}