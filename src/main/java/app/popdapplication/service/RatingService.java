package app.popdapplication.service;

import app.popdapplication.client.RatingClient;
import app.popdapplication.client.RatingDto.MovieRatingStatsResponse;
import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.RatingDto.RatingRequest;
import app.popdapplication.client.RatingDto.UserRatingStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RatingService {

    private final RatingClient client;
    private final ReviewService reviewService;
    private final UserService userService;
    private final MovieService movieService;

    @Autowired
    public RatingService(RatingClient client, ReviewService reviewService, UserService userService, MovieService movieService) {
        this.client = client;
        this.reviewService = reviewService;
        this.userService = userService;
        this.movieService = movieService;
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

    public Integer getTotalMoviesRatedByUser(UUID userId) {
        try {
            UserRatingStatsResponse ratingStats = client.getUserRatingStats(userId).getBody();
            return ratingStats != null ? ratingStats.getRatedMovies() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch total ratings for user with id {}: {}", userId, e.getMessage());
            return null;
        }
        //todo: throw custom exceptions if needed
    }

    public List<Rating> getLatestRatingsByUserId(UUID userId) {
        try {
            return client.getLatestRatingsByUser(userId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        }catch (FeignException e) {
            log.error("Failed to fetch latest ratings for user with id {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public Map<UUID, String> getMovieNamesForRatings(List<Rating> ratings) {

        Map<UUID, String> result = new HashMap<>();

        if (ratings == null || ratings.isEmpty()) {
            return result;
        }

        for (Rating rating : ratings) {
            UUID movieId = rating.getMovieId();

            if (movieId == null || result.containsKey(movieId)) {
                continue; // skip nulls and already processed users
            }

            try {
                Movie movie = movieService.findById(movieId);
                if (movie != null) {
                    result.put(movieId, movie.getTitle());
                }
            } catch (Exception e) {
                log.warn("Movie not found for movieId {}: {}", movieId, e.getMessage());
            }
        }

        return result;
    }

}
