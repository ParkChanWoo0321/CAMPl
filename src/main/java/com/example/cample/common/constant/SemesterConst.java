package com.example.cample.common.constant;

import java.time.LocalDate;

public final class SemesterConst {
    private SemesterConst() {}

    public static final String SEMESTER_CODE = "2025-2";
    // 학사 일정에 맞게 START만 실제 값으로 조정하라.
    public static final LocalDate SEMESTER_START = LocalDate.of(2025, 9, 1);
    public static final LocalDate SEMESTER_END   = LocalDate.of(2025, 12, 19); // 종강 주 포함(12/15~19)
}
