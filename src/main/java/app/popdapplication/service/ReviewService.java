package app.popdapplication.service;

import app.popdapplication.client.ReviewClient;
import app.popdapplication.client.ReviewDto.MovieReviewStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewRequest;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.client.ReviewDto.UserReviewsStatsResponse;
import app.popdapplication.event.ActivityDtoEvent;
import app.popdapplication.exception.ReviewMicroserviceUnavailableException;
import app.popdapplication.model.enums.ActivityType;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReviewService {

    private final ReviewClient client;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ReviewService(ReviewClient client, ApplicationEventPublisher eventPublisher) {
        this.client = client;
        this.eventPublisher = eventPublisher;
    }

    public void upsertReview(UUID userId, UUID movieId, Integer rating, String title, String content) {

        try {
            ReviewRequest reviewRequest = ReviewRequest.builder()
                    .userId(userId)
                    .movieId(movieId)
                    .rating(rating)
                    .title(title)
                    .content(content)
                    .build();

            client.upsertReview(reviewRequest);
        } catch (FeignException e) {
            log.error("Failed to upsert review for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            throw new ReviewMicroserviceUnavailableException("Unable to save review. The review service is currently unavailable. Please try again later.");
        }

        log.info("Review upserted successfully: user id {}, movie id {}", userId, movieId);

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.REVIEWED)
                .removed(false)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    public ReviewResponse getReviewByUserAndMovie(UUID userId, UUID movieId) {

        try {
            return client.getReviewByUserAndMovie(userId, movieId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch review for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            return null;
        }

    }

    public void deleteReview(UUID userId, UUID movieId) {
        try {
            client.deleteReview(userId, movieId);
        } catch (FeignException.NotFound e) {
            log.warn("Review not found for deletion: user with id {}, movie with id {}", userId, movieId);
        } catch (FeignException e) {
            log.error("Failed to delete review for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            throw new ReviewMicroserviceUnavailableException("Unable to delete review. The review service is currently unavailable. Please try again later.");
        }

        log.info("Review deleted successfully: user id {}, movie id {}", userId, movieId);

        ActivityDtoEvent event = ActivityDtoEvent.builder()
                .userId(userId)
                .movieId(movieId)
                .type(ActivityType.REVIEWED)
                .removed(true)
                .createdOn(LocalDateTime.now())
                .rating(null)
                .build();

        eventPublisher.publishEvent(event);
    }

    public List<ReviewResponse> getLatestFiveReviewsForAMovie(UUID movieId) {
        try {
            return client.getLatestFiveReviewsForAMovie(movieId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch latest reviews for movie with id {}: {}", movieId, e.getMessage());
            return null;
        }
    }

    public Page<ReviewResponse> getReviewsForMovie(UUID movieId, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 5;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return getReviewsForMovie(movieId, pageable);
    }

    private Page<ReviewResponse> getReviewsForMovie(UUID movieId, Pageable pageable) {
        try {
            ResponseEntity<Page<ReviewResponse>> response = client.getReviewsForMovie(movieId, pageable.getPageNumber(), pageable.getPageSize());

            if (response.getBody() != null) {
                return response.getBody();
            }

            return new PageImpl<>(List.of(), pageable, 0);
        } catch (FeignException.NotFound e) {
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (FeignException e) {
            log.error("Failed to fetch reviews for movie with id {}: {}", movieId, e.getMessage());
            throw new ReviewMicroserviceUnavailableException("Unable to fetch reviews. The review service is currently unavailable. Please try again later.");
        }
    }

    public Integer getMovieReviewsCount(UUID movieId) {
        try {
            MovieReviewStatsResponse movieReviewStats = client.getMovieReviewsCount(movieId).getBody();
            return movieReviewStats != null ? movieReviewStats.getTotalReviews() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch total reviews for movie with id {}: {}", movieId, e.getMessage());
            return null;
        }
    }

    public Integer getTotalMoviesReviewedByUser(UUID userId){
        try {
            UserReviewsStatsResponse reviewStats = client.getUserReviewsStats(userId).getBody();
            return reviewStats != null ? reviewStats.getReviewedMovies() : null;
        } catch (FeignException e) {
            log.error("Failed to fetch total reviews for user with id {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<ReviewResponse> getLatestReviewsByUserId(UUID userId) {
        try {
            return client.getLatestReviewsByUser(userId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        }catch (FeignException e) {
            log.error("Failed to fetch latest reviews for user with id {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public Set<UUID> extractUserIdsFromReviews(List<ReviewResponse> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return Set.of();
        }
        return reviews.stream()
                .map(ReviewResponse::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<UUID> extractMovieIdsFromReviews(List<ReviewResponse> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return Set.of();
        }
        return reviews.stream()
                .map(ReviewResponse::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
