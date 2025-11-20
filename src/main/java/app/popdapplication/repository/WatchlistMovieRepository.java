package app.popdapplication.repository;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchlistMovieRepository extends JpaRepository<WatchlistMovie, UUID> {
    Optional<WatchlistMovie> findByWatchlistAndMovie(Watchlist watchlist, Movie movie);

    List<WatchlistMovie> findAllByWatchlist(Watchlist watchlist);

    Page<WatchlistMovie> findAllByWatchlistOrderByAddedOnDesc(Watchlist watchlist, Pageable pageable);
}
