package app.popdapplication.repository;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieCreditRepository extends JpaRepository<MovieCredit, UUID> {
    
    Optional<MovieCredit> findByMovieAndArtistAndRoleType(Movie movie, Artist artist, ArtistRole roleType);
    
    List<MovieCredit> findByMovie(Movie movie);

    List<MovieCredit> findAllByArtist(Artist artist);
}


