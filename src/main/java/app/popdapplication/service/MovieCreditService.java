package app.popdapplication.service;

import app.popdapplication.repository.MovieCreditRepository;
import org.springframework.stereotype.Service;

@Service
public class MovieCreditService {

    private final MovieCreditRepository movieCreditRepository;

    public MovieCreditService(MovieCreditRepository movieCreditRepository) {
        this.movieCreditRepository = movieCreditRepository;
    }
}
