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
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RatingServiceUTest {

    @Mock
    private RatingClient client;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RatingService ratingService;

    @Test
    void whenUpsertRating_andRatingClientSucceeds_thenSaveRatingAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        int rating = 8;

        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(null);

        ratingService.upsertRating(userId, movieId, rating);

        ArgumentCaptor<RatingRequest> requestCaptor = ArgumentCaptor.forClass(RatingRequest.class);
        verify(client).upsertRating(requestCaptor.capture());

        RatingRequest capturedRequest = requestCaptor.getValue();
        assertEquals(userId, capturedRequest.getUserId());
        assertEquals(movieId, capturedRequest.getMovieId());
        assertEquals(rating, capturedRequest.getRating());

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.RATED, capturedEvent.getType());
        assertFalse(capturedEvent.isRemoved());
        assertEquals(rating, capturedEvent.getRating());
    }

    @Test
    void whenUpsertRating_andUserHasExistingReview_thenUpdateReviewWithNewRatingValue() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        int rating = 9;
        ReviewResponse existingReview = ReviewResponse.builder()
                .title("Title")
                .content("Content")
                .build();

        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(existingReview);

        ratingService.upsertRating(userId, movieId, rating);

        verify(reviewService).upsertReview(userId, movieId, rating, "Title", "Content");
        verify(eventPublisher).publishEvent(any(ActivityDtoEvent.class));
    }

    @Test
    void whenUpsertRating_andRatingClientThrowsFeignException_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(createFeignException(500)).when(client).upsertRating(any(RatingRequest.class));

        assertThrows(RatingMicroserviceUnavailableException.class,
                () -> ratingService.upsertRating(userId, movieId, 7));

        verify(eventPublisher, never()).publishEvent(any());
    }

    private FeignException createFeignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "", Map.of(), null, new RequestTemplate());
        return FeignException.errorStatus("test", feign.Response.builder()
                .status(status)
                .reason("Error")
                .request(request)
                .headers(Map.of())
                .build());
    }

    @Test
    void whenGetRatingByUserAndMovie_andRatingExists_thenReturnRatingValue() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Rating rating = Rating.builder().rating(8).build();

        when(client.getRatingByUserAndMovie(userId, movieId)).thenReturn(ResponseEntity.ok(rating));

        Integer result = ratingService.getRatingByUserAndMovie(userId, movieId);

        assertEquals(8, result);
    }

    @Test
    void whenGetRatingByUserAndMovie_andRatingIsNull_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(client.getRatingByUserAndMovie(userId, movieId)).thenReturn(ResponseEntity.ok(null));

        Integer result = ratingService.getRatingByUserAndMovie(userId, movieId);

        assertNull(result);
    }

    @Test
    void whenGetRatingByUserAndMovie_andRatingNotFoundInMicroservice_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(client.getRatingByUserAndMovie(userId, movieId))
                .thenThrow(FeignException.NotFound.class);

        Integer result = ratingService.getRatingByUserAndMovie(userId, movieId);

        assertNull(result);
    }

    @Test
    void whenGetRatingByUserAndMovie_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(client.getRatingByUserAndMovie(userId, movieId)).thenThrow(createFeignException(500));

        Integer result = ratingService.getRatingByUserAndMovie(userId, movieId);

        assertNull(result);
    }

    @Test
    void whenDeleteRating_andRatingExists_thenDeleteAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(null);

        ratingService.deleteRating(userId, movieId);

        verify(client).deleteRating(userId, movieId);

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.RATED, capturedEvent.getType());
        assertTrue(capturedEvent.isRemoved());
        assertNull(capturedEvent.getRating());
    }

    @Test
    void whenDeleteRating_andUserHasExistingReview_thenUpdateReviewToRemoveRatingValue() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        ReviewResponse existingReview = ReviewResponse.builder()
                .title("Title")
                .content("Content")
                .build();

        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(existingReview);

        ratingService.deleteRating(userId, movieId);

        verify(reviewService).upsertReview(userId, movieId, null, "Title", "Content");
    }

    @Test
    void whenDeleteRating_andRatingNotFoundInMicroservice_thenReturnWithoutPublishingEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(FeignException.NotFound.class).when(client).deleteRating(userId, movieId);

        ratingService.deleteRating(userId, movieId);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenDeleteRating_andMicroserviceThrowsGenericFeignException_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(createFeignException(500)).when(client).deleteRating(userId, movieId);

        assertThrows(RatingMicroserviceUnavailableException.class,
                () -> ratingService.deleteRating(userId, movieId));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenGetAverageRatingForAMovie_andStatsExist_thenReturnAverageRating() {
        UUID movieId = UUID.randomUUID();
        MovieRatingStatsResponse stats = MovieRatingStatsResponse.builder()
                .averageRating(7.5)
                .totalRatings(100)
                .build();

        when(client.getMovieRatingStats(movieId)).thenReturn(ResponseEntity.ok(stats));

        Double result = ratingService.getAverageRatingForAMovie(movieId);

        assertEquals(7.5, result);
    }

    @Test
    void whenGetAverageRatingForAMovie_andStatsAreNull_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieRatingStats(movieId)).thenReturn(ResponseEntity.ok(null));

        Double result = ratingService.getAverageRatingForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetAverageRatingForAMovie_andMovieNotFoundInMicroservice_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieRatingStats(movieId)).thenThrow(FeignException.NotFound.class);

        Double result = ratingService.getAverageRatingForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetAverageRatingForAMovie_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieRatingStats(movieId)).thenThrow(createFeignException(503));

        Double result = ratingService.getAverageRatingForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetTotalRatingsCountForAMovie_andStatsExist_thenReturnTotalRatingsCount() {
        UUID movieId = UUID.randomUUID();
        MovieRatingStatsResponse stats = MovieRatingStatsResponse.builder()
                .averageRating(8.0)
                .totalRatings(250)
                .build();

        when(client.getMovieRatingStats(movieId)).thenReturn(ResponseEntity.ok(stats));

        Integer result = ratingService.getTotalRatingsCountForAMovie(movieId);

        assertEquals(250, result);
    }

    @Test
    void whenGetTotalRatingsCountForAMovie_andStatsAreNull_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieRatingStats(movieId)).thenReturn(ResponseEntity.ok(null));

        Integer result = ratingService.getTotalRatingsCountForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetTotalRatingsCountForAMovie_andMovieNotFoundInMicroservice_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieRatingStats(movieId)).thenThrow(FeignException.NotFound.class);

        Integer result = ratingService.getTotalRatingsCountForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetTotalMoviesRatedByUser_andStatsExist_thenReturnRatedMoviesCount() {
        UUID userId = UUID.randomUUID();
        UserRatingStatsResponse stats = UserRatingStatsResponse.builder()
                .ratedMovies(45)
                .build();

        when(client.getUserRatingStats(userId)).thenReturn(ResponseEntity.ok(stats));

        Integer result = ratingService.getTotalMoviesRatedByUser(userId);

        assertEquals(45, result);
    }

    @Test
    void whenGetTotalMoviesRatedByUser_andStatsAreNull_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getUserRatingStats(userId)).thenReturn(ResponseEntity.ok(null));

        Integer result = ratingService.getTotalMoviesRatedByUser(userId);

        assertNull(result);
    }


    @Test
    void whenGetTotalMoviesRatedByUser_andUserNotFoundInMicroservice_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getUserRatingStats(userId)).thenThrow(FeignException.NotFound.class);

        Integer result = ratingService.getTotalMoviesRatedByUser(userId);

        assertNull(result);
    }

    @Test
    void whenGetTotalMoviesRatedByUser_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getUserRatingStats(userId)).thenThrow(createFeignException(500));

        Integer result = ratingService.getTotalMoviesRatedByUser(userId);

        assertNull(result);
    }

    @Test
    void whenGetLatestRatingsByUserId_andRatingsExist_thenReturnListOfRatings() {
        UUID userId = UUID.randomUUID();
        List<Rating> ratings = List.of(
                Rating.builder().movieId(UUID.randomUUID()).rating(8).build(),
                Rating.builder().movieId(UUID.randomUUID()).rating(9).build()
        );

        when(client.getLatestRatingsByUser(userId)).thenReturn(ResponseEntity.ok(ratings));

        List<Rating> result = ratingService.getLatestRatingsByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void whenGetLatestRatingsByUserId_andUserNotFoundInMicroservice_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getLatestRatingsByUser(userId)).thenThrow(FeignException.NotFound.class);

        List<Rating> result = ratingService.getLatestRatingsByUserId(userId);

        assertNull(result);
    }

    @Test
    void whenGetLatestRatingsByUserId_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getLatestRatingsByUser(userId)).thenThrow(createFeignException(503));

        List<Rating> result = ratingService.getLatestRatingsByUserId(userId);

        assertNull(result);
    }

    @Test
    void whenExtractMovieIdsFromRatings_andRatingsContainMultipleMovies_thenReturnSetOfUniqueMovieIds() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        UUID movieId3 = UUID.randomUUID();
        List<Rating> ratings = List.of(
                Rating.builder().movieId(movieId1).rating(7).build(),
                Rating.builder().movieId(movieId2).rating(8).build(),
                Rating.builder().movieId(movieId3).rating(9).build()
        );

        Set<UUID> result = ratingService.extractMovieIdsFromRatings(ratings);

        assertEquals(3, result.size());
        assertTrue(result.contains(movieId1));
        assertTrue(result.contains(movieId2));
        assertTrue(result.contains(movieId3));
    }

    @Test
    void whenExtractMovieIdsFromRatings_andRatingsContainDuplicateMovieIds_thenReturnSetWithoutDuplicates() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        List<Rating> ratings = List.of(
                Rating.builder().movieId(movieId1).rating(7).build(),
                Rating.builder().movieId(movieId1).rating(8).build(),
                Rating.builder().movieId(movieId2).rating(9).build()
        );

        Set<UUID> result = ratingService.extractMovieIdsFromRatings(ratings);

        assertEquals(2, result.size());
    }

    @Test
    void whenExtractMovieIdsFromRatings_andRatingsListIsNull_thenReturnEmptySet() {
        Set<UUID> result = ratingService.extractMovieIdsFromRatings(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractMovieIdsFromRatings_andRatingsListIsEmpty_thenReturnEmptySet() {
        Set<UUID> result = ratingService.extractMovieIdsFromRatings(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractMovieIdsFromRatings_andSomeRatingsHaveNullMovieIds_thenFilterOutNullsAndReturnValidIds() {
        UUID movieId1 = UUID.randomUUID();
        List<Rating> ratings = List.of(
                Rating.builder().movieId(movieId1).rating(7).build(),
                Rating.builder().movieId(null).rating(8).build()
        );

        Set<UUID> result = ratingService.extractMovieIdsFromRatings(ratings);

        assertEquals(1, result.size());
        assertTrue(result.contains(movieId1));
    }
}
