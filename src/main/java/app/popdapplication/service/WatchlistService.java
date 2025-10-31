package app.popdapplication.service;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.repository.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public WatchlistService(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
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
}
