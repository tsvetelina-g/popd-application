package app.popdapplication.service;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.web.dto.AddMovieRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final GenreService genreService;

    public MovieService(MovieRepository movieRepository, GenreService genreService) {
        this.movieRepository = movieRepository;
        this.genreService = genreService;
    }

    public void addMovie(AddMovieRequest addMovieRequest) {

        List<Genre> genres = genreService.findAllById(addMovieRequest.getGenresIds());

        Movie movie = Movie.builder()
                .title(addMovieRequest.getTitle())
                .description(addMovieRequest.getDescription())
                .genres(genres)
                .releaseDate(addMovieRequest.getReleaseDate())
                .posterUrl(addMovieRequest.getPosterUrl())
                .build();

        movieRepository.save(movie);
    }
}