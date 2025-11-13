package app.popdapplication.service;

import app.popdapplication.client.RatingClient;
import app.popdapplication.client.dto.Rating;
import app.popdapplication.client.dto.RatingRequest;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class RatingService {

    private final RatingClient client;

    public RatingService(RatingClient client) {
        this.client = client;
    }

    public void upsertRating(UUID userId, UUID movieId, int value) {

        try {
            RatingRequest ratingRequest = RatingRequest.builder()
                    .movieId(movieId)
                    .userId(userId)
                    .value(value)
                    .build();
            client.upsertRating(ratingRequest);
        } catch (FeignException e) {
            log.error("Failed to upsert rating for user {} and movie {}: {}", userId, movieId, e.getMessage());
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
        } catch (FeignException.NotFound e) {
            log.warn("Rating not found for deletion: user {}, movie {}", userId, movieId);
        } catch (FeignException e) {
            log.error("Failed to delete rating for user {} and movie {}: {}", userId, movieId, e.getMessage());
        }
        //todo: throw custom exceptions if needed
    }
}
