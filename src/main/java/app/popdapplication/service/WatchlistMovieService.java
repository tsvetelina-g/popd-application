package app.popdapplication.service;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.repository.WatchlistMovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WatchlistMovieService {

    private final WatchlistMovieRepository watchlistMovieRepository;

    @Autowired
    public WatchlistMovieService(WatchlistMovieRepository watchlistMovieRepository) {
        this.watchlistMovieRepository = watchlistMovieRepository;
    }

    public Optional<WatchlistMovie> findByWatchlistAndMovie(Watchlist watchlist, Movie movie) {
        return watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie);
    }

    public void saveToWatchlist(Watchlist watchlist, Movie movie) {

        WatchlistMovie watchlistMovie = WatchlistMovie.builder()
                .movie(movie)
                .watchlist(watchlist)
                .addedOn(LocalDateTime.now())
                .build();

        watchlistMovieRepository.save(watchlistMovie);
    }

    public void removeFromWatchlist(Watchlist watchlist, Movie movie) {

        Optional<WatchlistMovie> watchlistMovie = watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie);

        watchlistMovieRepository.delete(watchlistMovie.get());
    }

    public List<WatchlistMovie> findAllByWatchlist(Watchlist watchlist) {
        return watchlistMovieRepository.findAllByWatchlist(watchlist);
    }

    public Page<WatchlistMovie> findAllByWatchlistOrderByAddedOnDesc(Watchlist watchlist, Pageable pageable) {
        return watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(watchlist, pageable);
    }
}
