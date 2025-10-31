package app.popdapplication.service;

import app.popdapplication.repository.WatchedMovieRepository;
import org.springframework.stereotype.Service;

@Service
public class WatchlistMovieService {

    private final WatchedMovieRepository watchedMovieRepository;

    public WatchlistMovieService(WatchedMovieRepository watchedMovieRepository) {
        this.watchedMovieRepository = watchedMovieRepository;
    }
}
