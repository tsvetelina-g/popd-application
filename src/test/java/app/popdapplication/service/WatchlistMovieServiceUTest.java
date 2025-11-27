package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.AlreadyExistsException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.model.enums.ActivityType;
import app.popdapplication.repository.WatchlistMovieRepository;
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
public class WatchlistMovieServiceUTest {

    @Mock
    private WatchlistMovieRepository watchlistMovieRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WatchlistMovieService watchlistMovieService;

    @Test
    void whenFindByWatchlistAndMovie_andMovieExistsInWatchlist_thenReturnOptionalWithWatchlistMovie() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        WatchlistMovie watchlistMovie = WatchlistMovie.builder()
                .watchlist(watchlist)
                .movie(movie)
                .build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.of(watchlistMovie));

        Optional<WatchlistMovie> result = watchlistMovieService.findByWatchlistAndMovie(watchlist, movie);

        assertTrue(result.isPresent());
        assertEquals(watchlistMovie, result.get());
    }

    @Test
    void whenFindByWatchlistAndMovie_andMovieNotInWatchlist_thenReturnEmptyOptional() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.empty());

        Optional<WatchlistMovie> result = watchlistMovieService.findByWatchlistAndMovie(watchlist, movie);

        assertTrue(result.isEmpty());
    }

    @Test
    void whenSaveToWatchlist_andMovieNotAlreadyInWatchlist_thenSaveAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        User user = User.builder().id(userId).username("Username").build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.empty());

        watchlistMovieService.saveToWatchlist(watchlist, movie);

        ArgumentCaptor<WatchlistMovie> watchlistMovieCaptor = ArgumentCaptor.forClass(WatchlistMovie.class);
        verify(watchlistMovieRepository).save(watchlistMovieCaptor.capture());

        WatchlistMovie savedWatchlistMovie = watchlistMovieCaptor.getValue();
        assertEquals(movie, savedWatchlistMovie.getMovie());
        assertEquals(watchlist, savedWatchlistMovie.getWatchlist());
        assertNotNull(savedWatchlistMovie.getAddedOn());

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.ADDED_TO_WATCHLIST, capturedEvent.getType());
        assertFalse(capturedEvent.isRemoved());
        assertNull(capturedEvent.getRating());
    }

    @Test
    void whenSaveToWatchlist_andMovieAlreadyInWatchlist_thenThrowsExceptionAndDoNotSave() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        WatchlistMovie existingWatchlistMovie = WatchlistMovie.builder()
                .watchlist(watchlist)
                .movie(movie)
                .build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.of(existingWatchlistMovie));

        AlreadyExistsException exception = assertThrows(
                AlreadyExistsException.class,
                () -> watchlistMovieService.saveToWatchlist(watchlist, movie)
        );

        assertEquals("Movie already in watchlist", exception.getMessage());
        verify(watchlistMovieRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenSaveToWatchlist_andSaveSucceeds_thenAddedOnTimestampIsSetToCurrentTime() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).build();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.empty());

        watchlistMovieService.saveToWatchlist(watchlist, movie);

        ArgumentCaptor<WatchlistMovie> watchlistMovieCaptor = ArgumentCaptor.forClass(WatchlistMovie.class);
        verify(watchlistMovieRepository).save(watchlistMovieCaptor.capture());

        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);
        LocalDateTime addedOn = watchlistMovieCaptor.getValue().getAddedOn();

        assertTrue(addedOn.isAfter(beforeCall) && addedOn.isBefore(afterCall));
    }

    @Test
    void whenRemoveFromWatchlist_andMovieExistsInWatchlist_thenDeleteAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        User user = User.builder().id(userId).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        WatchlistMovie watchlistMovie = WatchlistMovie.builder()
                .id(UUID.randomUUID())
                .watchlist(watchlist)
                .movie(movie)
                .build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.of(watchlistMovie));

        watchlistMovieService.removeFromWatchlist(watchlist, movie);

        verify(watchlistMovieRepository).delete(watchlistMovie);

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.ADDED_TO_WATCHLIST, capturedEvent.getType());
        assertTrue(capturedEvent.isRemoved());
    }

    @Test
    void whenRemoveFromWatchlist_andMovieNotInWatchlist_thenThrowsExceptionAndDoNotPublishEvent() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();

        when(watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> watchlistMovieService.removeFromWatchlist(watchlist, movie)
        );

        assertEquals("Movie not found in watchlist", exception.getMessage());
        verify(watchlistMovieRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenFindAllByWatchlist_andWatchlistHasMultipleMovies_thenReturnListOfWatchlistMovies() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        List<WatchlistMovie> watchlistMovies = List.of(
                WatchlistMovie.builder().watchlist(watchlist).build(),
                WatchlistMovie.builder().watchlist(watchlist).build(),
                WatchlistMovie.builder().watchlist(watchlist).build()
        );

        when(watchlistMovieRepository.findAllByWatchlist(watchlist)).thenReturn(watchlistMovies);

        List<WatchlistMovie> result = watchlistMovieService.findAllByWatchlist(watchlist);

        assertEquals(3, result.size());
    }

    @Test
    void whenFindAllByWatchlist_andWatchlistIsEmpty_thenReturnEmptyList() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();

        when(watchlistMovieRepository.findAllByWatchlist(watchlist)).thenReturn(Collections.emptyList());

        List<WatchlistMovie> result = watchlistMovieService.findAllByWatchlist(watchlist);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andWatchlistHasMovies_thenReturnPageOfWatchlistMovies() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        List<WatchlistMovie> watchlistMovies = List.of(
                WatchlistMovie.builder().watchlist(watchlist).addedOn(LocalDateTime.now()).build(),
                WatchlistMovie.builder().watchlist(watchlist).addedOn(LocalDateTime.now().minusDays(1)).build()
        );
        Page<WatchlistMovie> page = new PageImpl<>(watchlistMovies);

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), any(PageRequest.class)))
                .thenReturn(page);

        Page<WatchlistMovie> result = watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andNegativePageNumber_thenDefaultToPageZero() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(List.of());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, -5, 10);

        verify(watchlistMovieRepository).findAllByWatchlistOrderByAddedOnDesc(watchlist, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andSizeIsZero_thenDefaultToSizeTen() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(List.of());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 0, 0);

        verify(watchlistMovieRepository).findAllByWatchlistOrderByAddedOnDesc(watchlist, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andSizeGreaterThan50_thenDefaultToSizeTen() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(List.of());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 0, 100);

        verify(watchlistMovieRepository).findAllByWatchlistOrderByAddedOnDesc(watchlist, PageRequest.of(0, 10));
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andWatchlistIsEmpty_thenReturnEmptyPage() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(Collections.emptyList());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), any(PageRequest.class)))
                .thenReturn(emptyPage);

        Page<WatchlistMovie> result = watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 0, 10);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andValidPageAndSize_thenUseProvidedValues() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(List.of());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), eq(PageRequest.of(3, 20))))
                .thenReturn(emptyPage);

        watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 3, 20);

        verify(watchlistMovieRepository).findAllByWatchlistOrderByAddedOnDesc(watchlist, PageRequest.of(3, 20));
    }

    @Test
    void whenFindAllByWatchlistOrderByAddedOnDesc_andSizeIsNegative_thenDefaultToSizeTen() {
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).build();
        Page<WatchlistMovie> emptyPage = new PageImpl<>(List.of());

        when(watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), eq(PageRequest.of(0, 10))))
                .thenReturn(emptyPage);

        watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, 0, -5);

        verify(watchlistMovieRepository).findAllByWatchlistOrderByAddedOnDesc(watchlist, PageRequest.of(0, 10));
    }
}
