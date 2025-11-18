//package com.github.yun531.climate.support;
//
//import com.github.yun531.climate.dto.POPSnapDto;
//import org.springframework.lang.Nullable;
//
//public final class PopArrays {
//    private PopArrays() {}
//
//
//
//    /** 길이 23 (0~22) 비교용으로 왼쪽 1칸 시프트 */
//    public static int[] shiftLeftBy1ToLen23(int[] arr24) {
//        int[] out = new int[23];
//        System.arraycopy(arr24, 1, out, 0, 23);
//        return out;
//    }
//
//    public static int n(@Nullable Byte v) { return v == null ? 0 : (v & 0xFF); }
//}
