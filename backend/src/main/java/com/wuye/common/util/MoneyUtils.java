package com.wuye.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scaleQuantity(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }
}
