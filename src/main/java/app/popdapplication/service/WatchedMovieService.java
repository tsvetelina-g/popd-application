package app.popdapplication.service;

import app.popdapplication.model.entity.User;
import app.popdapplication.repository.WatchedMovieRepository;
import org.springframework.stereotype.Service;

@Service
public class WatchedMovieService {

    private final WatchedMovieRepository watchedMovieRepository;

    public WatchedMovieService(WatchedMovieRepository watchedMovieRepository) {
        this.watchedMovieRepository = watchedMovieRepository;
    }

    public int countWatchedMovies(User user) {
        return watchedMovieRepository.findAllByUser(user).size();
    }
}
