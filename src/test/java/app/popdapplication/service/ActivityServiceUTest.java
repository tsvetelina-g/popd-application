package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.model.entity.Activity;
import app.popdapplication.model.enums.ActivityType;
import app.popdapplication.repository.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceUTest {

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void whenSaveActivity_withValidEvent_thenSaveActivityToRepository() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        LocalDateTime createdOn = LocalDateTime.now();
        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.WATCHED)
                .removed(false)
                .rating(null)
                .createdOn(createdOn)
                .build();

        activityService.saveActivity(event);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());

        Activity savedActivity = activityCaptor.getValue();
        assertEquals(userId, savedActivity.getUserId());
        assertEquals(movieId, savedActivity.getMovieId());
        assertEquals(ActivityType.WATCHED, savedActivity.getType());
        assertFalse(savedActivity.isRemoved());
        assertNull(savedActivity.getRating());
        assertEquals(createdOn, savedActivity.getCreatedOn());
    }

    @Test
    void whenSaveActivity_withRatedEvent_thenSaveWithRating() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.RATED)
                .removed(false)
                .rating(8)
                .createdOn(LocalDateTime.now())
                .build();

        activityService.saveActivity(event);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());

        Activity savedActivity = activityCaptor.getValue();
        assertEquals(ActivityType.RATED, savedActivity.getType());
        assertEquals(8, savedActivity.getRating());
    }

    @Test
    void whenSaveActivity_withRemovedEvent_thenSaveWithRemovedTrue() {
        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(UUID.randomUUID())
                .movieId(UUID.randomUUID())
                .type(ActivityType.ADDED_TO_WATCHLIST)
                .removed(true)
                .createdOn(LocalDateTime.now())
                .build();

        activityService.saveActivity(event);

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());

        assertTrue(activityCaptor.getValue().isRemoved());
    }

    @Test
    void whenReturnLatestFiveActivities_andActivitiesExist_thenReturnUpToFiveActivities() {
        UUID userId = UUID.randomUUID();
        List<Activity> activities = List.of(
                Activity.builder().userId(userId).type(ActivityType.WATCHED).build(),
                Activity.builder().userId(userId).type(ActivityType.RATED).build(),
                Activity.builder().userId(userId).type(ActivityType.REVIEWED).build()
        );
        when(activityRepository.findAllByUserIdOrderByCreatedOnDesc(userId)).thenReturn(activities);

        List<Activity> result = activityService.returnLatestFiveActivities(userId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void whenReturnLatestFiveActivities_andMoreThanFiveExist_thenReturnOnlyFive() {
        UUID userId = UUID.randomUUID();
        List<Activity> activities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            activities.add(Activity.builder().userId(userId).type(ActivityType.WATCHED).build());
        }
        when(activityRepository.findAllByUserIdOrderByCreatedOnDesc(userId)).thenReturn(activities);

        List<Activity> result = activityService.returnLatestFiveActivities(userId);

        assertNotNull(result);
        assertEquals(5, result.size());
    }

    @Test
    void whenReturnLatestFiveActivities_andNoActivitiesExist_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        when(activityRepository.findAllByUserIdOrderByCreatedOnDesc(userId)).thenReturn(Collections.emptyList());

        List<Activity> result = activityService.returnLatestFiveActivities(userId);

        assertNull(result);
    }

    @Test
    void whenDeleteByCreatedOnBefore_andActivitiesExist_thenReturnDeletedCount() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        when(activityRepository.deleteAllByCreatedOnBefore(oneYearAgo)).thenReturn(5L);

        long result = activityService.deleteByCreatedOnBefore(oneYearAgo);

        assertEquals(5L, result);
        verify(activityRepository).deleteAllByCreatedOnBefore(oneYearAgo);
    }

    @Test
    void whenDeleteByCreatedOnBefore_andNoActivitiesToDelete_thenReturnZero() {
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        when(activityRepository.deleteAllByCreatedOnBefore(oneYearAgo)).thenReturn(0L);

        long result = activityService.deleteByCreatedOnBefore(oneYearAgo);

        assertEquals(0L, result);
    }

    @Test
    void whenGetTopMovieIds_andMoviesExist_thenReturnListOfMovieIds() {
        List<UUID> topMovieIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(activityRepository.findTopMovieIds(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(topMovieIds);

        List<UUID> result = activityService.getTopMovieIds();

        assertNotNull(result);
        assertEquals(3, result.size());
        verify(activityRepository).findTopMovieIds(any(LocalDateTime.class), eq(PageRequest.of(0, 10)));
    }

    @Test
    void whenGetTopMovieIds_andThereAreNoMovies_thenReturnEmptyList() {
        when(activityRepository.findTopMovieIds(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        List<UUID> result = activityService.getTopMovieIds();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetMovieIdsFromActivities_andActivitiesExist_thenReturnSetOfMovieIds() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        UUID movieId3 = UUID.randomUUID();
        List<Activity> activities = List.of(
                Activity.builder().movieId(movieId1).build(),
                Activity.builder().movieId(movieId2).build(),
                Activity.builder().movieId(movieId3).build()
        );

        Set<UUID> result = activityService.getMovieIdsFromActivities(activities);

        assertEquals(3, result.size());
        assertTrue(result.contains(movieId1));
        assertTrue(result.contains(movieId2));
        assertTrue(result.contains(movieId3));
    }

    @Test
    void whenGetMovieIdsFromActivities_withDuplicateMovieIds_thenReturnUniqueSet() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        List<Activity> activities = List.of(
                Activity.builder().movieId(movieId1).build(),
                Activity.builder().movieId(movieId1).build(),  // duplicate
                Activity.builder().movieId(movieId2).build()
        );

        Set<UUID> result = activityService.getMovieIdsFromActivities(activities);

        assertEquals(2, result.size());  // duplicates removed
    }

    @Test
    void whenGetMovieIdsFromActivities_withNullList_thenReturnEmptySet() {
        Set<UUID> result = activityService.getMovieIdsFromActivities(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetMovieIdsFromActivities_withEmptyList_thenReturnEmptySet() {
        Set<UUID> result = activityService.getMovieIdsFromActivities(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
