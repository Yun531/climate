package com.github.yun531.climate.service;

import com.github.yun531.climate.dto.POPSnapDto;
import com.github.yun531.climate.repository.ClimateSnapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClimateService {

    private final ClimateSnapRepository climateSnapRepository;

    /** 비(POP) 판정에 필요한 시계열을 로드 (현재, 이전스냅샷-좌측1칸시프트) */
    public PopSeries loadPopSeries(Long regionId, Long currentSnapId, Long previousSnapId) {
        List<Long> ids = List.of(currentSnapId, previousSnapId);
        List<POPSnapDto> snaps = climateSnapRepository.findPopInfoBySnapIdsAndRegionId(ids, regionId);

        POPSnapDto cur = null, prv = null;
        for (POPSnapDto s : snaps) {
            if (s.getSnapId() == currentSnapId) cur = s;
            else if (s.getSnapId() == previousSnapId) prv = s;
        }
        if (cur == null || prv == null) return new PopSeries(null, null);

        int[] curArr = toHourlyArray(cur);
        int[] prvArr = toHourlyArray(prv);

        // 이전 스냅샷을 왼쪽 1칸 시프트 (0~22 비교용)
        int[] prvShift = new int[23];
        System.arraycopy(prvArr, 1, prvShift, 0, 23);
        return new PopSeries(curArr, prvShift);
    }

    /** 편의 메서드: 디폴트 스냅샷 id 변화에 대응 (예: 1=현재, 10=이전) */
    public PopSeries loadDefaultPopSeries(Long regionId) {
        return loadPopSeries(regionId, 1L, 10L);
    }

    private int[] toHourlyArray(POPSnapDto dto) {
        return new int[]{
                n(dto.getPopA00()), n(dto.getPopA01()), n(dto.getPopA02()), n(dto.getPopA03()), n(dto.getPopA04()),
                n(dto.getPopA05()), n(dto.getPopA06()), n(dto.getPopA07()), n(dto.getPopA08()), n(dto.getPopA09()),
                n(dto.getPopA10()), n(dto.getPopA11()), n(dto.getPopA12()), n(dto.getPopA13()), n(dto.getPopA14()),
                n(dto.getPopA15()), n(dto.getPopA16()), n(dto.getPopA17()), n(dto.getPopA18()), n(dto.getPopA19()),
                n(dto.getPopA20()), n(dto.getPopA21()), n(dto.getPopA22()), n(dto.getPopA23())
        };
    }

    private int n(@Nullable Byte v) { return v == null ? 0 : (v & 0xFF); }

    /** 판정용 입력 구조체 (현재 POP[24], 이전스냅 시프트 POP[23]) */
    public record PopSeries(@Nullable int[] current, @Nullable int[] previousShifted) {}
}