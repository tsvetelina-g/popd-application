package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceUTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreService genreService;

    @InjectMocks
    private MovieService movieService;

    @Test
    void whenAddMovie_withValidRequest_thenSaveAndReturnMovie() {
        UUID genreId = UUID.randomUUID();
        List<Genre> genres = List.of(Genre.builder().id(genreId).name("Action").build());
        AddMovieRequest request = AddMovieRequest.builder()
                .title("It")
                .description("Test movie description")
                .releaseDate(LocalDate.of(2024, 1, 15))
                .genresIds(List.of(genreId))
                .posterUrl("www.poster.com")
                .backgroundImage("www.poster.com")
                .build();

        when(genreService.findAllById(List.of(genreId))).thenReturn(genres);
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Movie result = movieService.addMovie(request);

        assertNotNull(result);
        assertEquals("It", result.getTitle());
        assertEquals("Test movie description", result.getDescription());
        assertEquals(LocalDate.of(2024, 1, 15), result.getReleaseDate());
        assertEquals(genres, result.getGenres());
        verify(movieRepository).save(any(Movie.class));
        verify(genreService).findAllById(List.of(genreId));
    }

    @Test
    void whenAddMovie_withNoGenres_thenSaveMovieWithEmptyGenres() {
        AddMovieRequest request = AddMovieRequest.builder()
                .title("It")
                .genresIds(Collections.emptyList())
                .build();

        when(genreService.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Movie result = movieService.addMovie(request);

        assertNotNull(result);
        assertEquals("It", result.getTitle());
        assertTrue(result.getGenres().isEmpty());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void whenFindById_andMovieExists_thenReturnMovie() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder()
                .id(movieId)
                .title("Existing Movie")
                .build();
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        Movie result = movieService.findById(movieId);

        assertNotNull(result);
        assertEquals(movieId, result.getId());
        assertEquals("Existing Movie", result.getTitle());
    }

    @Test
    void whenFindById_andMovieNotFound_thenThrowsException() {
        UUID movieId = UUID.randomUUID();
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> movieService.findById(movieId));
    }

    @Test
    void whenUpdateMovieInfo_withValidRequest_thenUpdateAndSaveMovie() {
        UUID movieId = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        Movie existingMovie = Movie.builder()
                .id(movieId)
                .title("Old Title")
                .description("Old description")
                .build();
        List<Genre> newGenres = List.of(Genre.builder().id(genreId).name("Comedy").build());
        EditMovieRequest request = EditMovieRequest.builder()
                .title("New Title")
                .description("New description")
                .releaseDate(LocalDate.of(1997, 6, 1))
                .genresIds(List.of(genreId))
                .posterUrl("www.poster.com")
                .backgroundImage("www.poster.com")
                .build();

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(existingMovie));
        when(genreService.findAllById(List.of(genreId))).thenReturn(newGenres);

        movieService.updateMovieInfo(movieId, request);

        assertEquals("New Title", existingMovie.getTitle());
        assertEquals("New description", existingMovie.getDescription());
        assertEquals(LocalDate.of(1997, 6, 1), existingMovie.getReleaseDate());
        assertEquals(newGenres, existingMovie.getGenres());
        assertEquals("www.poster.com", existingMovie.getPosterUrl());
        assertEquals("www.poster.com", existingMovie.getBackgroundImage());
        verify(movieRepository).save(existingMovie);
    }

    @Test
    void whenSearchByTitleLimited_andMoviesAreFound_thenReturnLimitedResults() {
        List<Movie> movies = List.of(
                Movie.builder().title("Movie 1").build(),
                Movie.builder().title("Movie 2").build(),
                Movie.builder().title("Movie 3").build()
        );
        when(movieRepository.findByTitleContainingIgnoreCase("Movie")).thenReturn(movies);

        List<Movie> result = movieService.searchByTitleLimited("Movie", 2);

        assertEquals(2, result.size());
    }

    @Test
    void whenSearchByTitleLimited_andNoMoviesAreFound_thenReturnEmptyList() {
        when(movieRepository.findByTitleContainingIgnoreCase("xyz")).thenReturn(Collections.emptyList());

        List<Movie> result = movieService.searchByTitleLimited("xyz", 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void whenSearchByTitleLimited_andLimitGreaterThanResults_thenReturnAllResults() {
        List<Movie> movies = List.of(
                Movie.builder().title("Movie 1").build(),
                Movie.builder().title("Movie 2").build()
        );
        when(movieRepository.findByTitleContainingIgnoreCase("Movie")).thenReturn(movies);

        List<Movie> result = movieService.searchByTitleLimited("Movie", 10);

        assertEquals(2, result.size());
    }

    @Test
    void whenGetMovieNamesByIds_withValidIds_thenReturnMap() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        Movie movie1 = Movie.builder().id(movieId1).title("Movie One").build();
        Movie movie2 = Movie.builder().id(movieId2).title("Movie Two").build();

        when(movieRepository.findById(movieId1)).thenReturn(Optional.of(movie1));
        when(movieRepository.findById(movieId2)).thenReturn(Optional.of(movie2));

        Set<UUID> movieIds = Set.of(movieId1, movieId2);
        Map<UUID, String> result = movieService.getMovieNamesByIds(movieIds);

        assertEquals(2, result.size());
        assertEquals("Movie One", result.get(movieId1));
        assertEquals("Movie Two", result.get(movieId2));
    }

    @Test
    void whenGetMovieNamesByIds_withNullSet_thenReturnEmptyMap() {
        Map<UUID, String> result = movieService.getMovieNamesByIds(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetMovieNamesByIds_withEmptySet_thenReturnEmptyMap() {
        Map<UUID, String> result = movieService.getMovieNamesByIds(Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetMovieNamesByIds_andSomeMoviesNotFound_thenSkipAndContinue() {
        UUID validId = UUID.randomUUID();
        UUID invalidId = UUID.randomUUID();
        Movie movie = Movie.builder().id(validId).title("Valid Movie").build();

        when(movieRepository.findById(validId)).thenReturn(Optional.of(movie));
        when(movieRepository.findById(invalidId)).thenReturn(Optional.empty());

        Set<UUID> movieIds = Set.of(validId, invalidId);
        Map<UUID, String> result = movieService.getMovieNamesByIds(movieIds);

        assertEquals(1, result.size());
        assertEquals("Valid Movie", result.get(validId));
    }

    @Test
    void whenGetTopMovies_withNullMovieIds_thenReturnFallbackMovies() {
        List<Movie> fallbackMovies = List.of(
                Movie.builder().title("Fallback 1").build(),
                Movie.builder().title("Fallback 2").build()
        );
        when(movieRepository.findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(fallbackMovies);

        List<Movie> result = movieService.getTopMovies(null);

        assertEquals(2, result.size());
        verify(movieRepository).findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    void whenGetTopMovies_withEmptyMovieIds_thenReturnFallbackMovies() {
        List<Movie> fallbackMovies = List.of(
                Movie.builder().title("Fallback 1").build()
        );
        when(movieRepository.findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(fallbackMovies);

        List<Movie> result = movieService.getTopMovies(Collections.emptyList());

        assertEquals(1, result.size());
    }

    @Test
    void whenGetTopMovies_andHas10Movies_thenReturnAllWithoutFallback() {
        List<UUID> movieIds = new ArrayList<>();
        List<Movie> movies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID id = UUID.randomUUID();
            movieIds.add(id);
            movies.add(Movie.builder().id(id).title("Movie " + i).build());
        }
        when(movieRepository.findAllById(movieIds)).thenReturn(movies);

        List<Movie> result = movieService.getTopMovies(movieIds);

        assertEquals(10, result.size());
        verify(movieRepository, never()).findTop10ByClosestReleaseDate(any(), any());
    }

    @Test
    void whenGetTopMovies_withLessThan10Movies_thenFillWithFallbackMovies() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<UUID> movieIds = List.of(id1, id2);
        List<Movie> movies = new ArrayList<>(List.of(
                Movie.builder().id(id1).title("Movie 1").build(),
                Movie.builder().id(id2).title("Movie 2").build()
        ));
        List<Movie> fallbackMovies = List.of(
                Movie.builder().id(UUID.randomUUID()).title("Fallback 1").build(),
                Movie.builder().id(UUID.randomUUID()).title("Fallback 2").build()
        );

        when(movieRepository.findAllById(movieIds)).thenReturn(movies);
        when(movieRepository.findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(fallbackMovies);

        List<Movie> result = movieService.getTopMovies(movieIds);

        assertEquals(4, result.size());
        verify(movieRepository).findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    void whenGetTop5MoviesByGenreClosestReleaseDate_thenReturnTop5Movies() {
        UUID genreId = UUID.randomUUID();
        List<Movie> movies = List.of(
                Movie.builder().title("Movie 1").build(),
                Movie.builder().title("Movie 2").build()
        );
        when(movieRepository.findTop5ByGenreIdClosestReleaseDate(
                eq(genreId), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(movies);

        List<Movie> result = movieService.getTop5MoviesByGenreClosestReleaseDate(genreId);

        assertEquals(2, result.size());
        verify(movieRepository).findTop5ByGenreIdClosestReleaseDate(
                eq(genreId), any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    void whenGetTop5MostRecentReleases_thenReturnTop5Movies() {
        List<Movie> movies = List.of(
                Movie.builder().title("Movie 1").build(),
                Movie.builder().title("Movie 2").build()
        );
        when(movieRepository.findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(movies);

        List<Movie> result = movieService.getTop5MostRecentReleases();

        assertEquals(2, result.size());
        verify(movieRepository).findTop10ByClosestReleaseDate(any(LocalDate.class), any(PageRequest.class));
    }

    @Test
    void whenGetMovieNamesByIds_withNullId_thenSkipNullId() {
        UUID validId = UUID.randomUUID();
        Movie movie = Movie.builder().id(validId).title("Valid Movie").build();
        Set<UUID> movieIds = new HashSet<>();
        movieIds.add(validId);
        movieIds.add(null);

        when(movieRepository.findById(validId)).thenReturn(Optional.of(movie));

        Map<UUID, String> result = movieService.getMovieNamesByIds(movieIds);

        assertEquals(1, result.size());
        assertEquals("Valid Movie", result.get(validId));
    }

    @Test
    void whenGetMovieNamesByIds_withDuplicateIds_thenSkipDuplicates() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Movie").build();
        Set<UUID> movieIds = new HashSet<>();
        movieIds.add(movieId);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        Map<UUID, String> result = movieService.getMovieNamesByIds(movieIds);

        assertEquals(1, result.size());
        assertEquals("Movie", result.get(movieId));
        verify(movieRepository, times(1)).findById(movieId);
    }

    @Test
    void whenGetMovieNamesByIds_andExceptionThrown_thenSkipAndContinue() {
        UUID validId = UUID.randomUUID();
        UUID invalidId = UUID.randomUUID();
        Movie movie = Movie.builder().id(validId).title("Valid Movie").build();

        when(movieRepository.findById(validId)).thenReturn(Optional.of(movie));
        when(movieRepository.findById(invalidId)).thenThrow(new RuntimeException("Database error"));

        Set<UUID> movieIds = Set.of(validId, invalidId);
        Map<UUID, String> result = movieService.getMovieNamesByIds(movieIds);

        assertEquals(1, result.size());
        assertEquals("Valid Movie", result.get(validId));
    }
}
