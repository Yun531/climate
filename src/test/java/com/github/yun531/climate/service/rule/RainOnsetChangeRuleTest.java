package com.github.yun531.climate.service.rule;


import com.github.yun531.climate.service.ClimateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RainOnsetChangeRuleTest {

    @Mock
    ClimateService climateService;

    @Test
    void 이전은비아님_현재는비임_교차시각에_AlertEvent_발생() {
        RainOnsetChangeRule rule = new RainOnsetChangeRule(climateService);

        int th = RainThresholdEnum.RAIN.getThreshold(); // 60
        // h=5에서 임계치 교차
        int[] current = new int[24];
        int[] prevShift = new int[23];
        for (int i = 0; i < 24; i++) current[i] = 0;
        for (int i = 0; i < 23; i++) prevShift[i] = 0;
        current[5] = th;                 // now ≥ 60
        prevShift[5] = th - 1;          // was < 60

        when(climateService.loadDefaultPopSeries(101L))
                .thenReturn(new ClimateService.PopSeries(current, prevShift));

        // when
        var events = rule.evaluate(List.of(101L), null);
        // then
        assertThat(events).hasSize(1);

        // when
        var e = events.get(0);
        // then
        assertThat(e.type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);
        assertThat(e.regionId()).isEqualTo(101L);
        assertThat((Integer)e.payload().get("hour")).isEqualTo(5);
        assertThat((Integer)e.payload().get("pop")).isEqualTo(th);

        assertThat(e.occurredAt()).isBeforeOrEqualTo(Instant.now());         // todo: since 관련 고민중
    }

    @Test
    void 이전부터_이미비임_교차없음_이벤트없음() {
        RainOnsetChangeRule rule = new RainOnsetChangeRule(climateService);

        int th = RainThresholdEnum.RAIN.getThreshold();
        int[] current = new int[24];
        int[] prevShift = new int[23];
        current[5] = th + 10;
        prevShift[5] = th + 1; // 이미 비

        when(climateService.loadDefaultPopSeries(7L))
                .thenReturn(new ClimateService.PopSeries(current, prevShift));

        // when
        var events = rule.evaluate(List.of(7L), null);
        // then
        assertThat(events).isEmpty();
    }

    @Test
    void 시계열없음_null이면_스킵() {
        RainOnsetChangeRule rule = new RainOnsetChangeRule(climateService);

        when(climateService.loadDefaultPopSeries(9L))
                .thenReturn(new ClimateService.PopSeries(null, null));

        var events = rule.evaluate(List.of(9L), null);
        assertThat(events).isEmpty();
    }
}