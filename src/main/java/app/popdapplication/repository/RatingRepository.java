package app.popdapplication.repository;

import app.popdapplication.model.entity.Rating;
import app.popdapplication.model.entity.User;
import io.micrometer.core.instrument.Counter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    Collection<Rating> findAllByUser(User user);
}
