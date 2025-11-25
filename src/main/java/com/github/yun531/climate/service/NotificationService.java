package com.github.yun531.climate.service;

import com.github.yun531.climate.dto.WarningKind;
import com.github.yun531.climate.service.rule.*;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.yun531.climate.util.TimeUtil.nowMinutes;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<AlertRule> rules; // @Component 룰 자동 주입 (RAIN_ONSET, WARNING_ISSUED 등)

    public List<AlertEvent> generate(NotificationRequest request) {
        if (request == null) {
            return List.of();
        }

        List<Integer> regionIds = request.regionIds();
        if (regionIds == null || regionIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime effectiveSince    = sinceOrNow(request.since());
        Set<AlertTypeEnum> enabledTypes = normalizeEnabledTypes(request.enabledTypes());
        Set<WarningKind> filterKinds    = request.filterWarningKinds();
        Integer rainHourLimit           = request.rainHourLimit();

        List<Integer> targetRegions = limitRegions(regionIds);
        List<AlertEvent> events     =
                collectEvents(targetRegions, enabledTypes, effectiveSince, filterKinds, rainHourLimit);
        List<AlertEvent> deduped    = deduplicate(events);
        sortEvents(deduped);

        return deduped;
    }

    /** 룰 실행 / 이벤트 수집 */
    private List<AlertEvent> collectEvents(List<Integer> targetRegions,
                                           Set<AlertTypeEnum> enabledTypes,
                                           LocalDateTime since,
                                           @Nullable Set<WarningKind> filterKinds,
                                           @Nullable Integer rainHourLimit) {

        return rules.stream()
                .filter(r -> enabledTypes.contains(r.supports()))
                .flatMap(r -> evaluateRuleWithFilter(r, targetRegions, since, filterKinds, rainHourLimit).stream())
                .collect(Collectors.toList());
    }

    /**
     * - filterWarningKinds 가 있을 때 WARNING_ISSUED 룰 → WarningIssuedRule.evaluate(regionIds, filterWarningKinds, since)
     * - rainHourLimit 가 있을 때 RAIN_ONSET 룰 → RainOnsetChangeRule.evaluate(regionIds, since, rainHourLimit)
     * - 나머지는 기본 AlertRule.evaluate(regionIds, since)
     */
    private List<AlertEvent> evaluateRuleWithFilter(AlertRule rule,
                                                    List<Integer> targetRegions,
                                                    LocalDateTime since,
                                                    @Nullable Set<WarningKind> filterKinds,
                                                    @Nullable Integer rainHourLimit) {

        // 1) 특보 종류 필터
        if (filterKinds != null
                && rule.supports() == AlertTypeEnum.WARNING_ISSUED
                && rule instanceof WarningIssuedRule wir) {
            return wir.evaluate(targetRegions, filterKinds, since);
        }

        // 2) 비 시작 시간 상한(0~23) 필터
        if (rainHourLimit != null
                && rule.supports() == AlertTypeEnum.RAIN_ONSET
                && rule instanceof RainOnsetChangeRule roc) {
            return roc.evaluate(targetRegions, since, rainHourLimit);
        }

        // 3) 기본 동작
        return rule.evaluate(targetRegions, since);
    }

    /** 중복 제거 / 정렬 / 문자열 변환 */
    private List<AlertEvent> deduplicate(List<AlertEvent> events) {
        Map<String, AlertEvent> map = new LinkedHashMap<>();
        for (AlertEvent event : events) {
            String key = keyOf(event);
            map.putIfAbsent(key, event);
        }
        return new ArrayList<>(map.values());
    }

    /** <type>|<regionId>|<occurredAt>|<payLoad> 형태로 생성 */
    private String keyOf(AlertEvent event) {
        String type = (event.type() == null) ? "?" : event.type().name();
        String region = String.valueOf(event.regionId());
        String ts = (event.occurredAt() == null) ? "?" : event.occurredAt().toString();

        String payloadKey = normalizePayload(event.payload());

        return type + "|" + region + "|" + ts + "|" + payloadKey;
    }

    private String normalizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return "-";
        }

        return payload.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + stringify(e.getValue()))
                .reduce((a, b) -> a + "," + b)
                .orElse("-");
    }

    private String stringify(Object v) {
        if (v == null) return "null";
        return String.valueOf(v);
    }

    /** event 정렬: 타입(ordinal) → 지역ID → 타입 이름 → 발생시각 순 */
    private void sortEvents(List<AlertEvent> events) {
        events.sort(Comparator
                .comparing(AlertEvent::type,
                        Comparator.nullsLast(Comparator.comparingInt(Enum::ordinal)))
                .thenComparing(AlertEvent::regionId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(event -> event.type().name())
                .thenComparing(AlertEvent::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /** since 가 null이면 현재 시각을 사용 */
    private LocalDateTime sinceOrNow(@Nullable LocalDateTime since) {
        return (since != null) ? since : nowMinutes();
    }

    /** receiveWarnings 플래그 기반 기본 타입 셋 */
    private Set<AlertTypeEnum> defaultEnabledTypes(boolean receiveWarnings) {
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        if (receiveWarnings) {
            enabled.add(AlertTypeEnum.WARNING_ISSUED);
        }
        return enabled;
    }

    /** null/빈 값이면 기본(RAIN_ONSET)으로 교체, 아니면 복사 */
    private Set<AlertTypeEnum> normalizeEnabledTypes(@Nullable Set<AlertTypeEnum> enabledTypes) {
        if (enabledTypes == null || enabledTypes.isEmpty()) {
            return EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        }
        return EnumSet.copyOf(enabledTypes);
    }

    /** 지역 최대 3개 제한 */
    private List<Integer> limitRegions(List<Integer> regionIds) {
        if (regionIds.size() <= 3) {
            return regionIds;
        }
        return regionIds.subList(0, 3);
    }
}
