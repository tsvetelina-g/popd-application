package app.popdapplication.repository;

import app.popdapplication.model.entity.MovieCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieCreditRepository extends JpaRepository<MovieCredit, UUID> {
}


