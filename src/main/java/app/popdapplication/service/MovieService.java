package app.popdapplication.service;

import app.popdapplication.model.entity.Activity;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return movie;
    }

    public Movie findById(UUID id) {
        return movieRepository.findById(id).orElseThrow(() -> new RuntimeException("Movie with [%s] id not found".formatted(id)));
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
    }

    public List<Movie> searchByTitle(String query) {
        return movieRepository.findByTitleContainingIgnoreCase(query);
    }

    public Map<UUID, String> getMovieNamesByIds(Set<UUID> movieIds) {
        Map<UUID, String> result = new HashMap<>();

        if (movieIds == null || movieIds.isEmpty()) {
            return result;
        }

        for (UUID movieId : movieIds) {
            if (movieId == null || result.containsKey(movieId)) {
                continue; // skip nulls and already processed movies
            }

            try {
                Movie movie = findById(movieId);
                if (movie != null) {
                    result.put(movieId, movie.getTitle());
                }
            } catch (Exception e) {
                // Movie not found, skip
            }
        }

        return result;
    }

    public Map<UUID, String> getMovieNamesForActivities(List<Activity> activities) {
        Map<UUID, String> result = new HashMap<>();

        if (activities == null || activities.isEmpty()) {
            return result;
        }

        for (Activity activity : activities) {
            UUID movieId = activity.getMovieId();

            if (movieId == null || result.containsKey(movieId)) {
                continue;
            }

            try {
                Movie movie = findById(movieId);
                if (movie != null) {
                    result.put(movieId, movie.getTitle());
                }
            } catch (Exception e) {
                // Log warning if needed
            }
        }

        return result;
    }

}