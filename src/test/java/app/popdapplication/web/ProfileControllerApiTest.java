package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .andExpect(model().attributeExists("user","moviesWatched", "ratedMoviesCount", "moviesInWatchlistCount", "reviewedMoviesCount", "activities", "movieIdToMovieNameMap"));
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
