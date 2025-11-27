package app.popdapplication;

import app.popdapplication.model.entity.*;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.repository.*;
import app.popdapplication.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class GetMoviePageITest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private WatchedMovieService watchedMovieService;

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private MovieCreditService movieCreditService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private MovieCreditRepository movieCreditRepository;

    @Test
    void getMoviePage_withAuthenticatedUser_shouldLoadAllMovieDataAndUserInteractions() {
        // Create genre
        Genre genre = Genre.builder()
                .name("Action")
                .build();
        genreRepository.save(genre);

        // Create movie
        Movie movie = Movie.builder()
                .title("Title")
                .description("Description")
                .releaseDate(LocalDate.of(2024, 1, 15))
                .posterUrl("www.picture.com")
                .genres(List.of(genre))
                .build();
        movieRepository.save(movie);

        // Create user
        User user = User.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        userRepository.save(user);
        watchlistService.createDefaultWatchlist(user);

        // Create actors and credits
        Artist actor = Artist.builder()
                .name("Artist 1")
                .birthDate(LocalDate.of(1980, 5, 15))
                .build();
        artistRepository.save(actor);

        Artist director = Artist.builder()
                .name("Artist 2")
                .birthDate(LocalDate.of(1970, 3, 20))
                .build();
        artistRepository.save(director);

        MovieCredit actorCredit = MovieCredit.builder()
                .movie(movie)
                .artist(actor)
                .roleType(ArtistRole.ACTOR)
                .build();
        movieCreditRepository.save(actorCredit);

        MovieCredit directorCredit = MovieCredit.builder()
                .movie(movie)
                .artist(director)
                .roleType(ArtistRole.DIRECTOR)
                .build();
        movieCreditRepository.save(directorCredit);

        // Test movie page data loading
        Movie foundMovie = movieService.findById(movie.getId());
        assertNotNull(foundMovie);
        assertEquals("Title", foundMovie.getTitle());

        // Test credits
        List<MovieCredit> credits = movieCreditService.getCreditsByMovie(foundMovie);
        assertEquals(2, credits.size());

        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(foundMovie);
        assertTrue(creditsByRole.containsKey(ArtistRole.ACTOR));
        assertTrue(creditsByRole.containsKey(ArtistRole.DIRECTOR));

        // Test user interactions - initially not watched and not in watchlist
        assertFalse(watchedMovieService.movieIsWatched(foundMovie, user));
        assertFalse(watchlistService.movieIsInWatchlist(foundMovie, user));
        assertEquals(0, watchedMovieService.usersWatchedCount(foundMovie.getId()));

        // Add to watched
        watchedMovieService.addToWatched(foundMovie, user);
        assertTrue(watchedMovieService.movieIsWatched(foundMovie, user));
        assertEquals(1, watchedMovieService.usersWatchedCount(foundMovie.getId()));

        // Add to watchlist
        watchlistService.addToWatchlist(foundMovie, user);
        assertTrue(watchlistService.movieIsInWatchlist(foundMovie, user));
        assertEquals(1, watchlistService.countMoviesInWatchlist(user));

        // Remove from watched
        watchedMovieService.removeFromWatched(foundMovie, user);
        assertFalse(watchedMovieService.movieIsWatched(foundMovie, user));

        // Remove from watchlist
        watchlistService.removeFromWatchlist(foundMovie, user);
        assertFalse(watchlistService.movieIsInWatchlist(foundMovie, user));
    }

    @Test
    void getMoviePage_withAnonymousUser_shouldLoadMovieDataWithoutUserInteractions() {
        // Create genre
        Genre genre = Genre.builder()
                .name("Drama")
                .build();
        genreRepository.save(genre);

        // Create movie
        Movie movie = Movie.builder()
                .title("Title 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2024, 6, 20))
                .posterUrl("www.picture.com")
                .genres(List.of(genre))
                .build();
        movieRepository.save(movie);

        // Create actor and credit
        Artist actor = Artist.builder()
                .name("Artist")
                .birthDate(LocalDate.of(1985, 8, 10))
                .build();
        artistRepository.save(actor);

        MovieCredit credit = MovieCredit.builder()
                .movie(movie)
                .artist(actor)
                .roleType(ArtistRole.ACTOR)
                .build();
        movieCreditRepository.save(credit);

        // Anonymous user can still load movie data
        Movie foundMovie = movieService.findById(movie.getId());
        assertNotNull(foundMovie);
        assertEquals("Title 1", foundMovie.getTitle());
        assertEquals("Description 1", foundMovie.getDescription());

        // Anonymous user can still see credits
        List<MovieCredit> credits = movieCreditService.getCreditsByMovie(foundMovie);
        assertEquals(1, credits.size());
        assertEquals("Artist", credits.get(0).getArtist().getName());

        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(foundMovie);
        assertTrue(creditsByRole.containsKey(ArtistRole.ACTOR));

        // Anonymous user can see how many users watched the movie
        int usersWatchedCount = watchedMovieService.usersWatchedCount(foundMovie.getId());
        assertEquals(0, usersWatchedCount);
    }
}
