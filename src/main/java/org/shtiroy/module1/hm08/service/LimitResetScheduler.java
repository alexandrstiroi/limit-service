package org.shtiroy.module1.hm08.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LimitResetScheduler {
    private static final Logger log = LoggerFactory.getLogger(LimitResetScheduler.class);

    private final LimitService limitService;

    public LimitResetScheduler(LimitService limitService) {
        this.limitService = limitService;
    }

    @Scheduled(cron = "${app.limit.reset-cron:0 0 0 * * *}")
    public void resetAllUsers() {
        int updatedUsers = limitService.resetAllUsersToDefault();
        log.info("Сброс дневного лимита завершен, обновлены {}", updatedUsers);
    }
}
