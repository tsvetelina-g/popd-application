package app.popdapplication.service;

import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.RatingDto.UserRatingStatsResponse;
import app.popdapplication.client.ReviewClient;
import app.popdapplication.client.ReviewDto.MovieReviewStatsResponse;
import app.popdapplication.client.ReviewDto.ReviewRequest;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.client.ReviewDto.UserReviewsStatsResponse;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ReviewService {

    private final ReviewClient client;
    private final UserService userService;
    private final MovieService movieService;

    @Autowired
    public ReviewService(ReviewClient client, UserService userService, MovieService movieService) {
        this.client = client;
        this.userService = userService;
        this.movieService = movieService;
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
            log.error("Failed to upsert review for user {} and movie {}: {}", userId, movieId, e.getMessage());
            //todo: throw custom exception if needed
        }
    }

    public ReviewResponse getReviewByUserAndMovie(UUID userId, UUID movieId) {

        try {
            return client.getReviewByUserAndMovie(userId, movieId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch review for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
            return null;
            //todo: or throw custom exception if needed
        }

    }

    public void deleteReview(UUID userId, UUID movieId) {
        try {
            client.deleteReview(userId, movieId);
        } catch (FeignException.NotFound e) {
            log.warn("Review not found for deletion: user {}, movie {}", userId, movieId);
        } catch (FeignException e) {
            log.error("Failed to delete review for user with id {} and movie with id {}: {}", userId, movieId, e.getMessage());
        }
    }

    public List<ReviewResponse> getLatestFiveReviewsForAMovie(UUID movieId) {
        try {
            return client.getLatestFiveReviewsForAMovie(movieId).getBody();
        } catch (FeignException.NotFound e) {
            return null;
        } catch (FeignException e) {
            log.error("Failed to fetch reviews movie with id {}: {}", movieId, e.getMessage());
            return null;
            //todo: or throw custom exception if needed
        }
    }

    public Map<UUID, String> getUsernamesForReviews(List<ReviewResponse> reviews) {

        Map<UUID, String> result = new HashMap<>();

        if (reviews == null || reviews.isEmpty()) {
            return result;
        }

        for (ReviewResponse review : reviews) {
            UUID userId = review.getUserId();

            if (userId == null || result.containsKey(userId)) {
                continue; // skip nulls and already processed users
            }

            try {
                User user = userService.findById(userId);
                if (user != null) {
                    result.put(userId, user.getUsername());
                }
            } catch (Exception e) {
                log.warn("User not found for userId {}: {}", userId, e.getMessage());
            }
        }

        return result;
    }

    public Page<ReviewResponse> getReviewsForMovie(UUID movieId, Pageable pageable) {
        try {
            // Call microservice to get paginated reviews
            ResponseEntity<Page<ReviewResponse>> response = client.getReviewsForMovie(movieId, pageable.getPageNumber(), pageable.getPageSize());

            if (response.getBody() != null) {
                return response.getBody();
            }

            // Return empty page if no reviews
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (FeignException.NotFound e) {
            return new PageImpl<>(List.of(), pageable, 0);
        } catch (FeignException e) {
            log.error("Failed to fetch reviews for movie with id {}: {}", movieId, e.getMessage());
            return new PageImpl<>(List.of(), pageable, 0);
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
        //todo: throw custom exceptions if needed
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

    public Map<UUID, String> getMovieNamesForReviews(List<ReviewResponse> reviews) {

        Map<UUID, String> result = new HashMap<>();

        if (reviews == null || reviews.isEmpty()) {
            return result;
        }

        for (ReviewResponse review : reviews) {
            UUID movieId = review.getMovieId();

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
