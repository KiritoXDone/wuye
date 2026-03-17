package com.wuye.payment.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class PaymentSignUtils {

    private PaymentSignUtils() {
    }

    public static String sign(String payOrderNo,
                              String outTradeNo,
                              String merchantId,
                              BigDecimal totalAmount,
                              String secret) {
        String payload = String.join("|",
                safe(payOrderNo),
                safe(outTradeNo),
                safe(merchantId),
                totalAmount == null ? "" : totalAmount.toPlainString(),
                safe(secret));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
