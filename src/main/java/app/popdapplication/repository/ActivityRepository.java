package app.popdapplication.repository;

import app.popdapplication.model.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findAllByUserIdOrderByCreatedOnDesc(UUID userId);

    long deleteAllByCreatedOnBefore(LocalDateTime oneYearAgo);
}
