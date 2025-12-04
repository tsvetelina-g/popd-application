package app.popdapplication.service;

import app.popdapplication.client.RatingClient;
import app.popdapplication.client.RatingDto.MovieRatingStatsResponse;
import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.RatingDto.RatingRequest;
import app.popdapplication.client.RatingDto.UserRatingStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.RatingMicroserviceUnavailableException;
import app.popdapplication.model.enums.ActivityType;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RatingService {

    private final RatingClient client;
    private final ReviewService reviewService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RatingService(RatingClient client, ReviewService reviewService, ApplicationEventPublisher eventPublisher) {
        this.client = client;
        this.reviewService = reviewService;
        this.eventPublisher = eventPublisher;
    }

    public void upsertRating(UUID userId, UUID movieId, int rating) {
        try {
            RatingRequest ratingRequest = RatingRequest.builder()
                    .movieId(movieId)
                    .userId(userId)
                    .rating(rating)
                    .build();
            client.upsertRating(ratingRequest);

            ReviewResponse reviewResponse = reviewService.getReviewByUserAndMovie(userId, movieId);

            if (reviewResponse != null) {
                reviewService.upsertReview(userId, movieId, rating, reviewResponse.getTitle(), reviewResponse.getContent());
            }

            log.info("Rating upserted successfully: user id {}, movie id {}, rating {}", userId, movieId, rating);

            ActivityDtoEvent event = ActivityDtoEvent.builder()
                    .userId(userId)
                    .movieId(movieId)
                    .type(ActivityType.RATED)
                    .removed(false)
                    .createdOn(LocalDateTime.now())
                    .rating(rating)
                    .build();

            eventPublisher.publishEvent(event);
        } catch (FeignException e) {
            log.error("Failed to upsert rating for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            throw new RatingMicroserviceUnavailableException("Unable to save rating. The rating service is currently unavailable. Please try again later.");
        }
    }

    public Integer getRatingByUserAndMovie(UUID userId, UUID movieId) {
        try {
            Rating rating = client.getRatingByUserAndMovie(userId, movieId).getBody();
            return rating != null ? rating.getRating() : null;
        } catch (FeignException.NotFound e) {
            log.info("Rating not found for user with id {} and movie with id {}", userId, movieId);
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch rating for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            return null;
        }
    }

    public void deleteRating(UUID userId, UUID movieId) {
        try {
            client.deleteRating(userId, movieId);

            ReviewResponse reviewResponse = reviewService.getReviewByUserAndMovie(userId, movieId);

            if (reviewResponse != null) {
                reviewService.upsertReview(userId, movieId, null, reviewResponse.getTitle(), reviewResponse.getContent());
            }

            log.info("Rating deleted successfully: user id {}, movie id {}", userId, movieId);

            ActivityDtoEvent event = ActivityDtoEvent.builder()
                    .userId(userId)
                    .movieId(movieId)
                    .type(ActivityType.RATED)
                    .removed(true)
                    .createdOn(LocalDateTime.now())
                    .rating(null)
                    .build();

            eventPublisher.publishEvent(event);
        } catch (FeignException.NotFound e) {
            log.info("Rating not found for deletion: user with id {}, movie with id {}", userId, movieId);
            return;
        } catch (FeignException e) {
            log.error("Failed to delete rating for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            throw new RatingMicroserviceUnavailableException("Unable to delete rating. The rating service is currently unavailable. Please try again later.");
        }
    }

    public Double getAverageRatingForAMovie(UUID movieId) {
        try {
            MovieRatingStatsResponse movieRatingStats = client.getMovieRatingStats(movieId).getBody();
            return movieRatingStats != null ? movieRatingStats.getAverageRating() : null;
        } catch (FeignException.NotFound e) {
            log.info("Rating statistics not found for movie with id {}", movieId);
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch average rating for movie with id {}: {}", movieId, e.getMessage());
            return null;
        }
    }

    public Integer getTotalRatingsCountForAMovie(UUID movieId) {
        try {
            MovieRatingStatsResponse movieRatingStats = client.getMovieRatingStats(movieId).getBody();
            return movieRatingStats != null ? movieRatingStats.getTotalRatings() : null;
        } catch (FeignException.NotFound e) {
            log.info("Rating statistics not found for movie with id {}", movieId);
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch total ratings for movie with id {}: {}", movieId, e.getMessage());
            return null;
        }
    }

    public Integer getTotalMoviesRatedByUser(UUID userId) {
        try {
            UserRatingStatsResponse ratingStats = client.getUserRatingStats(userId).getBody();
            return ratingStats != null ? ratingStats.getRatedMovies() : null;
        } catch (FeignException.NotFound e) {
            log.info("Rating statistics not found for user with id {}", userId);
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch total ratings for user with id {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<Rating> getLatestRatingsByUserId(UUID userId) {
        try {
            return client.getLatestRatingsByUser(userId).getBody();
        } catch (FeignException.NotFound e) {
            log.info("Latest ratings not found for user with id {}", userId);
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch latest ratings for user with id {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public Set<UUID> extractMovieIdsFromRatings(List<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return Set.of();
        }
        return ratings.stream()
                .map(Rating::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
