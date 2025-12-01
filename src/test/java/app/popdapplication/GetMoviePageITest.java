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
        Genre genre = Genre.builder()
                .name("Action")
                .build();
        genreRepository.save(genre);

        Movie movie = Movie.builder()
                .title("Title")
                .description("Description")
                .releaseDate(LocalDate.of(2024, 1, 15))
                .posterUrl("www.picture.com")
                .genres(List.of(genre))
                .build();
        movieRepository.save(movie);

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

        Movie foundMovie = movieService.findById(movie.getId());
        assertNotNull(foundMovie);
        assertEquals("Title", foundMovie.getTitle());

        List<MovieCredit> credits = movieCreditService.getCreditsByMovie(foundMovie);
        assertEquals(2, credits.size());

        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(foundMovie);
        assertTrue(creditsByRole.containsKey(ArtistRole.ACTOR));
        assertTrue(creditsByRole.containsKey(ArtistRole.DIRECTOR));

        assertFalse(watchedMovieService.movieIsWatched(foundMovie, user));
        assertFalse(watchlistService.movieIsInWatchlist(foundMovie, user));
        assertEquals(0, watchedMovieService.usersWatchedCount(foundMovie.getId()));

        watchedMovieService.addToWatched(foundMovie, user);
        assertTrue(watchedMovieService.movieIsWatched(foundMovie, user));
        assertEquals(1, watchedMovieService.usersWatchedCount(foundMovie.getId()));

        watchlistService.addToWatchlist(foundMovie, user);
        assertTrue(watchlistService.movieIsInWatchlist(foundMovie, user));
        assertEquals(1, watchlistService.countMoviesInWatchlist(user));

        watchedMovieService.removeFromWatched(foundMovie, user);
        assertFalse(watchedMovieService.movieIsWatched(foundMovie, user));

        watchlistService.removeFromWatchlist(foundMovie, user);
        assertFalse(watchlistService.movieIsInWatchlist(foundMovie, user));
    }

    @Test
    void getMoviePage_withAnonymousUser_shouldLoadMovieDataWithoutUserInteractions() {
        Genre genre = Genre.builder()
                .name("Drama")
                .build();
        genreRepository.save(genre);

        Movie movie = Movie.builder()
                .title("Title 1")
                .description("Description 1")
                .releaseDate(LocalDate.of(2024, 6, 20))
                .posterUrl("www.picture.com")
                .genres(List.of(genre))
                .build();
        movieRepository.save(movie);

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

        Movie foundMovie = movieService.findById(movie.getId());
        assertNotNull(foundMovie);
        assertEquals("Title 1", foundMovie.getTitle());
        assertEquals("Description 1", foundMovie.getDescription());

        List<MovieCredit> credits = movieCreditService.getCreditsByMovie(foundMovie);
        assertEquals(1, credits.size());
        assertEquals("Artist", credits.get(0).getArtist().getName());

        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(foundMovie);
        assertTrue(creditsByRole.containsKey(ArtistRole.ACTOR));

        int usersWatchedCount = watchedMovieService.usersWatchedCount(foundMovie.getId());
        assertEquals(0, usersWatchedCount);
    }
}
