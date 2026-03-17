package com.wuye.payment.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DunningScheduler {

    private final DunningService dunningService;

    public DunningScheduler(DunningService dunningService) {
        this.dunningService = dunningService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void runDaily() {
        dunningService.triggerScheduled(LocalDate.now());
    }
}
