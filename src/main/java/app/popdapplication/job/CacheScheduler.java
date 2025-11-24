package app.popdapplication.job;

import app.popdapplication.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheScheduler {

    private final ActivityService activityService;

    public CacheScheduler(ActivityService activityService) {
        this.activityService = activityService;
    }

    @Scheduled(fixedRate = 12, timeUnit = TimeUnit.HOURS)
    public void refreshTopMoviesCache() {
        activityService.evictTopMoviesCache();
        activityService.getTopMovieIds();
        log.info("Top movies cache refreshed successfully");
    }
}
