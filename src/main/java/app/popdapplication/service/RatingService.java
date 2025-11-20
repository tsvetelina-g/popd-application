package app.popdapplication.service;

import app.popdapplication.client.RatingClient;
import app.popdapplication.client.RatingDto.MovieRatingStatsResponse;
import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.RatingDto.RatingRequest;
import app.popdapplication.client.RatingDto.UserRatingStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.event.ActivityDtoEvent;
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

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.RATED)
                .removed(false)
                .createdOn(LocalDateTime.now())
                .rating(value)
                .build();

        eventPublisher.publishEvent(event);
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

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.RATED)
                .removed(true)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
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
