package app.popdapplication.service;

import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import app.popdapplication.model.enums.ActivityType;
import app.popdapplication.repository.WatchedMovieRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class WatchedMovieService {

    private final WatchedMovieRepository watchedMovieRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public WatchedMovieService(WatchedMovieRepository watchedMovieRepository, ApplicationEventPublisher eventPublisher) {
        this.watchedMovieRepository = watchedMovieRepository;
        this.eventPublisher = eventPublisher;
    }

    public int countWatchedMovies(User user) {
        return watchedMovieRepository.findAllByUser(user).size();
    }

    public boolean movieIsWatched(Movie movie, User user) {
        Optional<WatchedMovie> watchedMovieOpt = watchedMovieRepository.findByUserAndMovie(user, movie);

        return watchedMovieOpt.isPresent();
    }

    @Transactional
    public void addToWatched(Movie movie, User user) {

        WatchedMovie watchedMovie = WatchedMovie.builder()
                .movie(movie)
                .user(user)
                .createdOn(LocalDateTime.now())
                .build();

        watchedMovieRepository.save(watchedMovie);
        log.info("Movie marked as watched: movie id {}, user id {}", movie.getId(), user.getId());

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(user.getId())
                .movieId(movie.getId())
                .type(ActivityType.WATCHED)
                .removed(false)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    @Transactional
    public void removeFromWatched(Movie movie, User user) {

        Optional<WatchedMovie> watchedMovie = watchedMovieRepository.findByUserAndMovie(user, movie);

        if (watchedMovie.isEmpty()) {
            throw new NotFoundException("Movie not found in watched list");
        }
        watchedMovieRepository.delete(watchedMovie.get());
        log.info("Movie removed from watched list: movie id {}, user id {}", movie.getId(), user.getId());

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(user.getId())
                .movieId(movie.getId())
                .type(ActivityType.WATCHED)
                .removed(true)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    public int usersWatchedCount(UUID movieId) {
        return watchedMovieRepository.findAllByMovieId(movieId).size();
    }

    public Page<WatchedMovie> findAllByUserOrderByCreatedOnDesc(User user, int page, int size) {
        // Validate and normalize pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 10;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return findAllByUserOrderByCreatedOnDesc(user, pageable);
    }

    public Page<WatchedMovie> findAllByUserOrderByCreatedOnDesc(User user, Pageable pageable) {
        return watchedMovieRepository.findAllByUserOrderByCreatedOnDesc(user, pageable);
    }
}
