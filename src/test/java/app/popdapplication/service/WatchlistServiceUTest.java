package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.repository.WatchlistRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatchlistServiceUTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private WatchlistMovieService watchlistMovieService;

    @InjectMocks
    private WatchlistService watchlistService;

    @Test
    void whenCreateDefaultWatchlist_andUserIsValid_thenBuildWatchlistWithDefaultValuesAndSave() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("Username").build();

        watchlistService.createDefaultWatchlist(user);

        ArgumentCaptor<Watchlist> watchlistCaptor = ArgumentCaptor.forClass(Watchlist.class);
        verify(watchlistRepository).save(watchlistCaptor.capture());

        Watchlist savedWatchlist = watchlistCaptor.getValue();
        assertEquals(user, savedWatchlist.getUser());
        assertEquals("Default", savedWatchlist.getName());
        assertEquals(WatchlistType.DEFAULT, savedWatchlist.getWatchlistType());
        assertNotNull(savedWatchlist.getCreatedOn());
        assertNotNull(savedWatchlist.getUpdatedOn());
    }

    @Test
    void whenCreateDefaultWatchlist_andSaveSucceeds_thenTimestampsAreSetToCurrentTime() {
        User user = User.builder().id(UUID.randomUUID()).build();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        watchlistService.createDefaultWatchlist(user);

        ArgumentCaptor<Watchlist> watchlistCaptor = ArgumentCaptor.forClass(Watchlist.class);
        verify(watchlistRepository).save(watchlistCaptor.capture());

        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);
        Watchlist savedWatchlist = watchlistCaptor.getValue();

        assertTrue(savedWatchlist.getCreatedOn().isAfter(beforeCall) && savedWatchlist.getCreatedOn().isBefore(afterCall));
        assertTrue(savedWatchlist.getUpdatedOn().isAfter(beforeCall) && savedWatchlist.getUpdatedOn().isBefore(afterCall));
    }

    @Test
    void whenMovieIsInWatchlist_andMovieExistsInUserWatchlist_thenReturnTrue() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        WatchlistMovie watchlistMovie = WatchlistMovie.builder().watchlist(watchlist).movie(movie).build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));
        when(watchlistMovieService.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.of(watchlistMovie));

        boolean result = watchlistService.movieIsInWatchlist(movie, user);

        assertTrue(result);
    }

    @Test
    void whenMovieIsInWatchlist_andMovieNotInUserWatchlist_thenReturnFalse() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));
        when(watchlistMovieService.findByWatchlistAndMovie(watchlist, movie))
                .thenReturn(Optional.empty());

        boolean result = watchlistService.movieIsInWatchlist(movie, user);

        assertFalse(result);
    }

    @Test
    void whenAddToWatchlist_andMovieAndUserAreValid_thenSaveToWatchlistAndUpdateTimestamp() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(user)
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));

        watchlistService.addToWatchlist(movie, user);

        verify(watchlistMovieService).saveToWatchlist(watchlist, movie);
        verify(watchlistRepository).save(watchlist);
        assertNotNull(watchlist.getUpdatedOn());
    }

    @Test
    void whenAddToWatchlist_andSaveSucceeds_thenUpdatedOnIsSetToCurrentTime() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(user)
                .updatedOn(LocalDateTime.now().minusDays(10))
                .build();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));

        watchlistService.addToWatchlist(movie, user);

        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        assertTrue(watchlist.getUpdatedOn().isAfter(beforeCall) && watchlist.getUpdatedOn().isBefore(afterCall));
    }

    @Test
    void whenRemoveFromWatchlist_andMovieExistsInWatchlist_thenRemoveAndUpdateTimestamp() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(user)
                .updatedOn(LocalDateTime.now().minusDays(1))
                .build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));

        watchlistService.removeFromWatchlist(movie, user);

        verify(watchlistMovieService).removeFromWatchlist(watchlist, movie);
        verify(watchlistRepository).save(watchlist);
        assertNotNull(watchlist.getUpdatedOn());
    }

    @Test
    void whenRemoveFromWatchlist_andRemoveSucceeds_thenUpdatedOnIsSetToCurrentTime() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Movie movie = Movie.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(user)
                .updatedOn(LocalDateTime.now().minusDays(10))
                .build();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));

        watchlistService.removeFromWatchlist(movie, user);

        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        assertTrue(watchlist.getUpdatedOn().isAfter(beforeCall) && watchlist.getUpdatedOn().isBefore(afterCall));
    }

    @Test
    void whenCountMoviesInWatchlist_andWatchlistHasMultipleMovies_thenReturnCorrectCount() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();
        List<WatchlistMovie> watchlistMovies = List.of(
                WatchlistMovie.builder().watchlist(watchlist).build(),
                WatchlistMovie.builder().watchlist(watchlist).build(),
                WatchlistMovie.builder().watchlist(watchlist).build(),
                WatchlistMovie.builder().watchlist(watchlist).build()
        );

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));
        when(watchlistMovieService.findAllByWatchlist(watchlist)).thenReturn(watchlistMovies);

        int result = watchlistService.countMoviesInWatchlist(user);

        assertEquals(4, result);
    }

    @Test
    void whenCountMoviesInWatchlist_andWatchlistIsEmpty_thenReturnZero() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder().id(UUID.randomUUID()).user(user).build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));
        when(watchlistMovieService.findAllByWatchlist(watchlist)).thenReturn(Collections.emptyList());

        int result = watchlistService.countMoviesInWatchlist(user);

        assertEquals(0, result);
    }

    @Test
    void whenFindByUser_andUserHasWatchlist_thenReturnWatchlist() {
        User user = User.builder().id(UUID.randomUUID()).build();
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Default")
                .watchlistType(WatchlistType.DEFAULT)
                .build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.of(watchlist));

        Watchlist result = watchlistService.findByUser(user);

        assertNotNull(result);
        assertEquals(watchlist, result);
        assertEquals(user, result.getUser());
    }

    @Test
    void whenFindByUser_andUserHasNoWatchlist_thenThrowNotFoundException() {
        User user = User.builder().id(UUID.randomUUID()).build();

        when(watchlistRepository.findByUser(user)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            watchlistService.findByUser(user);
        });

        assertTrue(exception.getMessage().contains("Watchlist for user with id"));
        assertTrue(exception.getMessage().contains(user.getId().toString()));
    }
}
