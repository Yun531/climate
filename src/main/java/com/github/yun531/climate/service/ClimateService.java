package com.github.yun531.climate.service;

import com.github.yun531.climate.dto.*;
import com.github.yun531.climate.repository.ClimateSnapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClimateService {

    private static final int SNAP_CURRENT = SnapKindEnum.SNAP_CURRENT.getCode();
    private static final int SNAP_PREV    = SnapKindEnum.SNAP_PREVIOUS.getCode();

    private final ClimateSnapRepository climateSnapRepository;

    public PopSeries loadDefaultPopSeries(int regionId) {
        return loadPopSeries(regionId, SNAP_CURRENT, SNAP_PREV);
    }

    public ForecastSeries loadDefaultForecastSeries(int regionId) {
        return loadForecastSeries(regionId, SNAP_CURRENT);
    }

    /** 비(POP) 판정에 필요한 시계열을 로드 (현재*이전 스냅샷) */
    public PopSeries loadPopSeries(int regionId, int currentSnapId, int previousSnapId) {
        List<Integer> snapIds = List.of(currentSnapId, previousSnapId);
        List<POPSnapDto> snaps = fetchSnaps(regionId, snapIds);

        POPSnapDto cur = findSnapById(snaps, currentSnapId);
        POPSnapDto prv = findSnapById(snaps, previousSnapId);

        if (cur == null || prv == null) {
            return emptyPopSeries();
        }

        int reportTimeGap = computeReportTimeGap(prv.getReportTime(), cur.getReportTime());
        LocalDateTime curReportTime = cur.getReportTime();

        return new PopSeries(
                cur.getHourly(),
                prv.getHourly(),
                reportTimeGap,
                curReportTime
        );
    }

    /** 예보 요약용: 스냅에서 시간대 [24] + 오전/오후[14] */
    public ForecastSeries loadForecastSeries(int regionId, int snapId) {
        POPSnapDto dto = fetchSingleSnap(regionId, snapId);
        if (dto == null) {
            return emptyForecastSeries();
        }
        return new ForecastSeries(dto.getHourly(), dto.getDaily());
    }

    /** 레포지토리 접근 */
    private List<POPSnapDto> fetchSnaps(int regionId, List<Integer> snapIds) {
        return climateSnapRepository.findPopInfoBySnapIdsAndRegionId(snapIds, regionId);
    }

    private POPSnapDto fetchSingleSnap(int regionId, int snapId) {
        List<POPSnapDto> rows =
                climateSnapRepository.findPopInfoBySnapIdsAndRegionId(List.of(snapId), regionId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 스냅 선택 */
    private POPSnapDto findSnapById(List<POPSnapDto> snaps, int snapId) {
        for (POPSnapDto snap : snaps) {
            if (snap.getSnapId() == snapId) {
                return snap;
            }
        }
        return null;
    }

    /** 발표시간 갭 계산 */
    private int computeReportTimeGap(LocalDateTime previous, LocalDateTime current) {
        if (previous == null || current == null) {
            return 0;
        }
        long minutes = Duration.between(previous, current).toMinutes();
        return (int) Math.round(minutes / 60.0);
    }

    /** 빈 결과 생성 */
    private PopSeries emptyPopSeries() {
        return new PopSeries(null, null, 0, null);
    }
    private ForecastSeries emptyForecastSeries() {
        return new ForecastSeries(null, null);
    }
}