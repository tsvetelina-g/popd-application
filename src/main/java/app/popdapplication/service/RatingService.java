package app.popdapplication.service;

import app.popdapplication.model.entity.User;
import app.popdapplication.repository.RatingRepository;
import org.springframework.stereotype.Service;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public int countMoviesRated(User user) {
        return ratingRepository.findAllByUser(user).size();
    }
}
