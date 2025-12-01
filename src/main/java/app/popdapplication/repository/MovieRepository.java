package app.popdapplication.repository;

import app.popdapplication.model.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    List<Movie> findByTitleContainingIgnoreCase(String query);

    @Query(
            value = """
            SELECT * FROM movie m
            WHERE m.release_date IS NOT NULL
            ORDER BY ABS(DATEDIFF(:today, m.release_date)) ASC
            """,
            nativeQuery = true
    )
    List<Movie> findTop10ByClosestReleaseDate(LocalDate today, Pageable pageable);

    @Query(
            value = """
    SELECT DISTINCT m.* FROM movie m
    INNER JOIN movie_genres mg ON m.id = mg.movie_id
    WHERE mg.genres_id = :genreId
    AND m.release_date IS NOT NULL
    ORDER BY ABS(DATEDIFF(:today, m.release_date)) ASC
    """,
            nativeQuery = true
    )
    List<Movie> findTop5ByGenreIdClosestReleaseDate(@Param("genreId") UUID genreId, @Param("today") LocalDate today, Pageable pageable);
}