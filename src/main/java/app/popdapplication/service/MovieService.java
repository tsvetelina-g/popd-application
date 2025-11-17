package app.popdapplication.service;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreService genreService;

    @Autowired
    public MovieService(MovieRepository movieRepository, GenreService genreService) {
        this.movieRepository = movieRepository;
        this.genreService = genreService;
    }

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
}