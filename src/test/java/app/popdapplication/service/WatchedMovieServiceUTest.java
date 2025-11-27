package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import app.popdapplication.model.enums.ActivityType;
import app.popdapplication.repository.WatchedMovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatchedMovieServiceUTest {

    @Mock
    private WatchedMovieRepository watchedMovieRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WatchedMovieService watchedMovieService;

    @Test
    void whenCountWatchedMovies_andUserHasWatchedMultipleMovies_thenReturnCorrectCount() {
        User user = User.builder().id(UUID.randomUUID()).username("User").build();
        List<WatchedMovie> watchedMovies = List.of(
                WatchedMovie.builder().user(user).build(),
                WatchedMovie.builder().user(user).build(),
                WatchedMovie.builder().user(user).build()
        );

        when(watchedMovieRepository.findAllByUser(user)).thenReturn(watchedMovies);

        int result = watchedMovieService.countWatchedMovies(user);

        assertEquals(3, result);
    }

    @Test
    void whenCountWatchedMovies_andUserHasNotWatchedAnyMovies_thenReturnZero() {
        User user = User.builder().id(UUID.randomUUID()).username("User").build();

        when(watchedMovieRepository.findAllByUser(user)).thenReturn(Collections.emptyList());

        int result = watchedMovieService.countWatchedMovies(user);

        assertEquals(0, result);
    }

    @Test
    void whenMovieIsWatched_andUserHasWatchedTheMovie_thenReturnTrue() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        WatchedMovie watchedMovie = WatchedMovie.builder().user(user).movie(movie).build();

        when(watchedMovieRepository.findByUserAndMovie(user, movie)).thenReturn(Optional.of(watchedMovie));

        boolean result = watchedMovieService.movieIsWatched(movie, user);

        assertTrue(result);
    }

    @Test
    void whenMovieIsWatched_andUserHasNotWatchedTheMovie_thenReturnFalse() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();

        when(watchedMovieRepository.findByUserAndMovie(user, movie)).thenReturn(Optional.empty());

        boolean result = watchedMovieService.movieIsWatched(movie, user);

        assertFalse(result);
    }

    @Test
    void whenAddToWatched_andMovieAndUserAreValid_thenSaveWatchedMovieAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        User user = User.builder().id(userId).username("Username").build();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        watchedMovieService.addToWatched(movie, user);

        ArgumentCaptor<WatchedMovie> watchedCaptor = ArgumentCaptor.forClass(WatchedMovie.class);
        verify(watchedMovieRepository).save(watchedCaptor.capture());

        WatchedMovie savedWatched = watchedCaptor.getValue();
        assertEquals(movie, savedWatched.getMovie());
        assertEquals(user, savedWatched.getUser());
        assertNotNull(savedWatched.getCreatedOn());

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.WATCHED, capturedEvent.getType());
        assertFalse(capturedEvent.isRemoved());
        assertNull(capturedEvent.getRating());
    }

    @Test
    void whenAddToWatched_andSaveSucceeds_thenCreatedOnTimestampIsSetToCurrentTime() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).build();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        watchedMovieService.addToWatched(movie, user);

        ArgumentCaptor<WatchedMovie> watchedCaptor = ArgumentCaptor.forClass(WatchedMovie.class);
        verify(watchedMovieRepository).save(watchedCaptor.capture());

        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);
        LocalDateTime createdOn = watchedCaptor.getValue().getCreatedOn();

        assertTrue(createdOn.isAfter(beforeCall) && createdOn.isBefore(afterCall));
    }

    @Test
    void whenRemoveFromWatched_andMovieExistsInWatchedList_thenDeleteAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        WatchedMovie watchedMovie = WatchedMovie.builder()
                .id(UUID.randomUUID())
                .user(user)
                .movie(movie)
                .build();

        when(watchedMovieRepository.findByUserAndMovie(user, movie)).thenReturn(Optional.of(watchedMovie));

        watchedMovieService.removeFromWatched(movie, user);

        verify(watchedMovieRepository).delete(watchedMovie);

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.WATCHED, capturedEvent.getType());
        assertTrue(capturedEvent.isRemoved());
    }

    @Test
    void whenRemoveFromWatched_andMovieNotInWatchedList_thenThrowsExceptionAndDoNotPublishEvent() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();

        when(watchedMovieRepository.findByUserAndMovie(user, movie)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> watchedMovieService.removeFromWatched(movie, user)
        );

        assertEquals("Movie not found in watched list", exception.getMessage());
        verify(watchedMovieRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenUsersWatchedCount_andMultipleUsersHaveWatchedTheMovie_thenReturnCorrectCount() {
        UUID movieId = UUID.randomUUID();
        List<WatchedMovie> watchedMovies = List.of(
                WatchedMovie.builder().build(),
                WatchedMovie.builder().build(),
                WatchedMovie.builder().build(),
                WatchedMovie.builder().build(),
                WatchedMovie.builder().build()
        );

        when(watchedMovieRepository.findAllByMovieId(movieId)).thenReturn(watchedMovies);

        int result = watchedMovieService.usersWatchedCount(movieId);

        assertEquals(5, result);
    }

    @Test
    void whenUsersWatchedCount_andNoUsersHaveWatchedTheMovie_thenReturnZero() {
        UUID movieId = UUID.randomUUID();

        when(watchedMovieRepository.findAllByMovieId(movieId)).thenReturn(Collections.emptyList());

        int result = watchedMovieService.usersWatchedCount(movieId);

        assertEquals(0, result);
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andUserHasWatchedMovies_thenReturnPageOfWatchedMovies() {
        User user = User.builder().id(UUID.randomUUID()).build();
        List<WatchedMovie> watchedMovies = List.of(
                WatchedMovie.builder().user(user).createdOn(LocalDateTime.now()).build(),
                WatchedMovie.builder().user(user).createdOn(LocalDateTime.now().minusDays(1)).build()
        );
        Page<WatchedMovie> page = new PageImpl<>(watchedMovies);

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), any(PageRequest.class)))
                .thenReturn(page);

        Page<WatchedMovie> result = watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andNegativePageNumber_thenDefaultToPageZero() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Page<WatchedMovie> emptyPage = new PageImpl<>(List.of());

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, -5, 10);

        verify(watchedMovieRepository).findAllByUserOrderByCreatedOnDesc(user, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andSizeIsZero_thenDefaultToSizeTen() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Page<WatchedMovie> emptyPage = new PageImpl<>(List.of());

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, 0, 0);

        verify(watchedMovieRepository).findAllByUserOrderByCreatedOnDesc(user, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andSizeGreaterThan50_thenDefaultToSizeTen() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Page<WatchedMovie> emptyPage = new PageImpl<>(List.of());

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, 0, 100);

        verify(watchedMovieRepository).findAllByUserOrderByCreatedOnDesc(user, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andUserHasNoWatchedMovies_thenReturnEmptyPage() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Page<WatchedMovie> emptyPage = new PageImpl<>(Collections.emptyList());

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<WatchedMovie> result = watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, 0, 10);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void whenFindAllByUserOrderByCreatedOnDesc_andValidPageAndSize_thenUseProvidedValues() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Page<WatchedMovie> emptyPage = new PageImpl<>(List.of());

        when(watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(eq(user), eq(PageRequest.of(2, 25))))
                .thenReturn(emptyPage);

        watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, 2, 25);

        verify(watchedMovieRepository).findAllByUserOrderByCreatedOnDesc(user, PageRequest.of(2, 25));
    }
}
