package com.wuye.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public final class NoGenerator {

    private static final AtomicLong SEQ = new AtomicLong(1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private NoGenerator() {
    }

    public static String billNo() {
        return "B" + FORMATTER.format(LocalDateTime.now()) + leftPad(SEQ.getAndIncrement() % 1000, 3);
    }

    public static String payOrderNo() {
        return "P" + FORMATTER.format(LocalDateTime.now()) + leftPad(SEQ.getAndIncrement() % 1000, 3);
    }

    private static String leftPad(long value, int length) {
        return String.format("%0" + length + "d", value);
    }
}
