package com.plomteux.ncconnector.scheduler;

import com.plomteux.ncconnector.controller.NCControllerApiImpl;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DailyRequestScheduler {
    private NCControllerApiImpl ncControllerApi;

    @Scheduled(cron = "0 0 0 * * ?")
    public void triggerDailyCruiseDetailsRequest() {
        ncControllerApi.getCruiseDetails();
    }

}
