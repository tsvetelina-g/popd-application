package app.popdapplication.service;

import app.popdapplication.exception.AlreadyExistsException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.repository.MovieCreditRepository;
import app.popdapplication.web.dto.AddCreditRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieCreditServiceUTest {

    @Mock
    private MovieCreditRepository movieCreditRepository;

    @Mock
    private MovieService movieService;

    @Mock
    private ArtistService artistService;

    @InjectMocks
    private MovieCreditService movieCreditService;

    @Test
    void whenSaveCredit_andCreditDoesNotExist_thenBuildCreditAndSaveToRepository() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Actor").build();
        AddCreditRequest request = AddCreditRequest.builder()
                .artistName("Actor")
                .role("ACTOR")
                .build();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(artistService.findByName("Actor")).thenReturn(artist);
        when(movieCreditRepository.findByMovieAndArtistAndRoleType(movie, artist, ArtistRole.ACTOR))
                .thenReturn(Optional.empty());

        movieCreditService.saveCredit(request, movieId);

        ArgumentCaptor<MovieCredit> creditCaptor = ArgumentCaptor.forClass(MovieCredit.class);
        verify(movieCreditRepository).save(creditCaptor.capture());

        MovieCredit savedCredit = creditCaptor.getValue();
        assertEquals(movie, savedCredit.getMovie());
        assertEquals(artist, savedCredit.getArtist());
        assertEquals(ArtistRole.ACTOR, savedCredit.getRoleType());
    }

    @Test
    void whenSaveCredit_andCreditAlreadyExistsForSameMovieArtistAndRole_thenThrowsException() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Actor").build();
        MovieCredit existingCredit = MovieCredit.builder()
                .movie(movie)
                .artist(artist)
                .roleType(ArtistRole.ACTOR)
                .build();
        AddCreditRequest request = AddCreditRequest.builder()
                .artistName("Actor")
                .role("ACTOR")
                .build();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(artistService.findByName("Actor")).thenReturn(artist);
        when(movieCreditRepository.findByMovieAndArtistAndRoleType(movie, artist, ArtistRole.ACTOR))
                .thenReturn(Optional.of(existingCredit));

        assertThrows(AlreadyExistsException.class, () -> movieCreditService.saveCredit(request, movieId));
        verify(movieCreditRepository, never()).save(any());
    }

    @Test
    void whenSaveCredit_andMovieDoesNotExist_thenThrowsException() {
        UUID movieId = UUID.randomUUID();
        AddCreditRequest request = AddCreditRequest.builder()
                .artistName("Actor")
                .role("ACTOR")
                .build();

        when(movieService.findById(movieId)).thenThrow(new NotFoundException());

        assertThrows(NotFoundException.class, () -> movieCreditService.saveCredit(request, movieId));
        verify(movieCreditRepository, never()).save(any());
    }

    @Test
    void whenSaveCredit_andArtistDoesNotExist_thenThrowNotFoundExceptionFromArtistService() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        AddCreditRequest request = AddCreditRequest.builder()
                .artistName("Actor")
                .role("ACTOR")
                .build();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(artistService.findByName("Actor")).thenThrow(new NotFoundException());

        assertThrows(NotFoundException.class, () -> movieCreditService.saveCredit(request, movieId));
        verify(movieCreditRepository, never()).save(any());
    }

    @Test
    void whenSaveCredit_andRoleIsDirector_thenSaveCreditWithDirectorRole() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Director").build();
        AddCreditRequest request = AddCreditRequest.builder()
                .artistName("Director")
                .role("DIRECTOR")
                .build();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(artistService.findByName("Director")).thenReturn(artist);
        when(movieCreditRepository.findByMovieAndArtistAndRoleType(movie, artist, ArtistRole.DIRECTOR))
                .thenReturn(Optional.empty());

        movieCreditService.saveCredit(request, movieId);

        ArgumentCaptor<MovieCredit> creditCaptor = ArgumentCaptor.forClass(MovieCredit.class);
        verify(movieCreditRepository).save(creditCaptor.capture());
        assertEquals(ArtistRole.DIRECTOR, creditCaptor.getValue().getRoleType());
    }

    @Test
    void whenGetCreditsByMovie_andCreditsExist_thenReturnListOfMovieCredits() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        List<MovieCredit> credits = List.of(
                MovieCredit.builder().movie(movie).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().movie(movie).roleType(ArtistRole.DIRECTOR).build()
        );
        when(movieCreditRepository.findByMovie(movie)).thenReturn(credits);

        List<MovieCredit> result = movieCreditService.getCreditsByMovie(movie);

        assertEquals(2, result.size());
        verify(movieCreditRepository).findByMovie(movie);
    }

    @Test
    void whenGetCreditsByMovie_andNoCreditsExist_thenReturnEmptyList() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        when(movieCreditRepository.findByMovie(movie)).thenReturn(Collections.emptyList());

        List<MovieCredit> result = movieCreditService.getCreditsByMovie(movie);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetCreditsByMovieGroupedByRole_andMultipleRolesExist_thenReturnMapGroupedByArtistRole() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Artist actor1 = Artist.builder().name("Actor 1").build();
        Artist actor2 = Artist.builder().name("Actor 1").build();
        Artist director = Artist.builder().name("Director").build();

        List<MovieCredit> credits = List.of(
                MovieCredit.builder().movie(movie).artist(actor1).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().movie(movie).artist(actor2).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().movie(movie).artist(director).roleType(ArtistRole.DIRECTOR).build()
        );
        when(movieCreditRepository.findByMovie(movie)).thenReturn(credits);

        Map<ArtistRole, List<MovieCredit>> result = movieCreditService.getCreditsByMovieGroupedByRole(movie);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(ArtistRole.ACTOR));
        assertTrue(result.containsKey(ArtistRole.DIRECTOR));
        assertEquals(2, result.get(ArtistRole.ACTOR).size());
        assertEquals(1, result.get(ArtistRole.DIRECTOR).size());
    }

    @Test
    void whenGetCreditsByMovieGroupedByRole_andNoCreditsExist_thenReturnEmptyMap() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        when(movieCreditRepository.findByMovie(movie)).thenReturn(Collections.emptyList());

        Map<ArtistRole, List<MovieCredit>> result = movieCreditService.getCreditsByMovieGroupedByRole(movie);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenFindCreditById_andCreditExists_thenReturnMovieCredit() {
        UUID creditId = UUID.randomUUID();
        MovieCredit credit = MovieCredit.builder()
                .id(creditId)
                .roleType(ArtistRole.ACTOR)
                .build();
        when(movieCreditRepository.findById(creditId)).thenReturn(Optional.of(credit));

        MovieCredit result = movieCreditService.findCreditById(creditId);

        assertNotNull(result);
        assertEquals(creditId, result.getId());
    }

    @Test
    void whenFindCreditById_andCreditDoesNotExist_thenThrowsException() {
        UUID creditId = UUID.randomUUID();
        when(movieCreditRepository.findById(creditId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieCreditService.findCreditById(creditId)
        );

        assertTrue(exception.getMessage().contains(creditId.toString()));
    }

    @Test
    void whenDeleteCredit_andCreditExists_thenDeleteFromRepository() {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Title").build();
        Artist artist = Artist.builder().name("Actor").build();
        MovieCredit credit = MovieCredit.builder()
                .id(UUID.randomUUID())
                .movie(movie)
                .artist(artist)
                .roleType(ArtistRole.ACTOR)
                .build();

        movieCreditService.deleteCredit(credit);

        verify(movieCreditRepository).delete(credit);
    }

    @Test
    void whenFindAllCreditsByArtist_andArtistHasMultipleCredits_thenReturnCountOfCredits() {
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Actor").build();
        List<MovieCredit> credits = List.of(
                MovieCredit.builder().artist(artist).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().artist(artist).roleType(ArtistRole.PRODUCER).build(),
                MovieCredit.builder().artist(artist).roleType(ArtistRole.ACTOR).build()
        );
        when(movieCreditRepository.findAllByArtist(artist)).thenReturn(credits);

        int result = movieCreditService.findAllCreditsByArtist(artist);

        assertEquals(3, result);
    }

    @Test
    void whenFindAllCreditsByArtist_andArtistHasNoCredits_thenReturnZero() {
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Actor").build();
        when(movieCreditRepository.findAllByArtist(artist)).thenReturn(Collections.emptyList());

        int result = movieCreditService.findAllCreditsByArtist(artist);

        assertEquals(0, result);
    }

    @Test
    void whenGetCreditsByArtistGrouped_andArtistHasMultipleRoles_thenReturnMapGroupedByRoleAndSortedByReleaseDate() {
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Actor").build();
        Movie oldMovie = Movie.builder().title("Old Movie").releaseDate(LocalDate.of(1990, 1, 1)).build();
        Movie newMovie = Movie.builder().title("New Movie").releaseDate(LocalDate.of(2020, 1, 1)).build();

        List<MovieCredit> credits = List.of(
                MovieCredit.builder().artist(artist).movie(oldMovie).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().artist(artist).movie(newMovie).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().artist(artist).movie(newMovie).roleType(ArtistRole.DIRECTOR).build()
        );
        when(movieCreditRepository.findAllByArtist(artist)).thenReturn(credits);

        Map<ArtistRole, List<MovieCredit>> result = movieCreditService.getCreditsByArtistGrouped(artist);

        assertEquals(2, result.size());
        assertTrue(result.containsKey(ArtistRole.ACTOR));
        assertTrue(result.containsKey(ArtistRole.DIRECTOR));

        // Verify sorted by release date descending (newest first)
        List<MovieCredit> actorCredits = result.get(ArtistRole.ACTOR);
        assertEquals(2, actorCredits.size());
        assertEquals("New Movie", actorCredits.get(0).getMovie().getTitle());
        assertEquals("Old Movie", actorCredits.get(1).getMovie().getTitle());
    }

    @Test
    void whenGetCreditsByArtistGrouped_andArtistHasNoCredits_thenReturnEmptyMap() {
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Artist").build();
        when(movieCreditRepository.findAllByArtist(artist)).thenReturn(Collections.emptyList());

        Map<ArtistRole, List<MovieCredit>> result = movieCreditService.getCreditsByArtistGrouped(artist);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetCreditsByArtistGrouped_andMoviesHaveNullReleaseDates_thenSortWithNullsLast() {
        Artist artist = Artist.builder().id(UUID.randomUUID()).name("Artist").build();
        Movie movieWithDate = Movie.builder().title("Title 1").releaseDate(LocalDate.of(2020, 1, 1)).build();
        Movie movieWithoutDate = Movie.builder().title("Title 2").releaseDate(null).build();

        List<MovieCredit> credits = new ArrayList<>(List.of(
                MovieCredit.builder().artist(artist).movie(movieWithoutDate).roleType(ArtistRole.ACTOR).build(),
                MovieCredit.builder().artist(artist).movie(movieWithDate).roleType(ArtistRole.ACTOR).build()
        ));
        when(movieCreditRepository.findAllByArtist(artist)).thenReturn(credits);

        Map<ArtistRole, List<MovieCredit>> result = movieCreditService.getCreditsByArtistGrouped(artist);

        List<MovieCredit> actorCredits = result.get(ArtistRole.ACTOR);
        assertEquals("Title 1", actorCredits.get(0).getMovie().getTitle());
        assertEquals("Title 2", actorCredits.get(1).getMovie().getTitle());
    }
}
