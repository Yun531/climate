package com.github.yun531.climate.service.rule;

import java.time.Instant;
import java.util.Map;

public record AlertEvent(
        AlertTypeEnum type,
        Long regionId,
        Instant occurredAt,
        Map<String, Object> payload // 시간대, 단계 등 부가정보
) {}
