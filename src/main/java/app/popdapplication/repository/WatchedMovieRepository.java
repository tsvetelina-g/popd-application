package app.popdapplication.repository;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface WatchedMovieRepository extends JpaRepository<WatchedMovie, UUID> {
    Collection<Object> findAllByUser(User user);
}
