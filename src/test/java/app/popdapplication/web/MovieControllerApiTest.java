package app.popdapplication.web;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import app.popdapplication.web.dto.EditMovieRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
public class MovieControllerApiTest {

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private WatchedMovieService watchedMovieService;

    @MockitoBean
    private WatchlistService watchlistService;

    @MockitoBean
    private GenreService genreService;

    @MockitoBean
    private MovieCreditService movieCreditService;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAddMoviePage_andUserIsAdmin_thenReturn200AndMoviesAddView() throws Exception {
        UserDetails adminUser = adminAuthentication();
        List<Genre> genres = List.of(
                Genre.builder().id(UUID.randomUUID()).name("Action").build(),
                Genre.builder().id(UUID.randomUUID()).name("Comedy").build()
        );

        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = get("/movie/add")
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-add"))
                .andExpect(model().attributeExists("addMovieRequest"))
                .andExpect(model().attributeExists("genres"));

        verify(genreService).findAll();
    }

    @Test
    void getAddMoviePage_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();

        MockHttpServletRequestBuilder httpRequest = get("/movie/add")
                .with(user(regularUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));

        verify(genreService, never()).findAll();
    }

    @Test
    void getAddMoviePage_andUserIsNotAuthenticated_thenReturn404NotFound() throws Exception {
        MockHttpServletRequestBuilder httpRequest = get("/movie/add");

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));

        verify(genreService, never()).findAll();
    }

    @Test
    void postAddMovie_andUserIsAdmin_andValidData_thenRedirectToMoviePage() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie createdMovie = Movie.builder().id(movieId).title("Title").build();

        when(movieService.addMovie(any())).thenReturn(createdMovie);

        MockHttpServletRequestBuilder httpRequest = post("/movie/add")
                .param("title", "Title")
                .param("description", "Description")
                .param("posterUrl", "https://www.picture.com")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(movieService).addMovie(any());
    }

    @Test
    void postAddMovie_andUserIsAdmin_andBlankTitle_thenReturnMoviesAddViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        List<Genre> genres = List.of();

        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = post("/movie/add")
                .param("title", "")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-add"))
                .andExpect(model().attributeExists("genres"));

        verify(movieService, never()).addMovie(any());
        verify(genreService).findAll();
    }

    @Test
    void postAddMovie_andUserIsAdmin_andInvalidPosterUrl_thenReturnMoviesAddViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        List<Genre> genres = List.of();

        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = post("/movie/add")
                .param("title", "Title")
                .param("posterUrl", "not-a-valid-url")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-add"));

        verify(movieService, never()).addMovie(any());
    }

    @Test
    void postAddMovie_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();

        MockHttpServletRequestBuilder httpRequest = post("/movie/add")
                .param("title", "Title")
                .with(user(regularUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(movieService, never()).addMovie(any());
    }

    @Test
    void postAddMovie_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails adminUser = adminAuthentication();

        MockHttpServletRequestBuilder httpRequest = post("/movie/add")
                .param("title", "Title")
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieService, never()).addMovie(any());
    }

    @Test
    void getMoviePage_andUserIsAuthenticated_thenReturn200WithAllAttributes() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        Movie movie = createTestMovie(movieId);
        User user = createTestUser(userId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(userService.findById(userId)).thenReturn(user);
        when(ratingService.getTotalRatingsCountForAMovie(movieId)).thenReturn(10);
        when(reviewService.getMovieReviewsCount(movieId)).thenReturn(5);
        when(reviewService.getLatestFiveReviewsForAMovie(movieId)).thenReturn(List.of());
        when(watchedMovieService.usersWatchedCount(movieId)).thenReturn(100);
        when(movieCreditService.getCreditsByMovie(movie)).thenReturn(List.of());
        when(movieCreditService.getCreditsByMovieGroupedByRole(movie)).thenReturn(Map.of());
        when(reviewService.extractUserIdsFromReviews(any())).thenReturn(Set.of());
        when(userService.getUsernamesByIds(any())).thenReturn(Map.of());
        when(watchedMovieService.movieIsWatched(movie, user)).thenReturn(true);
        when(watchlistService.movieIsInWatchlist(movie, user)).thenReturn(false);
        when(ratingService.getRatingByUserAndMovie(userId, movieId)).thenReturn(8);
        when(ratingService.getAverageRatingForAMovie(movieId)).thenReturn(7.5);
        when(reviewService.getReviewByUserAndMovie(userId, movieId)).thenReturn(null);

        MockHttpServletRequestBuilder httpRequest = get("/movie/{id}", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movie"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("isWatched"))
                .andExpect(model().attributeExists("movieIsInWatchlist"))
                .andExpect(model().attributeExists("usersWatchedCount"))
                .andExpect(model().attributeExists("movieCredits"))
                .andExpect(model().attributeExists("creditsByRole"))
                .andExpect(model().attributeExists("rating"))
                .andExpect(model().attributeExists("averageRating"))
                .andExpect(model().attributeExists("totalMovieRatingsCount"))
                .andExpect(model().attributeExists("totalMovieReviewsCount"))
                .andExpect(model().attributeExists("latestFiveReviews"))
                .andExpect(model().attributeExists("userIdToUsernameMap"));

        verify(movieService).findById(movieId);
        verify(userService).findById(userId);
        verify(watchedMovieService).movieIsWatched(movie, user);
    }

    @Test
    void getMoviePage_andUserIsAnonymous_thenReturn200WithLimitedAttributes() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(ratingService.getTotalRatingsCountForAMovie(movieId)).thenReturn(10);
        when(reviewService.getMovieReviewsCount(movieId)).thenReturn(5);
        when(reviewService.getLatestFiveReviewsForAMovie(movieId)).thenReturn(List.of());
        when(watchedMovieService.usersWatchedCount(movieId)).thenReturn(100);
        when(movieCreditService.getCreditsByMovie(movie)).thenReturn(List.of());
        when(movieCreditService.getCreditsByMovieGroupedByRole(movie)).thenReturn(Map.of());
        when(reviewService.extractUserIdsFromReviews(any())).thenReturn(Set.of());
        when(userService.getUsernamesByIds(any())).thenReturn(Map.of());

        MockHttpServletRequestBuilder httpRequest = get("/movie/{id}", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movie"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attribute("isWatched", false))
                .andExpect(model().attribute("movieIsInWatchlist", false))
                .andExpect(model().attribute("user", (Object) null));

        verify(movieService).findById(movieId);
        verify(userService, never()).findById(any());
        verify(watchedMovieService, never()).movieIsWatched(any(), any());
    }

    @Test
    void postAddToWatched_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        Movie movie = createTestMovie(movieId);
        User user = createTestUser(userId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(userService.findById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder httpRequest = post("/movie/{movieId}/watched", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(watchedMovieService).addToWatched(movie, user);
    }

    @Test
    void postAddToWatched_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/movie/{movieId}/watched", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(watchedMovieService, never()).addToWatched(any(), any());
    }

    @Test
    void postAddToWatched_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/movie/{movieId}/watched", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(watchedMovieService, never()).addToWatched(any(), any());
    }

    @Test
    void deleteFromWatched_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        Movie movie = createTestMovie(movieId);
        User user = createTestUser(userId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(userService.findById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder httpRequest = delete("/movie/{movieId}/delete-watched", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(watchedMovieService).removeFromWatched(movie, user);
    }

    @Test
    void deleteFromWatched_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/movie/{movieId}/delete-watched", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(watchedMovieService, never()).removeFromWatched(any(), any());
    }

    @Test
    void postAddToWatchlist_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        Movie movie = createTestMovie(movieId);
        User user = createTestUser(userId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(userService.findById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder httpRequest = post("/movie/{movieId}/watchlist", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(watchlistService).addToWatchlist(movie, user);
    }

    @Test
    void postAddToWatchlist_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/movie/{movieId}/watchlist", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(watchlistService, never()).addToWatchlist(any(), any());
    }

    @Test
    void deleteFromWatchlist_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        Movie movie = createTestMovie(movieId);
        User user = createTestUser(userId);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(userService.findById(userId)).thenReturn(user);

        MockHttpServletRequestBuilder httpRequest = delete("/movie/{movieId}/delete-from-watchlist", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(watchlistService).removeFromWatchlist(movie, user);
    }

    @Test
    void deleteFromWatchlist_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/movie/{movieId}/delete-from-watchlist", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(watchlistService, never()).removeFromWatchlist(any(), any());
    }

    @Test
    void getEditMoviePage_andUserIsAdmin_thenReturn200WithEditView() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);
        List<Genre> genres = List.of(
                Genre.builder().id(UUID.randomUUID()).name("Action").build()
        );

        when(movieService.findById(movieId)).thenReturn(movie);
        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = get("/movie/{movieId}/edit", movieId)
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-edit"))
                .andExpect(model().attributeExists("editMovieRequest"))
                .andExpect(model().attributeExists("genres"))
                .andExpect(model().attributeExists("movie"));

        verify(movieService).findById(movieId);
        verify(genreService).findAll();
    }

    @Test
    void getEditMoviePage_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/movie/{movieId}/edit", movieId)
                .with(user(regularUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));

        verify(movieService, never()).findById(any());
    }

    @Test
    void getEditMoviePage_andUserIsNotAuthenticated_thenReturn404NotFound() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/movie/{movieId}/edit", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));

        verify(movieService, never()).findById(any());
    }

    @Test
    void putEditMovie_andUserIsAdmin_andValidData_thenRedirectToMoviePage() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);

        MockHttpServletRequestBuilder httpRequest = put("/movie/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("description", "Description")
                .param("posterUrl", "https://www.new-picture.com")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movie/" + movieId));

        verify(movieService).updateMovieInfo(eq(movieId), any(EditMovieRequest.class));
    }

    @Test
    void putEditMovie_andUserIsAdmin_andBlankTitle_thenReturnEditViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);
        List<Genre> genres = List.of();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = put("/movie/{movieId}/edit", movieId)
                .param("title", "")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-edit"))
                .andExpect(model().attributeExists("genres"))
                .andExpect(model().attributeExists("movie"));

        verify(movieService, never()).updateMovieInfo(any(), any());
    }

    @Test
    void putEditMovie_andUserIsAdmin_andInvalidPosterUrl_thenReturnEditViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);
        List<Genre> genres = List.of();

        when(movieService.findById(movieId)).thenReturn(movie);
        when(genreService.findAll()).thenReturn(genres);

        MockHttpServletRequestBuilder httpRequest = put("/movie/{movieId}/edit", movieId)
                .param("title", "Title")
                .param("posterUrl", "invalid-url")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("movies-edit"));

        verify(movieService, never()).updateMovieInfo(any(), any());
    }

    @Test
    void putEditMovie_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = put("/movie/{movieId}/edit", movieId)
                .param("title", "Title")
                .with(user(regularUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(movieService, never()).updateMovieInfo(any(), any());
    }

    @Test
    void putEditMovie_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = put("/movie/{movieId}/edit", movieId)
                .param("title", "Title")
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieService, never()).updateMovieInfo(any(), any());
    }

    private static UserDetails adminAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.ADMIN, true);
    }

    private static UserDetails regularUserAuthentication() {
        return new UserData(UUID.randomUUID(), "stanimir", "123123123", UserRole.USER, true);
    }

    private static Movie createTestMovie(UUID movieId) {
        return Movie.builder()
                .id(movieId)
                .title("Title")
                .description("Description")
                .releaseDate(LocalDate.of(2024, 1, 15))
                .posterUrl("www.picture.com")
                .backgroundImage("www.picture.com")
                .genres(new ArrayList<>())
                .build();
    }

    private static User createTestUser(UUID userId) {
        return User.builder()
                .id(userId)
                .username("gosho")
                .email("g@gmail.com")
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }
}

