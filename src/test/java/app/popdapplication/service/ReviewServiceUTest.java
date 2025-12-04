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
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceUTest {

    @Mock
    private ReviewClient client;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void whenUpsertReview_andClientSucceeds_thenSaveReviewAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Integer rating = 8;
        String title = "Great Movie!";
        String content = "This movie was fantastic, highly recommend it.";

        reviewService.upsertReview(userId, movieId, rating, title, content);

        ArgumentCaptor<ReviewRequest> requestCaptor = ArgumentCaptor.forClass(ReviewRequest.class);
        verify(client).upsertReview(requestCaptor.capture());

        ReviewRequest capturedRequest = requestCaptor.getValue();
        assertEquals(userId, capturedRequest.getUserId());
        assertEquals(movieId, capturedRequest.getMovieId());
        assertEquals(rating, capturedRequest.getRating());
        assertEquals(title, capturedRequest.getTitle());
        assertEquals(content, capturedRequest.getContent());

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.REVIEWED, capturedEvent.getType());
        assertFalse(capturedEvent.isRemoved());
    }

    @Test
    void whenUpsertReview_andRatingIsNull_thenSaveReviewWithoutRating() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        reviewService.upsertReview(userId, movieId, null, "Title", "Content");

        ArgumentCaptor<ReviewRequest> requestCaptor = ArgumentCaptor.forClass(ReviewRequest.class);
        verify(client).upsertReview(requestCaptor.capture());

        assertNull(requestCaptor.getValue().getRating());
        verify(eventPublisher).publishEvent(any(ActivityDtoEvent.class));
    }

    @Test
    void whenUpsertReview_andClientThrowsFeignException_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(createFeignException(500)).when(client).upsertReview(any(ReviewRequest.class));

        assertThrows(ReviewMicroserviceUnavailableException.class,
                () -> reviewService.upsertReview(userId, movieId, 7, "Title", "Content"));

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
    void whenGetReviewByUserAndMovie_andReviewExists_thenReturnReviewResponse() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        ReviewResponse review = ReviewResponse.builder()
                .userId(userId)
                .movieId(movieId)
                .title("Title")
                .content("Content")
                .build();

        when(client.getReviewByUserAndMovie(userId, movieId)).thenReturn(ResponseEntity.ok(review));

        ReviewResponse result = reviewService.getReviewByUserAndMovie(userId, movieId);

        assertNotNull(result);
        assertEquals("Title", result.getTitle());
        assertEquals("Content", result.getContent());
    }

    @Test
    void whenGetReviewByUserAndMovie_andReviewNotFoundInMicroservice_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(client.getReviewByUserAndMovie(userId, movieId)).thenThrow(FeignException.NotFound.class);

        ReviewResponse result = reviewService.getReviewByUserAndMovie(userId, movieId);

        assertNull(result);
    }

    @Test
    void whenGetReviewByUserAndMovie_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        when(client.getReviewByUserAndMovie(userId, movieId)).thenThrow(createFeignException(500));

        ReviewResponse result = reviewService.getReviewByUserAndMovie(userId, movieId);

        assertNull(result);
    }

    @Test
    void whenDeleteReview_andReviewExists_thenDeleteAndPublishActivityEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        reviewService.deleteReview(userId, movieId);

        verify(client).deleteReview(userId, movieId);

        ArgumentCaptor<ActivityDtoEvent> eventCaptor = ArgumentCaptor.forClass(ActivityDtoEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        ActivityDtoEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.getUserId());
        assertEquals(movieId, capturedEvent.getMovieId());
        assertEquals(ActivityType.REVIEWED, capturedEvent.getType());
        assertTrue(capturedEvent.isRemoved());
    }

    @Test
    void whenDeleteReview_andReviewNotFoundInMicroservice_thenLogWarningAndDoNotPublishEvent() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(FeignException.NotFound.class).when(client).deleteReview(userId, movieId);

        reviewService.deleteReview(userId, movieId);

        verify(eventPublisher, never()).publishEvent(any(ActivityDtoEvent.class));
    }

    @Test
    void whenDeleteReview_andMicroserviceThrowsGenericFeignException_thenThrowReviewMicroserviceUnavailableException() {
        UUID userId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();

        doThrow(createFeignException(503)).when(client).deleteReview(userId, movieId);

        assertThrows(ReviewMicroserviceUnavailableException.class,
                () -> reviewService.deleteReview(userId, movieId));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void whenGetLatestFiveReviewsForAMovie_andReviewsExist_thenReturnListOfReviews() {
        UUID movieId = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().title("Review 1").build(),
                ReviewResponse.builder().title("Review 2").build(),
                ReviewResponse.builder().title("Review 3").build()
        );

        when(client.getLatestFiveReviewsForAMovie(movieId)).thenReturn(ResponseEntity.ok(reviews));

        List<ReviewResponse> result = reviewService.getLatestFiveReviewsForAMovie(movieId);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void whenGetLatestFiveReviewsForAMovie_andMovieNotFoundInMicroservice_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getLatestFiveReviewsForAMovie(movieId)).thenThrow(FeignException.NotFound.class);

        List<ReviewResponse> result = reviewService.getLatestFiveReviewsForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetLatestFiveReviewsForAMovie_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getLatestFiveReviewsForAMovie(movieId)).thenThrow(createFeignException(500));

        List<ReviewResponse> result = reviewService.getLatestFiveReviewsForAMovie(movieId);

        assertNull(result);
    }

    @Test
    void whenGetReviewsForMovie_andReviewsExist_thenReturnPageOfReviews() {
        UUID movieId = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().title("Review 1").build(),
                ReviewResponse.builder().title("Review 2").build()
        );
        Page<ReviewResponse> page = new PageImpl<>(reviews);

        when(client.getReviewsForMovie(eq(movieId), eq(0), eq(5))).thenReturn(ResponseEntity.ok(page));

        Page<ReviewResponse> result = reviewService.getReviewsForMovie(movieId, 0, 5);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void whenGetReviewsForMovie_andNegativePageNumber_thenDefaultToPageZero() {
        UUID movieId = UUID.randomUUID();
        Page<ReviewResponse> emptyPage = new PageImpl<>(List.of());

        when(client.getReviewsForMovie(eq(movieId), eq(0), anyInt())).thenReturn(ResponseEntity.ok(emptyPage));

        reviewService.getReviewsForMovie(movieId, -5, 5);

        verify(client).getReviewsForMovie(movieId, 0, 5);
    }

    @Test
    void whenGetReviewsForMovie_andSizeIsZero_thenDefaultToSizeFive() {
        UUID movieId = UUID.randomUUID();
        Page<ReviewResponse> emptyPage = new PageImpl<>(List.of());

        when(client.getReviewsForMovie(eq(movieId), anyInt(), eq(5))).thenReturn(ResponseEntity.ok(emptyPage));

        reviewService.getReviewsForMovie(movieId, 0, 0);

        verify(client).getReviewsForMovie(movieId, 0, 5);
    }

    @Test
    void whenGetReviewsForMovie_andSizeGreaterThan50_thenDefaultToSizeFive() {
        UUID movieId = UUID.randomUUID();
        Page<ReviewResponse> emptyPage = new PageImpl<>(List.of());

        when(client.getReviewsForMovie(eq(movieId), anyInt(), eq(5))).thenReturn(ResponseEntity.ok(emptyPage));

        reviewService.getReviewsForMovie(movieId, 0, 100);

        verify(client).getReviewsForMovie(movieId, 0, 5);
    }

    @Test
    void whenGetReviewsForMovie_andResponseBodyIsNull_thenReturnEmptyPage() {
        UUID movieId = UUID.randomUUID();

        when(client.getReviewsForMovie(eq(movieId), anyInt(), anyInt())).thenReturn(ResponseEntity.ok(null));

        Page<ReviewResponse> result = reviewService.getReviewsForMovie(movieId, 0, 5);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void whenGetReviewsForMovie_andMovieNotFoundInMicroservice_thenReturnEmptyPage() {
        UUID movieId = UUID.randomUUID();

        when(client.getReviewsForMovie(eq(movieId), anyInt(), anyInt())).thenThrow(FeignException.NotFound.class);

        Page<ReviewResponse> result = reviewService.getReviewsForMovie(movieId, 0, 5);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void whenGetReviewsForMovie_andMicroserviceThrowsGenericFeignException_thenThrowReviewMicroserviceUnavailableException() {
        UUID movieId = UUID.randomUUID();

        when(client.getReviewsForMovie(eq(movieId), anyInt(), anyInt())).thenThrow(createFeignException(503));

        assertThrows(ReviewMicroserviceUnavailableException.class,
                () -> reviewService.getReviewsForMovie(movieId, 0, 5));
    }

    @Test
    void whenGetMovieReviewsCount_andStatsExist_thenReturnTotalReviewsCount() {
        UUID movieId = UUID.randomUUID();
        MovieReviewStatsResponse stats = MovieReviewStatsResponse.builder()
                .totalReviews(42)
                .build();

        when(client.getMovieReviewsCount(movieId)).thenReturn(ResponseEntity.ok(stats));

        Integer result = reviewService.getMovieReviewsCount(movieId);

        assertEquals(42, result);
    }

    @Test
    void whenGetMovieReviewsCount_andStatsAreNull_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieReviewsCount(movieId)).thenReturn(ResponseEntity.ok(null));

        Integer result = reviewService.getMovieReviewsCount(movieId);

        assertNull(result);
    }

    @Test
    void whenGetMovieReviewsCount_andMicroserviceThrowsFeignException_thenReturnNull() {
        UUID movieId = UUID.randomUUID();

        when(client.getMovieReviewsCount(movieId)).thenThrow(createFeignException(500));

        Integer result = reviewService.getMovieReviewsCount(movieId);

        assertNull(result);
    }

    @Test
    void whenGetTotalMoviesReviewedByUser_andStatsExist_thenReturnReviewedMoviesCount() {
        UUID userId = UUID.randomUUID();
        UserReviewsStatsResponse stats = UserReviewsStatsResponse.builder()
                .reviewedMovies(25)
                .build();

        when(client.getUserReviewsStats(userId)).thenReturn(ResponseEntity.ok(stats));

        Integer result = reviewService.getTotalMoviesReviewedByUser(userId);

        assertEquals(25, result);
    }

    @Test
    void whenGetTotalMoviesReviewedByUser_andStatsAreNull_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getUserReviewsStats(userId)).thenReturn(ResponseEntity.ok(null));

        Integer result = reviewService.getTotalMoviesReviewedByUser(userId);

        assertNull(result);
    }

    @Test
    void whenGetTotalMoviesReviewedByUser_andMicroserviceThrowsFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getUserReviewsStats(userId)).thenThrow(createFeignException(500));

        Integer result = reviewService.getTotalMoviesReviewedByUser(userId);

        assertNull(result);
    }

    @Test
    void whenGetLatestReviewsByUserId_andReviewsExist_thenReturnListOfReviews() {
        UUID userId = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().title("Review 1").build(),
                ReviewResponse.builder().title("Review 2").build()
        );

        when(client.getLatestReviewsByUser(userId)).thenReturn(ResponseEntity.ok(reviews));

        List<ReviewResponse> result = reviewService.getLatestReviewsByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void whenGetLatestReviewsByUserId_andUserNotFoundInMicroservice_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getLatestReviewsByUser(userId)).thenThrow(FeignException.NotFound.class);

        List<ReviewResponse> result = reviewService.getLatestReviewsByUserId(userId);

        assertNull(result);
    }

    @Test
    void whenGetLatestReviewsByUserId_andMicroserviceThrowsGenericFeignException_thenReturnNull() {
        UUID userId = UUID.randomUUID();

        when(client.getLatestReviewsByUser(userId)).thenThrow(createFeignException(503));

        List<ReviewResponse> result = reviewService.getLatestReviewsByUserId(userId);

        assertNull(result);
    }

    @Test
    void whenExtractUserIdsFromReviews_andReviewsContainMultipleUsers_thenReturnSetOfUniqueUserIds() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().userId(userId1).build(),
                ReviewResponse.builder().userId(userId2).build(),
                ReviewResponse.builder().userId(userId3).build()
        );

        Set<UUID> result = reviewService.extractUserIdsFromReviews(reviews);

        assertEquals(3, result.size());
        assertTrue(result.contains(userId1));
        assertTrue(result.contains(userId2));
        assertTrue(result.contains(userId3));
    }

    @Test
    void whenExtractUserIdsFromReviews_andReviewsContainDuplicateUserIds_thenReturnSetWithoutDuplicates() {
        UUID userId1 = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().userId(userId1).build(),
                ReviewResponse.builder().userId(userId1).build()
        );

        Set<UUID> result = reviewService.extractUserIdsFromReviews(reviews);

        assertEquals(1, result.size());
    }

    @Test
    void whenExtractUserIdsFromReviews_andReviewsListIsNull_thenReturnEmptySet() {
        Set<UUID> result = reviewService.extractUserIdsFromReviews(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractUserIdsFromReviews_andReviewsListIsEmpty_thenReturnEmptySet() {
        Set<UUID> result = reviewService.extractUserIdsFromReviews(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractUserIdsFromReviews_andSomeReviewsHaveNullUserIds_thenFilterOutNullsAndReturnValidIds() {
        UUID userId1 = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().userId(userId1).build(),
                ReviewResponse.builder().userId(null).build()
        );

        Set<UUID> result = reviewService.extractUserIdsFromReviews(reviews);

        assertEquals(1, result.size());
        assertTrue(result.contains(userId1));
    }

    @Test
    void whenExtractMovieIdsFromReviews_andReviewsContainMultipleMovies_thenReturnSetOfUniqueMovieIds() {
        UUID movieId1 = UUID.randomUUID();
        UUID movieId2 = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().movieId(movieId1).build(),
                ReviewResponse.builder().movieId(movieId2).build()
        );

        Set<UUID> result = reviewService.extractMovieIdsFromReviews(reviews);

        assertEquals(2, result.size());
        assertTrue(result.contains(movieId1));
        assertTrue(result.contains(movieId2));
    }

    @Test
    void whenExtractMovieIdsFromReviews_andReviewsContainDuplicateMovieIds_thenReturnSetWithoutDuplicates() {
        UUID movieId = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().movieId(movieId).build(),
                ReviewResponse.builder().movieId(movieId).build(),
                ReviewResponse.builder().movieId(movieId).build()
        );

        Set<UUID> result = reviewService.extractMovieIdsFromReviews(reviews);

        assertEquals(1, result.size());
    }

    @Test
    void whenExtractMovieIdsFromReviews_andReviewsListIsNull_thenReturnEmptySet() {
        Set<UUID> result = reviewService.extractMovieIdsFromReviews(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractMovieIdsFromReviews_andReviewsListIsEmpty_thenReturnEmptySet() {
        Set<UUID> result = reviewService.extractMovieIdsFromReviews(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenExtractMovieIdsFromReviews_andSomeReviewsHaveNullMovieIds_thenFilterOutNullsAndReturnValidIds() {
        UUID movieId = UUID.randomUUID();
        List<ReviewResponse> reviews = List.of(
                ReviewResponse.builder().movieId(movieId).build(),
                ReviewResponse.builder().movieId(null).build()
        );

        Set<UUID> result = reviewService.extractMovieIdsFromReviews(reviews);

        assertEquals(1, result.size());
        assertTrue(result.contains(movieId));
    }
}
