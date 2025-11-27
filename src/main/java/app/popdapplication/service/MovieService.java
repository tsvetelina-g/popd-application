package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreService genreService;

    @Autowired
    public MovieService(MovieRepository movieRepository, GenreService genreService) {
        this.movieRepository = movieRepository;
        this.genreService = genreService;
    }

    @Transactional
    public Movie addMovie(AddMovieRequest addMovieRequest) {

        List<Genre> genres = genreService.findAllById(addMovieRequest.getGenresIds());

        Movie movie = Movie.builder()
                .title(addMovieRequest.getTitle())
                .description(addMovieRequest.getDescription())
                .genres(genres)
                .releaseDate(addMovieRequest.getReleaseDate())
                .posterUrl(addMovieRequest.getPosterUrl())
                .backgroundImage(addMovieRequest.getBackgroundImage())
                .build();

        movieRepository.save(movie);
        log.info("Movie added successfully with id: {} and title: {}", movie.getId(), movie.getTitle());
        return movie;
    }

    public Movie findById(UUID id) {
        return movieRepository.findById(id).orElseThrow(() -> new NotFoundException("Movie with id [%s] not found".formatted(id)));
    }

    @Transactional
    public void updateMovieInfo(UUID movieId, EditMovieRequest editMovieRequest) {

        Movie movie = movieRepository.getReferenceById(movieId);
        List<Genre> genres = genreService.findAllById(editMovieRequest.getGenresIds());

        movie.setTitle(editMovieRequest.getTitle());
        movie.setDescription(editMovieRequest.getDescription());
        movie.setPosterUrl(editMovieRequest.getPosterUrl());
        movie.setBackgroundImage(editMovieRequest.getBackgroundImage());
        movie.setReleaseDate(editMovieRequest.getReleaseDate());
        movie.setGenres(genres);

        movieRepository.save(movie);
        log.info("Movie info updated for movie with id: {}", movieId);
    }

    private List<Movie> searchByTitle(String query) {
        return movieRepository.findByTitleContainingIgnoreCase(query);
    }

    public List<Movie> searchByTitleLimited(String query, int limit) {
        return searchByTitle(query).stream()
                .limit(limit)
                .toList();
    }

    public Map<UUID, String> getMovieNamesByIds(Set<UUID> movieIds) {
        Map<UUID, String> result = new HashMap<>();

        if (movieIds == null || movieIds.isEmpty()) {
            return result;
        }

        for (UUID movieId : movieIds) {
            if (movieId == null || result.containsKey(movieId)) {
                continue;
            }

            try {
                Movie movie = findById(movieId);
                if (movie != null) {
                    result.put(movieId, movie.getTitle());
                }
            } catch (Exception e) {
                log.warn("Could not fetch movie with id {}: {}", movieId, e.getMessage());
            }
        }

        return result;
    }

    private List<Movie> findTop10ByClosestReleaseDate() {
        LocalDate today = LocalDate.now();
        return movieRepository.findTop10ByClosestReleaseDate(today, PageRequest.of(0, 10));
    }

    public List<Movie> getTopMovies(List<UUID> movieIds) {

        if (movieIds == null || movieIds.isEmpty()) {
            return findTop10ByClosestReleaseDate();
        }

        List<Movie> movies = movieRepository.findAllById(movieIds);

        if (movies.size() < 10) {
            List<Movie> fallbackMovies = findTop10ByClosestReleaseDate();

            for (Movie m : fallbackMovies) {
                if (movies.size() == 10) {
                    break;
                }
                if (!movies.contains(m)) {
                    movies.add(m);
                }
            }

        }
        return movies;
    }
}