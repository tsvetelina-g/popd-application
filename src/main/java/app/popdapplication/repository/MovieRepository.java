package app.popdapplication.repository;

import app.popdapplication.model.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    List<Movie> findByTitleContainingIgnoreCase(String query);

    @Query("""
    SELECT m FROM Movie m 
    WHERE m.releaseDate IS NOT NULL 
    ORDER BY ABS(DATEDIFF(:today, m.releaseDate)) ASC
    """)
    List<Movie> findTop10ByClosestReleaseDate(LocalDate today, Pageable pageable);
}