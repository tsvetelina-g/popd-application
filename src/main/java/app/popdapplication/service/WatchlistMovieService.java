package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.AlreadyExistsException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.model.enums.ActivityType;
import app.popdapplication.repository.WatchlistMovieRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class WatchlistMovieService {

    private final WatchlistMovieRepository watchlistMovieRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public WatchlistMovieService(WatchlistMovieRepository watchlistMovieRepository, ApplicationEventPublisher eventPublisher) {
        this.watchlistMovieRepository = watchlistMovieRepository;
        this.eventPublisher = eventPublisher;
    }

    public Optional<WatchlistMovie> findByWatchlistAndMovie(Watchlist watchlist, Movie movie) {
        return watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie);
    }

    @Transactional
    public void saveToWatchlist(Watchlist watchlist, Movie movie) {
        if (findByWatchlistAndMovie(watchlist, movie).isPresent()) {
            throw new AlreadyExistsException("Movie already in watchlist");
        }

        WatchlistMovie watchlistMovie = WatchlistMovie.builder()
                .movie(movie)
                .watchlist(watchlist)
                .addedOn(LocalDateTime.now())
                .build();

        watchlistMovieRepository.save(watchlistMovie);
        log.info("Movie added to watchlist: movie id {}, user id {}", movie.getId(), watchlist.getUser().getId());

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(watchlist.getUser().getId())
                .movieId(movie.getId())
                .type(ActivityType.ADDED_TO_WATCHLIST)
                .removed(false)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void removeFromWatchlist(Watchlist watchlist, Movie movie) {
        Optional<WatchlistMovie> watchlistMovie = watchlistMovieRepository.findByWatchlistAndMovie(watchlist, movie);

        if (watchlistMovie.isPresent()){
            watchlistMovieRepository.delete(watchlistMovie.get());
            log.info("Movie removed from watchlist: movie id {}, user id {}", movie.getId(), watchlist.getUser().getId());
        } else {
            throw new NotFoundException("Movie not found in watchlist");
        }

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(watchlist.getUser().getId())
                .movieId(movie.getId())
                .type(ActivityType.ADDED_TO_WATCHLIST)
                .removed(true)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    public List<WatchlistMovie> findAllByWatchlist(Watchlist watchlist) {
        return watchlistMovieRepository.findAllByWatchlist(watchlist);
    }

    public Page<WatchlistMovie> findAllByWatchlistOrderByAddedOnDesc(Watchlist watchlist, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 10;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return findAllByWatchlistOrderByAddedOnDesc(watchlist, pageable);
    }

    private Page<WatchlistMovie> findAllByWatchlistOrderByAddedOnDesc(Watchlist watchlist, Pageable pageable) {
        return watchlistMovieRepository.findAllByWatchlistOrderByAddedOnDesc(watchlist, pageable);
    }
}
