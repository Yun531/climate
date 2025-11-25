package com.github.yun531.climate.service;

import com.github.yun531.climate.dto.WarningKind;
import com.github.yun531.climate.dto.WarningLevel;
import com.github.yun531.climate.service.rule.AlertEvent;
import com.github.yun531.climate.service.rule.AlertTypeEnum;
import com.github.yun531.climate.service.rule.RainForecastRule;
import com.github.yun531.climate.service.rule.RainOnsetChangeRule;
import com.github.yun531.climate.service.rule.WarningIssuedRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    // 구체 타입으로 mock
    @Mock
    private RainOnsetChangeRule rainRule;      // supports() -> RAIN_ONSET
    @Mock
    private WarningIssuedRule warnRule;        // supports() -> WARNING_ISSUED
    @Mock
    private RainForecastRule forecastRule;     // supports() -> RAIN_FORECAST

    private NotificationService service;

    @BeforeEach
    void setUp() {
        // supports() 스텁
        lenient().when(rainRule.supports()).thenReturn(AlertTypeEnum.RAIN_ONSET);
        lenient().when(warnRule.supports()).thenReturn(AlertTypeEnum.WARNING_ISSUED);
        lenient().when(forecastRule.supports()).thenReturn(AlertTypeEnum.RAIN_FORECAST);

        // SUT
        service = new NotificationService(List.of(rainRule, warnRule, forecastRule));
    }

    // ======================================================
    // 1) 방어 로직
    // ======================================================

    @Test
    @DisplayName("request == null이면 빈 리스트를 반환하고 어떤 룰도 실행되지 않는다")
    void null_request_returns_empty_and_does_not_call_rules() {
        // when
        List<AlertEvent> out = service.generate(null);

        // then
        assertThat(out).isEmpty();
        verifyNoInteractions(rainRule, warnRule, forecastRule);
    }

    @Test
    @DisplayName("regionIds가 null이면 빈 리스트를 반환하고 룰이 실행되지 않는다")
    void null_regionIds_returns_empty_and_does_not_call_rules() {
        NotificationRequest request = new NotificationRequest(
                null,
                LocalDateTime.parse("2025-11-04T05:00:00"),
                EnumSet.of(AlertTypeEnum.RAIN_ONSET),
                null,
                null
        );

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).isEmpty();
        verifyNoInteractions(rainRule, warnRule, forecastRule);
    }

    @Test
    @DisplayName("regionIds가 empty면 빈 리스트를 반환하고 룰이 실행되지 않는다")
    void empty_regionIds_returns_empty_and_does_not_call_rules() {
        NotificationRequest request = new NotificationRequest(
                List.of(),
                LocalDateTime.parse("2025-11-04T05:00:00"),
                EnumSet.of(AlertTypeEnum.RAIN_ONSET),
                null,
                null
        );

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).isEmpty();
        verifyNoInteractions(rainRule, warnRule, forecastRule);
    }

    // ======================================================
    // 2) enabledTypes 기본/정상 동작
    // ======================================================

    @Test
    @DisplayName("기본값: enabledTypes가 null이면 RAIN_ONSET만 실행된다")
    void default_selects_only_rain_rule_when_null() {
        // given
        LocalDateTime t = LocalDateTime.parse("2025-11-04T05:00:00");
        int regionId = 1;

        NotificationRequest request = new NotificationRequest(
                List.of(regionId),
                t,
                null,               // enabledTypes
                null,               // filterKinds
                null                // rainHourLimit
        );

        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of(
                rainEvent(regionId, t, 5, 70)
        ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);
        assertThat(out.get(0).regionId()).isEqualTo(1);

        verify(rainRule, times(1)).evaluate(anyList(), any());
        verify(warnRule, never()).evaluate(anyList(), any());
        verify(forecastRule, never()).evaluate(anyList(), any());
    }

    @Test
    @DisplayName("기본값: enabledTypes가 empty이면 RAIN_ONSET만 실행된다")
    void default_selects_only_rain_rule_when_empty() {
        // given
        LocalDateTime t = LocalDateTime.parse("2025-11-04T05:00:00");
        int regionId = 1;

        NotificationRequest request = new NotificationRequest(
                List.of(regionId),
                t,
                Collections.emptySet(),   // enabledTypes empty
                null,
                null
        );

        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of(
                rainEvent(regionId, t, 5, 70)
        ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);

        verify(rainRule, times(1)).evaluate(anyList(), any());
        verify(warnRule, never()).evaluate(anyList(), any());
        verify(forecastRule, never()).evaluate(anyList(), any());
    }

    @Test
    @DisplayName("enabledTypes에 RAIN_ONSET, WARNING_ISSUED를 주면 두 룰 모두 실행된다")
    void enabled_both_rules() {
        // given
        int regionId01 = 1, regionId02 = 2;
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET, AlertTypeEnum.WARNING_ISSUED);
        NotificationRequest request = new NotificationRequest(
                List.of(regionId01),
                null,                              // since (null → nowMinutes)
                enabled,
                null,
                null
        );

        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T05:00:00");
        LocalDateTime t2 = LocalDateTime.parse("2025-11-04T06:00:00");
        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of(
                rainEvent(regionId01, t1, 5, 70)
        ));
        when(warnRule.evaluate(anyList(), any())).thenReturn(List.of(
                warningEvent(regionId02, t2, WarningKind.RAIN, WarningLevel.WARNING)
        ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(2);
        assertThat(out)
                .extracting(AlertEvent::type)
                .containsExactlyInAnyOrder(AlertTypeEnum.RAIN_ONSET, AlertTypeEnum.WARNING_ISSUED);

        verify(rainRule, times(1)).evaluate(anyList(), any());
        verify(warnRule, times(1)).evaluate(anyList(), any());
        verify(forecastRule, never()).evaluate(anyList(), any());
    }

    // ======================================================
    // 3) deduplicate / region cap / sort / since 전달
    // ======================================================

    @Test
    @DisplayName("deduplicate: 동일 (type|region|occurredAt|payload) 이벤트는 한 번만 남는다")
    void deduplicate_removes_duplicates() {
        // given
        int regionId = 1;
        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T05:00:00");
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        NotificationRequest request = new NotificationRequest(
                List.of(regionId),
                t1,
                enabled,
                null,
                null
        );

        AlertEvent dup1 = rainEvent(1, t1, 5, 70);
        AlertEvent dup2 = rainEvent(1, t1, 5, 70); // payload까지 동일
        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of(dup1, dup2));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);
        AlertEvent e = out.get(0);
        assertThat(e.type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);
        assertThat(e.regionId()).isEqualTo(1);
        assertThat(e.occurredAt()).isEqualTo(t1);
    }

    @Test
    @DisplayName("지역 ID는 최대 3개까지만 룰에 전달된다 (앞 3개 사용)")
    void region_is_capped_to_three() {
        // given
        List<Integer> regionIds = List.of(10, 11, 12, 13); // 4개 입력
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T05:00:00");
        NotificationRequest request = new NotificationRequest(
                regionIds,
                t1,
                enabled,
                null,
                null
        );

        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of());

        // when
        service.generate(request);

        // then: 전달된 regionIds는 앞 3개만
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(rainRule, times(1)).evaluate(captor.capture(), any());
        List<Integer> passed = captor.getValue();
        assertThat(passed).containsExactly(10, 11, 12);
    }

    @Test
    @DisplayName("정렬: 타입 → 지역 → 타입명 → 발생시각 순으로 정렬된다")
    void sort_by_region_type_then_time() {
        // given
        var regionIds = List.of(1, 2);
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET, AlertTypeEnum.WARNING_ISSUED);
        LocalDateTime t0 = LocalDateTime.parse("2025-11-04T04:00:00");
        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T05:00:00");
        LocalDateTime t2 = LocalDateTime.parse("2025-11-04T06:00:00");
        NotificationRequest request = new NotificationRequest(
                regionIds,
                t0,
                enabled,
                null,
                null
        );

        // region 2에 비 시작(t1), region 1에 비 시작(t2), region 1에 특보(t1)
        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of(
                rainEvent(2, t1, 5, 70),
                rainEvent(1, t2, 6, 80)
        ));
        when(warnRule.evaluate(anyList(), any())).thenReturn(List.of(
                warningEvent(1, t1, WarningKind.RAIN, WarningLevel.WARNING)
        ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(3);

        // 정렬 규칙: type(ordinal) → regionId → typeName → occurredAt
        AlertEvent e0 = out.get(0);
        AlertEvent e1 = out.get(1);
        AlertEvent e2 = out.get(2);

        assertThat(e0.type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);
        assertThat(e0.regionId()).isEqualTo(1);

        assertThat(e1.type()).isEqualTo(AlertTypeEnum.RAIN_ONSET);
        assertThat(e1.regionId()).isEqualTo(2);

        assertThat(e2.type()).isEqualTo(AlertTypeEnum.WARNING_ISSUED);
        assertThat(e2.regionId()).isEqualTo(1);
    }

    @Test
    @DisplayName("since 값이 룰 evaluate 로 그대로 전달된다")
    void since_is_forwarded_to_rules() {
        var regionIds = List.of(1);
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        LocalDateTime since = LocalDateTime.parse("2025-11-04T05:55:00");
        NotificationRequest request = new NotificationRequest(
                regionIds,
                since,
                enabled,
                null,
                null
        );

        when(rainRule.evaluate(anyList(), any())).thenReturn(List.of());

        // when
        service.generate(request);

        // then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(rainRule).evaluate(anyList(), captor.capture());
        assertThat(captor.getValue()).isEqualTo(since);
    }

    // ======================================================
    // 4) RAIN_FORECAST 관련
    // ======================================================

    @Test
    @DisplayName("RAIN_FORECAST: 룰이 만든 payload(hourlyParts/dayParts)를 그대로 전달한다")
    void forecast_payload_is_preserved() {
        // given
        LocalDateTime t = LocalDateTime.parse("2025-11-04T05:00:00");

        Map<String, Object> payload = new HashMap<>();
        payload.put("hourlyParts", List.of(List.of(9, 12)));      // 09~12시
        payload.put("dayParts",   List.of(List.of(1, 0)));        // 오늘 오전만 비

        when(forecastRule.evaluate(anyList(), any())).thenReturn(List.of(
                new AlertEvent(AlertTypeEnum.RAIN_FORECAST, 1, t, payload)
        ));

        var regionIds = List.of(1);
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_FORECAST);
        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T04:00:00");
        NotificationRequest request = new NotificationRequest(
                regionIds,
                t1,
                enabled,
                null,
                null
        );

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);
        AlertEvent e = out.get(0);
        assertThat(e.type()).isEqualTo(AlertTypeEnum.RAIN_FORECAST);
        assertThat(e.regionId()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        List<List<Integer>> hourly = (List<List<Integer>>) e.payload().get("hourlyParts");
        @SuppressWarnings("unchecked")
        List<List<Integer>> day = (List<List<Integer>>) e.payload().get("dayParts");

        assertThat(hourly).containsExactly(List.of(9, 12));
        assertThat(day).containsExactly(List.of(1, 0));

        verify(rainRule, never()).evaluate(anyList(), any());
        verify(warnRule, never()).evaluate(anyList(), any());
    }

    @Test
    @DisplayName("RAIN_FORECAST: 지역 3개 입력 시 각 지역별 이벤트가 그대로 전달된다")
    void forecast_three_regions_payload_and_regions() {
        // given
        LocalDateTime t = LocalDateTime.parse("2025-11-04T05:00:00");

        Map<String, Object> p1 = new HashMap<>();
        p1.put("hourlyParts", List.of(List.of(9, 12)));       // 09~12시 비
        p1.put("dayParts",   List.of(List.of(1, 0)));         // 오늘 오전만 비

        Map<String, Object> p2 = new HashMap<>();
        p2.put("hourlyParts", List.of(List.of(15, 15)));      // 15시 한 번만 비
        p2.put("dayParts",   List.of());                      // 일자 플래그 없음

        Map<String, Object> p3 = new HashMap<>();
        p3.put("hourlyParts", List.of());                     // 시간대 정보 없음
        p3.put("dayParts",   List.of(List.of(0, 1)));         // 오늘 오후만 비

        when(forecastRule.evaluate(anyList(), any())).thenReturn(List.of(
                new AlertEvent(AlertTypeEnum.RAIN_FORECAST, 1, t, p1),
                new AlertEvent(AlertTypeEnum.RAIN_FORECAST, 2, t, p2),
                new AlertEvent(AlertTypeEnum.RAIN_FORECAST, 3, t, p3)
        ));

        var regionIds = List.of(1, 2, 3);
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_FORECAST);
        LocalDateTime t1 = LocalDateTime.parse("2025-11-04T04:00:00");
        NotificationRequest request = new NotificationRequest(
                regionIds,
                t1,
                enabled,
                null,
                null
        );

        // when
        List<AlertEvent> out = service.generate(request);

        // then: 3개 지역 각각 한 이벤트
        assertThat(out).hasSize(3);
        assertThat(out)
                .extracting(AlertEvent::regionId)
                .containsExactly(1, 2, 3);

        @SuppressWarnings("unchecked")
        List<List<Integer>> hourly1 = (List<List<Integer>>) out.get(0).payload().get("hourlyParts");
        @SuppressWarnings("unchecked")
        List<List<Integer>> day1 = (List<List<Integer>>) out.get(0).payload().get("dayParts");

        @SuppressWarnings("unchecked")
        List<List<Integer>> hourly2 = (List<List<Integer>>) out.get(1).payload().get("hourlyParts");
        @SuppressWarnings("unchecked")
        List<List<Integer>> day2 = (List<List<Integer>>) out.get(1).payload().get("dayParts");

        @SuppressWarnings("unchecked")
        List<List<Integer>> hourly3 = (List<List<Integer>>) out.get(2).payload().get("hourlyParts");
        @SuppressWarnings("unchecked")
        List<List<Integer>> day3 = (List<List<Integer>>) out.get(2).payload().get("dayParts");

        assertThat(hourly1).containsExactly(List.of(9, 12));
        assertThat(day1).containsExactly(List.of(1, 0));

        assertThat(hourly2).containsExactly(List.of(15, 15));
        assertThat(day2).isEmpty();

        assertThat(hourly3).isEmpty();
        assertThat(day3).containsExactly(List.of(0, 1));

        // 전달된 regionIds가 정확히 [1,2,3]인지 검증
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(forecastRule, times(1)).evaluate(captor.capture(), any());
        assertThat(captor.getValue()).containsExactly(1, 2, 3);

        verify(rainRule, never()).evaluate(anyList(), any());
        verify(warnRule, never()).evaluate(anyList(), any());
    }

    // ======================================================
    // 5) filterKinds / rainHourLimit 분기
    // ======================================================

    @Test
    @DisplayName("filterWarningKinds가 있으면 WARNING_ISSUED 룰은 WarningIssuedRule.evaluate(regionIds, kinds, since)를 사용한다")
    void filter_kinds_uses_warningIssuedRule_specialized_overload() {
        // given
        var regionIds = List.of(1, 2);
        LocalDateTime since = LocalDateTime.parse("2025-11-04T05:00:00");
        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.WARNING_ISSUED);
        Set<WarningKind> kinds = EnumSet.of(WarningKind.RAIN);

        NotificationRequest request = new NotificationRequest(
                regionIds,
                since,
                enabled,
                kinds,      // filterKinds
                null        // rainHourLimit
        );

        when(warnRule.evaluate(anyList(), anySet(), any()))
                .thenReturn(List.of(
                        warningEvent(1, since, WarningKind.RAIN, WarningLevel.WARNING)
                ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> regionCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<WarningKind>> kindCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(warnRule, times(1))
                .evaluate(regionCaptor.capture(), kindCaptor.capture(), sinceCaptor.capture());

        assertThat(regionCaptor.getValue()).containsExactly(1, 2);
        assertThat(kindCaptor.getValue()).containsExactly(WarningKind.RAIN);
        assertThat(sinceCaptor.getValue()).isEqualTo(since);
    }

    @Test
    @DisplayName("rainHourLimit가 있으면 RAIN_ONSET 룰은 RainOnsetChangeRule.evaluate(regionIds, since, hourLimit)를 사용한다")
    void rainHourLimit_uses_rainOnsetChangeRule_specialized_overload() {
        // given
        var regionIds = List.of(10, 11, 12, 13); // 4개 → limitRegions로 앞 3개
        LocalDateTime since = LocalDateTime.parse("2025-11-04T05:00:00");
        int limitHour = 12;

        Set<AlertTypeEnum> enabled = EnumSet.of(AlertTypeEnum.RAIN_ONSET);
        NotificationRequest request = new NotificationRequest(
                regionIds,
                since,
                enabled,
                null,
                limitHour
        );

        when(rainRule.evaluate(anyList(), any(), anyInt()))
                .thenReturn(List.of(
                        rainEvent(10, since, 9, 80)
                ));

        // when
        List<AlertEvent> out = service.generate(request);

        // then
        assertThat(out).hasSize(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> regionCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);

        verify(rainRule, times(1))
                .evaluate(regionCaptor.capture(), sinceCaptor.capture(), limitCaptor.capture());

        assertThat(regionCaptor.getValue()).containsExactly(10, 11, 12); // limitRegions 적용
        assertThat(sinceCaptor.getValue()).isEqualTo(since);
        assertThat(limitCaptor.getValue()).isEqualTo(limitHour);
    }

    // ---- helpers ----
    private static AlertEvent rainEvent(int regionId, LocalDateTime t, int hour, int pop) {
        return new AlertEvent(
                AlertTypeEnum.RAIN_ONSET,
                regionId,
                t,
                Map.of("hour", hour, "pop", pop)
        );
    }

    private static AlertEvent warningEvent(int regionId, LocalDateTime t, WarningKind kind, WarningLevel level) {
        return new AlertEvent(
                AlertTypeEnum.WARNING_ISSUED,
                regionId,
                t,
                Map.of("kind", kind, "level", level)
        );
    }
}