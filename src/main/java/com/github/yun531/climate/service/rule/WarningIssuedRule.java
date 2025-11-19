package com.github.yun531.climate.service.rule;


import com.github.yun531.climate.dto.WarningKind;
import com.github.yun531.climate.dto.WarningStateDto;
import com.github.yun531.climate.service.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WarningIssuedRule implements AlertRule {

    private final WarningService warningService;

    @Override
    public AlertTypeEnum supports() {
        return AlertTypeEnum.WARNING_ISSUED;
    }

    @Override
    public List<AlertEvent> evaluate(List<Integer> regionIds, LocalDateTime since) {
        if (regionIds == null || regionIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, Map<WarningKind, WarningStateDto>> latestByRegion =
                warningService.findLatestByRegionAndKind(regionIds);

        LocalDateTime adjustedSince = adjustSince(since);

        List<AlertEvent> out = new ArrayList<>();
        for (int regionId : regionIds) {
            Map<WarningKind, WarningStateDto> byKind = latestByRegion.get(regionId);
            collectEventsForRegion(regionId, byKind, adjustedSince, out);
        }
        return out;
    }

    /** 특보 발효 시각과의 시차 보정을 위해 since 를 90분 당겨서 사용 */
    private LocalDateTime adjustSince(LocalDateTime since) {
        if (since == null) {
            return null;
        }
        return since.minusMinutes(90);
    }

    // 한 지역에 대한 이벤트 수집
    private void collectEventsForRegion(int regionId,
                                        Map<WarningKind, WarningStateDto> byKind,
                                        LocalDateTime adjustedSince,
                                        List<AlertEvent> out) {
        if (byKind == null || byKind.isEmpty()) {
            return;
        }

        for (Map.Entry<WarningKind, WarningStateDto> entry : byKind.entrySet()) {
            WarningStateDto state = entry.getValue();
            if (!isNewWarning(state, adjustedSince)) {
                continue;
            }

            AlertEvent event = toAlertEvent(regionId, state);
            out.add(event);
        }
    }

    // 새로 발효된 특보인지 판단
    private boolean isNewWarning(WarningStateDto state, LocalDateTime adjustedSince) {
        if (state == null) {
            return false;
        }
        // adjustedSince 가 null 이면 “전부 새로 발효된 것으로 간주”
        if (adjustedSince == null) {
            return true;
        }
        return warningService.isNewlyIssuedSince(state, adjustedSince);
    }

    // DTO → AlertEvent 변환
    private AlertEvent toAlertEvent(int regionId, WarningStateDto state) {
        LocalDateTime occurredAt =
                (state.getUpdatedAt() != null) ? state.getUpdatedAt() : LocalDateTime.now();

        Map<String, Object> payload = Map.of(
                "_srcRule", "WarningIssuedRule",
                "kind",  state.getKind(),   // WarningKind enum
                "level", state.getLevel()   // WarningLevel enum
        );

        return new AlertEvent(
                AlertTypeEnum.WARNING_ISSUED,
                regionId,
                occurredAt,
                payload
        );
    }
}