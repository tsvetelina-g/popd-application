package app.popdapplication.repository;

import app.popdapplication.model.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    @Query("SELECT a FROM Artist a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY a.name")
    List<Artist> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT DISTINCT a FROM Artist a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :word1, '%')) AND " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :word2, '%')) " +
           "ORDER BY a.name")
    List<Artist> findByNameContainingBothWords(@Param("word1") String word1, @Param("word2") String word2);
    
    @Query("SELECT a FROM Artist a ORDER BY a.name")
    List<Artist> findAllByOrderByName();
    
    Optional<Artist> findByNameIgnoreCase(String name);
}


