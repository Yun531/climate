package com.github.yun531.climate.service;

import com.github.yun531.climate.dto.POPSnapDto;
import com.github.yun531.climate.repository.ClimateSnapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClimateServiceTest {

    @Mock
    ClimateSnapRepository climateSnapRepository;

    ClimateService climateService;

    @BeforeEach
    void setUp() {
        climateService = new ClimateService(climateSnapRepository);
    }

    @Test
    void loadPopSeries_정상_현재24_이전시프트23_길이검증_및_값검증() {
        // given
        Long regionId = 100L;
        Long curId = 1L;
        Long prvId = 10L;

        POPSnapDto cur = new POPSnapDto();
        cur.setSnapId(curId);
        cur.setRegionId(regionId);
        fillHourly(cur, 0);            // 0,1,2,...,23

        POPSnapDto prv = new POPSnapDto();
        prv.setSnapId(prvId);
        prv.setRegionId(regionId);
        fillHourly(prv, 10);           // 10,11,12,...,33

        when(climateSnapRepository.findPopInfoBySnapIdsAndRegionId(List.of(curId, prvId), regionId))
                .thenReturn(List.of(cur, prv));   // mocking

        // when
        ClimateService.PopSeries series = climateService.loadPopSeries(regionId, curId, prvId);

        // then
        assertThat(series.current()).hasSize(24);
        assertThat(series.previousShifted()).hasSize(23);

        // current: 0..23
        for (int i = 0; i < 24; i++) {
            assertThat(series.current()[i]).isEqualTo(i);
        }
        // prvShift: prv[1..23] → 11..33
        for (int i = 0; i < 23; i++) {
            assertThat(series.previousShifted()[i]).isEqualTo(11 + i);
        }

        // given
        // null → 0 변환 확인 (예: A05 null)
        cur.setPopA05(null);
        when(climateSnapRepository.findPopInfoBySnapIdsAndRegionId(List.of(curId, prvId), regionId))
                .thenReturn(List.of(cur, prv));

        // when
        series = climateService.loadPopSeries(regionId, curId, prvId);

        // then
        assertThat(series.current()[5]).isEqualTo(0);
    }

    @Test
    void loadDefaultPopSeries_레포호출파라미터_검증() {
        // given
        Long regionId = 7L;
        POPSnapDto cur = new POPSnapDto(); cur.setSnapId(1L); cur.setRegionId(regionId);
        POPSnapDto prv = new POPSnapDto(); prv.setSnapId(10L); prv.setRegionId(regionId);
        fillHourly(cur, 0);
        fillHourly(prv, 1);

        when(climateSnapRepository.findPopInfoBySnapIdsAndRegionId(List.of(1L, 10L), regionId))
                .thenReturn(List.of(cur, prv));

        // when
        climateService.loadDefaultPopSeries(regionId);

        // then
        verify(climateSnapRepository, times(1))
                .findPopInfoBySnapIdsAndRegionId(List.of(1L, 10L), regionId);
    }

    // ---- helpers ----
    private void fillHourly(POPSnapDto d, int base) {
        d.setPopA00((byte)(base + 0));  d.setPopA01((byte)(base + 1));  d.setPopA02((byte)(base + 2));  d.setPopA03((byte)(base + 3));
        d.setPopA04((byte)(base + 4));  d.setPopA05((byte)(base + 5));  d.setPopA06((byte)(base + 6));  d.setPopA07((byte)(base + 7));
        d.setPopA08((byte)(base + 8));  d.setPopA09((byte)(base + 9));  d.setPopA10((byte)(base +10));  d.setPopA11((byte)(base +11));
        d.setPopA12((byte)(base +12));  d.setPopA13((byte)(base +13));  d.setPopA14((byte)(base +14));  d.setPopA15((byte)(base +15));
        d.setPopA16((byte)(base +16));  d.setPopA17((byte)(base +17));  d.setPopA18((byte)(base +18));  d.setPopA19((byte)(base +19));
        d.setPopA20((byte)(base +20));  d.setPopA21((byte)(base +21));  d.setPopA22((byte)(base +22));  d.setPopA23((byte)(base +23));
    }
}