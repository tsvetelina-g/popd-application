package app.popdapplication.service;

import app.popdapplication.client.RatingClient;
import app.popdapplication.client.RatingDto.MovieRatingStatsResponse;
import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.RatingDto.RatingRequest;
import app.popdapplication.client.RatingDto.UserRatingStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class RatingService {

    private final RatingClient client;
    private final ReviewService reviewService;

    @Autowired
    public RatingService(RatingClient client, ReviewService reviewService) {
        this.client = client;
        this.reviewService = reviewService;
    }

    public void upsertRating(UUID userId, UUID movieId, int value) {

        try {
            RatingRequest ratingRequest = RatingRequest.builder()
                    .movieId(movieId)
                    .userId(userId)
                    .value(value)
                    .build();
            client.upsertRating(ratingRequest);

          ReviewResponse reviewResponse = reviewService.getReviewByUserAndMovie(userId, movieId);

          if (reviewResponse != null) {
              reviewService.upsertReview(userId, movieId, value, reviewResponse.getTitle(), reviewResponse.getContent());
          }

        } catch (FeignException e) {
            log.error("Failed to upsert rating for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            //todo: throw custom exception if needed
        }
    }

    public Integer getRatingByUserAndMovie(UUID userId, UUID movieId) {
        try {
            Rating rating = client.getRatingByUserAndMovie(userId, movieId).getBody();
            return rating != null ? rating.getValue() : null;
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch rating for user {} and movie {}: {}", userId, movieId, e.getMessage());
            return null;
            //todo: or throw custom exception if needed
        }
    }

    public void deleteRating(UUID userId, UUID movieId) {
        try {
            client.deleteRating(userId, movieId);

            ReviewResponse reviewResponse = reviewService.getReviewByUserAndMovie(userId, movieId);

            if (reviewResponse != null) {
                reviewService.upsertReview(userId, movieId, null, reviewResponse.getTitle(), reviewResponse.getContent());
            }
        } catch (FeignException.NotFound e) {
            log.warn("Rating not found for deletion: user {}, movie {}", userId, movieId);
        } catch (FeignException e) {
            log.error("Failed to delete rating for user {} and movie {}: {}", userId, movieId, e.getMessage());
        }
        //todo: throw custom exceptions if needed
    }

    public Double getAverageRatingForAMovie(UUID movieId) {
        try {
            MovieRatingStatsResponse movieRatingStats = client.getMovieRatingStats(movieId).getBody();
            return movieRatingStats != null ? movieRatingStats.getAverageRating() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch average rating for movie {}: {}", movieId, e.getMessage());
            return null;
        }
        //todo: throw custom exceptions if needed
    }

    public Integer getTotalRatingsCountForAMovie(UUID movieId) {
        try {
            MovieRatingStatsResponse movieRatingStats = client.getMovieRatingStats(movieId).getBody();
            return movieRatingStats != null ? movieRatingStats.getTotalRatings() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch total ratings for movie {}: {}", movieId, e.getMessage());
            return null;
        }
        //todo: throw custom exceptions if needed
    }

    public Integer getTotalMoviesRatedByUser(UUID userId){
        try {
            UserRatingStatsResponse ratingStats = client.getUserRatingStats(userId).getBody();
            return ratingStats != null ? ratingStats.getRatedMovies() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch total ratings for user {}: {}", userId, e.getMessage());
            return null;
        }
        //todo: throw custom exceptions if needed
    }
}
