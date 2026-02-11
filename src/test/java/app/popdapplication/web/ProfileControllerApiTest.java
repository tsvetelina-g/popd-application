package app.popdapplication.web;

import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.WatchedMovie;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.entity.WatchlistMovie;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
public class ProfileControllerApiTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WatchedMovieService watchedMovieService;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private WatchlistService watchlistService;

    @MockitoBean
    private WatchlistMovieService watchlistMovieService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProfilePage_shouldReturnProfileViewWithUserModelAttributesAndStatusCodeIs200() throws Exception {

        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(
                userId,
                mockUser.getUsername(),
                mockUser.getPassword(),
                UserRole.USER,
                true
        );

        when(userService.findById(userId)).thenReturn(mockUser);
        when(watchedMovieService.countWatchedMovies(any())).thenReturn(5);
        when(watchlistService.countMoviesInWatchlist(any())).thenReturn(3);
        when(ratingService.getTotalMoviesRatedByUser(any())).thenReturn(10);
        when(reviewService.getTotalMoviesReviewedByUser(any())).thenReturn(2);
        when(activityService.returnLatestFiveActivities(any())).thenReturn(Collections.emptyList());
        when(activityService.getMovieIdsFromActivities(any())).thenReturn(Collections.emptySet());
        when(movieService.getMovieNamesByIds(any())).thenReturn(Collections.emptyMap());

        MockHttpServletRequestBuilder httpRequest = get("/profile")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(view().name("profile"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("user", "moviesWatched", "ratedMoviesCount", "moviesInWatchlistCount", "reviewedMoviesCount", "activities", "movieIdToMovieNameMap"));
    }

    @Test
    void getProfilePage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getEditProfilePage_andUserIsAuthenticated_thenReturnEditView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);

        when(userService.findById(userId)).thenReturn(mockUser);

        mockMvc.perform(get("/profile/{id}/edit", userId)
                        .with(user(mockUserData)))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-profile"))
                .andExpect(model().attributeExists("editProfileRequest", "user"));
    }

    @Test
    void getEditProfilePage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/profile/{id}/edit", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getEditProfilePage_andUnauthorizedUser_thenRedirectToProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UserData mockUserData = new UserData(otherUserId, "other", "123123123", UserRole.USER, true);

        mockMvc.perform(get("/profile/{id}/edit", userId)
                        .with(user(mockUserData)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void putUpdateProfile_andUserIsAuthenticated_andValidData_thenRedirectToProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);

        mockMvc.perform(put("/profile/{id}/edit", userId)
                        .param("username", "newusername")
                        .param("email", "new@email.com")
                        .with(user(mockUserData))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(userService).updateProfile(eq(userId), any());
    }

    @Test
    void putUpdateProfile_andUserIsAuthenticated_andInvalidData_thenReturnEditView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);

        when(userService.findById(userId)).thenReturn(mockUser);

        mockMvc.perform(put("/profile/{id}/edit", userId)
                        .param("username", "ab") // Invalid: too short
                        .param("email", "invalid-email") // Invalid email
                        .with(user(mockUserData))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-profile"))
                .andExpect(model().attributeExists("user"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    void putUpdateProfile_andUnauthorizedUser_thenRedirectToProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UserData mockUserData = new UserData(otherUserId, "other", "123123123", UserRole.USER, true);

        mockMvc.perform(put("/profile/{id}/edit", userId)
                        .param("username", "newusername")
                        .param("email", "new@email.com")
                        .with(user(mockUserData))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    void getUserWatchlist_thenReturnWatchlistView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);
        Watchlist watchlist = Watchlist.builder()
                .id(UUID.randomUUID())
                .user(mockUser)
                .name("My Watchlist")
                .watchlistType(WatchlistType.DEFAULT)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        Page<WatchlistMovie> watchlistMovies = new PageImpl<>(Collections.emptyList());

        when(userService.findById(userId)).thenReturn(mockUser);
        when(watchlistService.findByUser(mockUser)).thenReturn(watchlist);
        when(watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(eq(watchlist), anyInt(), anyInt()))
                .thenReturn(watchlistMovies);

        mockMvc.perform(get("/profile/{userId}/watchlist", userId)
                        .with(user(mockUserData)))
                .andExpect(status().isOk())
                .andExpect(view().name("user-watchlist"))
                .andExpect(model().attributeExists("user", "watchlist", "watchlistMovies", "page", "size"));
    }

    @Test
    void getUserWatchedMovies_thenReturnWatchedView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);
        Page<WatchedMovie> watchedMovies = new PageImpl<>(Collections.emptyList());

        when(userService.findById(userId)).thenReturn(mockUser);
        when(watchedMovieService.findAllByUserOrderByCreatedOnDesc(eq(mockUser), anyInt(), anyInt()))
                .thenReturn(watchedMovies);

        mockMvc.perform(get("/profile/{userId}/watched", userId)
                        .with(user(mockUserData)))
                .andExpect(status().isOk())
                .andExpect(view().name("user-watched"))
                .andExpect(model().attributeExists("user", "watchedMovies", "page", "size"));
    }

    @Test
    void getLatestRatings_thenReturnRatingsView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);
        List<Rating> ratings = Collections.emptyList();

        when(userService.findById(userId)).thenReturn(mockUser);
        when(ratingService.getLatestRatingsByUserId(userId)).thenReturn(ratings);
        when(ratingService.extractMovieIdsFromRatings(ratings)).thenReturn(Collections.emptySet());
        when(movieService.getMovieNamesByIds(any())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/profile/{userId}/latest-ratings", userId)
                        .with(user(mockUserData)))
                .andExpect(status().isOk())
                .andExpect(view().name("user-ratings"))
                .andExpect(model().attributeExists("user", "ratings", "movieIdToMovieNameMap"));
    }

    @Test
    void getLatestReviews_thenReturnReviewsView() throws Exception {
        UUID userId = UUID.randomUUID();
        User mockUser = randomUser();
        mockUser.setId(userId);
        UserData mockUserData = new UserData(userId, "tsvetelina", "123123123", UserRole.USER, true);
        List<ReviewResponse> reviews = Collections.emptyList();

        when(userService.findById(userId)).thenReturn(mockUser);
        when(reviewService.getLatestReviewsByUserId(userId)).thenReturn(reviews);
        when(reviewService.extractMovieIdsFromReviews(reviews)).thenReturn(Collections.emptySet());
        when(movieService.getMovieNamesByIds(any())).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/profile/{userId}/latest-reviews", userId)
                        .with(user(mockUserData)))
                .andExpect(status().isOk())
                .andExpect(view().name("user-reviews"))
                .andExpect(model().attributeExists("user", "reviews", "movieIdToMovieNameMap"));
    }

    public static User randomUser() {
        return User.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}
