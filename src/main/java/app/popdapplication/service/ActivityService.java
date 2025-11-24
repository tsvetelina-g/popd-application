package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.model.entity.Activity;
import app.popdapplication.repository.ActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @EventListener
    public void saveActivity(ActivityDtoEvent activityDtoEvent) {

        Activity activity = Activity.builder()
                .userId(activityDtoEvent.getUserId())
                .movieId(activityDtoEvent.getMovieId())
                .type(activityDtoEvent.getType())
                .removed(activityDtoEvent.isRemoved())
                .createdOn(activityDtoEvent.getCreatedOn())
                .rating(activityDtoEvent.getRating())
                .build();

        activityRepository.save(activity);
    }

    public List<Activity> returnLatestFiveActivities(UUID userId) {

        List<Activity> activities = activityRepository.findAllByUserIdOrderByCreatedOnDesc(userId).stream().limit(5).toList();

        if (activities.isEmpty()) {
            return null;
        }

        return activities;
    }

    public long deleteByCreatedOnBefore(LocalDateTime oneYearAgo) {
        return activityRepository.deleteAllByCreatedOnBefore(oneYearAgo);
    }

    @Cacheable(value = "topMovies", key = "'top10'")
    public List<UUID> getTopMovieIds() {
        LocalDateTime lastDay = LocalDateTime.now().minusDays(1);

        return activityRepository.findTopMovieIds(lastDay, PageRequest.of(0, 10));
    }

    @CacheEvict(value = "topMovies", allEntries = true)
    public void evictTopMoviesCache() {
        log.debug("Evicting top movies cache");
    }

    public Set<UUID> getMovieIdsFromActivities(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Set.of();
        }
        return activities.stream()
                .map(Activity::getMovieId)
                .collect(Collectors.toSet());
    }
}
