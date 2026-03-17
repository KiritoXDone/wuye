package com.wuye.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public final class FileNoGenerator {

    private static final AtomicLong SEQ = new AtomicLong(1);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private FileNoGenerator() {
    }

    public static String importBatchNo() {
        return "IMP-" + FORMATTER.format(LocalDateTime.now()) + leftPad(SEQ.getAndIncrement() % 1000, 3);
    }

    public static String exportFileName(String extension) {
        return "export-" + FORMATTER.format(LocalDateTime.now()) + leftPad(SEQ.getAndIncrement() % 1000, 3) + "." + extension;
    }

    private static String leftPad(long value, int length) {
        return String.format("%0" + length + "d", value);
    }
}
