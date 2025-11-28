package app.popdapplication.repository;

import app.popdapplication.model.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    List<Activity> findAllByUserIdOrderByCreatedOnDesc(UUID userId);

    long deleteAllByCreatedOnBefore(LocalDateTime sixMonthsAgo);

    @Query("""
    SELECT a.movieId
    FROM Activity a
    WHERE a.createdOn > :afterDate
    GROUP BY a.movieId
    ORDER BY SUM(CASE 
                   WHEN a.type = 'RATED' THEN 4
                   WHEN a.type = 'REVIEWED' THEN 5
                   WHEN a.type = 'WATCHED' THEN 3
                   WHEN a.type = 'ADDED_TO_WATCHLIST' THEN 2
                   ELSE 0
               END) DESC
    """)
    List<UUID> findTopMovieIds(LocalDateTime afterDate, Pageable pageable);
}
