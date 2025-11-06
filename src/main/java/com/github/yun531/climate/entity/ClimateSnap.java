package com.github.yun531.climate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "climate_snap")
public class ClimateSnap {

    @Id
    @Column(name = "snap_id")
    private long snapId;

    @Column(name = "region_id", nullable = false)
    private long regionId;

    // ---- 시간대별 온도 (00~23시) ----
    @Column(name = "temp_A00") private Byte tempA00; @Column(name = "temp_A01") private Byte tempA01; @Column(name = "temp_A02") private Byte tempA02;
    @Column(name = "temp_A03") private Byte tempA03; @Column(name = "temp_A04") private Byte tempA04; @Column(name = "temp_A05") private Byte tempA05;
    @Column(name = "temp_A06") private Byte tempA06; @Column(name = "temp_A07") private Byte tempA07; @Column(name = "temp_A08") private Byte tempA08;
    @Column(name = "temp_A09") private Byte tempA09; @Column(name = "temp_A10") private Byte tempA10; @Column(name = "temp_A11") private Byte tempA11;
    @Column(name = "temp_A12") private Byte tempA12; @Column(name = "temp_A13") private Byte tempA13; @Column(name = "temp_A14") private Byte tempA14;
    @Column(name = "temp_A15") private Byte tempA15; @Column(name = "temp_A16") private Byte tempA16; @Column(name = "temp_A17") private Byte tempA17;
    @Column(name = "temp_A18") private Byte tempA18; @Column(name = "temp_A19") private Byte tempA19; @Column(name = "temp_A20") private Byte tempA20;
    @Column(name = "temp_A21") private Byte tempA21; @Column(name = "temp_A22") private Byte tempA22; @Column(name = "temp_A23") private Byte tempA23;

    // ---- 일자별 오전/오후 온도 ----
    @Column(name = "temp_A0d_am") private Byte tempA0dAm; @Column(name = "temp_A0d_pm") private Byte tempA0dPm;
    @Column(name = "temp_A1d_am") private Byte tempA1dAm; @Column(name = "temp_A1d_pm") private Byte tempA1dPm;
    @Column(name = "temp_A2d_am") private Byte tempA2dAm; @Column(name = "temp_A2d_pm") private Byte tempA2dPm;
    @Column(name = "temp_A3d_am") private Byte tempA3dAm; @Column(name = "temp_A3d_pm") private Byte tempA3dPm;
    @Column(name = "temp_A4d_am") private Byte tempA4dAm; @Column(name = "temp_A4d_pm") private Byte tempA4dPm;
    @Column(name = "temp_A5d_am") private Byte tempA5dAm; @Column(name = "temp_A5d_pm") private Byte tempA5dPm;
    @Column(name = "temp_A6d_am") private Byte tempA6dAm; @Column(name = "temp_A6d_pm") private Byte tempA6dPm;


    // ---- 시간대별 강수확률 (00~23시) ----
    @Column(name = "POP_A00") private Byte popA00; @Column(name = "POP_A01") private Byte popA01; @Column(name = "POP_A02") private Byte popA02;
    @Column(name = "POP_A03") private Byte popA03; @Column(name = "POP_A04") private Byte popA04; @Column(name = "POP_A05") private Byte popA05;
    @Column(name = "POP_A06") private Byte popA06; @Column(name = "POP_A07") private Byte popA07; @Column(name = "POP_A08") private Byte popA08;
    @Column(name = "POP_A09") private Byte popA09; @Column(name = "POP_A10") private Byte popA10; @Column(name = "POP_A11") private Byte popA11;
    @Column(name = "POP_A12") private Byte popA12; @Column(name = "POP_A13") private Byte popA13; @Column(name = "POP_A14") private Byte popA14;
    @Column(name = "POP_A15") private Byte popA15; @Column(name = "POP_A16") private Byte popA16; @Column(name = "POP_A17") private Byte popA17;
    @Column(name = "POP_A18") private Byte popA18; @Column(name = "POP_A19") private Byte popA19; @Column(name = "POP_A20") private Byte popA20;
    @Column(name = "POP_A21") private Byte popA21; @Column(name = "POP_A22") private Byte popA22; @Column(name = "POP_A23") private Byte popA23;

    // ---- 일자별 오전/오후 강수확률 ----
    @Column(name = "POP_A0d_am") private Byte popA0dAm; @Column(name = "POP_A0d_pm") private Byte popA0dPm;
    @Column(name = "POP_A1d_am") private Byte popA1dAm; @Column(name = "POP_A1d_pm") private Byte popA1dPm;
    @Column(name = "POP_A2d_am") private Byte popA2dAm; @Column(name = "POP_A2d_pm") private Byte popA2dPm;
    @Column(name = "POP_A3d_am") private Byte popA3dAm; @Column(name = "POP_A3d_pm") private Byte popA3dPm;
    @Column(name = "POP_A4d_am") private Byte popA4dAm; @Column(name = "POP_A4d_pm") private Byte popA4dPm;
    @Column(name = "POP_A5d_am") private Byte popA5dAm; @Column(name = "POP_A5d_pm") private Byte popA5dPm;
    @Column(name = "POP_A6d_am") private Byte popA6dAm; @Column(name = "POP_A6d_pm") private Byte popA6dPm;
}
