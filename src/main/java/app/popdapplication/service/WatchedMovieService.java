package app.popdapplication.service;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import app.popdapplication.repository.WatchedMovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class WatchedMovieService {

    private final WatchedMovieRepository watchedMovieRepository;

    @Autowired
    public WatchedMovieService(WatchedMovieRepository watchedMovieRepository) {
        this.watchedMovieRepository = watchedMovieRepository;
    }

    public int countWatchedMovies(User user) {
        return watchedMovieRepository.findAllByUser(user).size();
    }

    public boolean movieIsWatched(Movie movie, User user) {
        Optional<WatchedMovie> watchedMovieOpt = watchedMovieRepository.findByUserAndMovie(user, movie);

        return watchedMovieOpt.isPresent();
    }

    public void addToWatched(Movie movie, User user) {

        WatchedMovie watchedMovie = WatchedMovie.builder()
                .movie(movie)
                .user(user)
                .createdOn(LocalDateTime.now())
                .build();

        watchedMovieRepository.save(watchedMovie);
    }

    public void removeFromWatched(Movie movie, User user) {

        Optional<WatchedMovie> watchedMovie = watchedMovieRepository.findByUserAndMovie(user, movie);

        watchedMovieRepository.delete(watchedMovie.get());
    }

    public int usersWatchedCount(UUID movieId) {
        return watchedMovieRepository.findAllByMovieId(movieId).size();
    }

    public Page<WatchedMovie> findAllByUserOrderByCreatedOnDesc(User user, Pageable pageable) {
        return watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(user, pageable);
    }
}
