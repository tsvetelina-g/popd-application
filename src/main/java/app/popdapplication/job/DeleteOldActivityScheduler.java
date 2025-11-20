package app.popdapplication.job;

import app.popdapplication.service.ActivityService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DeleteOldActivityScheduler {

    private final ActivityService activityService;

    public DeleteOldActivityScheduler(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Sofia")
    @Transactional
    public void cleanupOldActivities() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        long deletedCount = activityService.deleteByCreatedOnBefore(sixMonthsAgo);
        log.info("Cleaned up {} old activities", deletedCount);
    }
}
