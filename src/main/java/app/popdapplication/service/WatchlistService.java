package app.popdapplication.service;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.repository.WatchlistRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistMovieService watchlistMovieService;

    @Autowired
    public WatchlistService(WatchlistRepository watchlistRepository, WatchlistMovieService watchlistMovieService) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistMovieService = watchlistMovieService;
    }

    public void createDefaultWatchlist(User user) {

        Watchlist watchlist = Watchlist.builder()
                .user(user)
                .name("Default")
                .watchlistType(WatchlistType.DEFAULT)
                .createdOn(LocalDateTime.now())
                .updated(LocalDateTime.now())
                .build();

        watchlistRepository.save(watchlist);
    }

    public boolean movieIsInWatchlist(Movie movie, User user) {

        Watchlist watchlist = watchlistRepository.findByUser(user);

        Optional<WatchlistMovie> watchlistMovieOpt = watchlistMovieService.findByWatchlistAndMovie(watchlist, movie);

        return watchlistMovieOpt.isPresent();
    }

    @Transactional
    public void addToWatchlist(Movie movie, User user) {
        Watchlist watchlist = watchlistRepository.findByUser(user);

        watchlistMovieService.saveToWatchlist(watchlist, movie);

        watchlist.setUpdated(LocalDateTime.now());

        watchlistRepository.save(watchlist);
    }

    @Transactional
    public void removeFromWatchlist(Movie movie, User user) {
        Watchlist watchlist = watchlistRepository.findByUser(user);

        watchlistMovieService.removeFromWatchlist(watchlist, movie);

        watchlist.setUpdated(LocalDateTime.now());

        watchlistRepository.save(watchlist);
    }

    public int countMoviesInWatchlist(User user) {
        Watchlist watchlist = watchlistRepository.findByUser(user);

        return watchlistMovieService.findAllByWatchlist(watchlist).size();
    }

    public Watchlist findByUser(User user) {
        return watchlistRepository.findByUser(user);
    }
}
