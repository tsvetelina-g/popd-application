package app.popdapplication.service;

import app.popdapplication.model.entity.User;
import app.popdapplication.repository.ReviewRepository;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public int countMoviesReviewed(User user) {
        return reviewRepository.findAllByUser(user).size();
    }
}
