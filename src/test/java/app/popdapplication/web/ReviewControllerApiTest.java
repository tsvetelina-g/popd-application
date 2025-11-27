package app.popdapplication.web;

import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.exception.ReviewMicroserviceUnavailableException;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.RatingService;
import app.popdapplication.service.ReviewService;
import app.popdapplication.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
public class ReviewControllerApiTest {

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postUpsertReview_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        when(ratingService.getRatingByUserAndMovie(userId, movieId)).thenReturn(8);

        MockHttpServletRequestBuilder httpRequest = post("/review/{movieId}", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(ratingService).getRatingByUserAndMovie(userId, movieId);
        verify(reviewService).upsertReview(eq(userId), eq(movieId), eq(8), eq("Title"), eq("Description"));
    }

    @Test
    void postUpsertReview_andUserIsAuthenticated_andNoRating_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        when(ratingService.getRatingByUserAndMovie(userId, movieId)).thenReturn(null);

        MockHttpServletRequestBuilder httpRequest = post("/review/{movieId}", movieId)
                .param("title", "Title 1")
                .param("content", "Description 1")
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(reviewService).upsertReview(eq(userId), eq(movieId), eq(null), eq("Title 1"), eq("Description 1"));
    }

    @Test
    void postUpsertReview_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/review/{movieId}", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).upsertReview(any(), any(), any(), any(), any());
    }

    @Test
    void postUpsertReview_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/review/{movieId}", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).upsertReview(any(), any(), any(), any(), any());
    }

    @Test
    void deleteReview_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        MockHttpServletRequestBuilder httpRequest = delete("/review/{movieId}", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(reviewService).deleteReview(userId, movieId);
    }

    @Test
    void deleteReview_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/review/{movieId}", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).deleteReview(any(), any());
    }

    @Test
    void deleteReview_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/review/{movieId}", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).deleteReview(any(), any());
    }

    @Test
    void getEditReviewPage_andUserIsAuthenticated_thenReturn200WithEditView() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        ReviewResponse reviewResponse = createTestReviewResponse(userId, movieId);

        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(reviewResponse);

        MockHttpServletRequestBuilder httpRequest = get("/review/{movieId}/edit", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("review-edit"))
                .andExpect(model().attributeExists("editReviewRequest"));

        verify(reviewService).getReviewByUserAndMovie(userId, movieId);
    }

    @Test
    void getEditReviewPage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/review/{movieId}/edit", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).getReviewByUserAndMovie(any(), any());
    }

    @Test
    void putUpdateReview_andUserIsAuthenticated_andValidData_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        when(ratingService.getRatingByUserAndMovie(userId, movieId)).thenReturn(9);

        MockHttpServletRequestBuilder httpRequest = put("/review/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(ratingService).getRatingByUserAndMovie(userId, movieId);
        verify(reviewService).upsertReview(eq(userId), eq(movieId), eq(9), eq("Title"), eq("Description"));
    }

    @Test
    void putUpdateReview_andUserIsAuthenticated_andEmptyContent_thenReturnEditViewWithErrors() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = put("/review/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("content", "")
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest);
    }

    @Test
    void putUpdateReview_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = put("/review/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).upsertReview(any(), any(), any(), any(), any());
    }

    @Test
    void putUpdateReview_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = put("/review/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("content", "Description")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(reviewService, never()).upsertReview(any(), any(), any(), any(), any());
    }

    @Test
    void getAllReviewsForMovie_andUserIsAuthenticated_thenReturn200WithReviewsView() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        List<ReviewResponse> reviewList = List.of(
                createTestReviewResponse(UUID.randomUUID(), movieId),
                createTestReviewResponse(UUID.randomUUID(), movieId)
        );
        Page<ReviewResponse> reviewsPage = new PageImpl<>(reviewList);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(reviewService.getReviewsForMovie(movieId, 0, 5)).thenReturn(reviewsPage);
        when(reviewService.extractUserIdsFromReviews(reviewList)).thenReturn(Set.of());
        when(userService.getUsernamesByIds(any())).thenReturn(Map.of());

        MockHttpServletRequestBuilder httpRequest = get("/reviews/{movieId}", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("reviews"))
                .andExpect(model().attributeExists("userIdToUsernameMap"));

        verify(movieService).findById(movieId);
        verify(reviewService).getReviewsForMovie(movieId, 0, 5);
    }

    @Test
    void getAllReviewsForMovie_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/reviews/{movieId}", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieService, never()).findById(any());
    }

    @Test
    void getAllReviewsForMovie_andUserIsAuthenticated_andCustomPagination_thenUseProvidedPageParams() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        Page<ReviewResponse> emptyPage = new PageImpl<>(List.of());

        when(movieService.findById(movieId)).thenReturn(movie);
        when(reviewService.getReviewsForMovie(movieId, 2, 10)).thenReturn(emptyPage);
        when(reviewService.extractUserIdsFromReviews(any())).thenReturn(Set.of());
        when(userService.getUsernamesByIds(any())).thenReturn(Map.of());

        MockHttpServletRequestBuilder httpRequest = get("/reviews/{movieId}", movieId)
                .param("page", "2")
                .param("size", "10")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"));

        verify(reviewService).getReviewsForMovie(movieId, 2, 10);
    }

    @Test
    void getAllReviewsForMovie_andUserIsAuthenticated_andReviewMicroserviceUnavailable_thenRedirectToMoviePageWithError() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(reviewService.getReviewsForMovie(eq(movieId), anyInt(), anyInt()))
                .thenThrow(new ReviewMicroserviceUnavailableException("Review service is unavailable"));

        MockHttpServletRequestBuilder httpRequest = get("/reviews/{movieId}", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId))
                .andExpect(flash().attributeExists("reviewErrorMessage"));

        verify(reviewService).getReviewsForMovie(movieId, 0, 5);
    }

    @Test
    void getAllReviewsForMovie_andUserIsAuthenticated_andWithUsernames_thenReturnUsernameMap() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Title").build();

        List<ReviewResponse> reviewList = List.of(
                createTestReviewResponse(userId1, movieId),
                createTestReviewResponse(userId2, movieId)
        );
        Page<ReviewResponse> reviewsPage = new PageImpl<>(reviewList);

        Set<UUID> userIds = Set.of(userId1, userId2);
        Map<UUID, String> usernameMap = Map.of(
                userId1, "tsvetelina",
                userId2, "stanimir"
        );

        when(movieService.findById(movieId)).thenReturn(movie);
        when(reviewService.getReviewsForMovie(movieId, 0, 5)).thenReturn(reviewsPage);
        when(reviewService.extractUserIdsFromReviews(reviewList)).thenReturn(userIds);
        when(userService.getUsernamesByIds(userIds)).thenReturn(usernameMap);

        MockHttpServletRequestBuilder httpRequest = get("/reviews/{movieId}", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("reviews"))
                .andExpect(model().attribute("userIdToUsernameMap", usernameMap));

        verify(userService).getUsernamesByIds(userIds);
    }

    private static UserDetails regularUserAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.USER, true);
    }

    private static ReviewResponse createTestReviewResponse(UUID userId, UUID movieId) {
        return ReviewResponse.builder()
                .userId(userId)
                .movieId(movieId)
                .rating(8)
                .title("Title")
                .content("Description")
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}

