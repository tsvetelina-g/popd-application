package app.popdapplication.repository;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, UUID> {
    Collection<WatchedMovie> findAllByUser(User user);

    Optional<WatchedMovie> findByUserAndMovie(User user, Movie movie);

    Collection<WatchedMovie> findAllByMovieId(UUID movieId);

    Page<WatchedMovie> findAllByUserOrderByCreatedOnDesc(User user, Pageable pageable);
}
