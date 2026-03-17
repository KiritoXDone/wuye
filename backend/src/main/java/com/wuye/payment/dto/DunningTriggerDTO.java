package com.wuye.payment.dto;

import java.time.LocalDate;

public class DunningTriggerDTO {

    private LocalDate triggerDate;

    public LocalDate getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(LocalDate triggerDate) {
        this.triggerDate = triggerDate;
    }
}
