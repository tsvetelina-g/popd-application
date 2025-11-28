package app.popdapplication.web;

import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Controller
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;
    private final UserService userService;
    private final WatchedMovieService watchedMovieService;
    private final WatchlistService watchlistService;
    private final GenreService genreService;
    private final MovieCreditService movieCreditService;
    private final RatingService ratingService;
    private final ReviewService reviewService;

    public MovieController(MovieService movieService, UserService userService, WatchedMovieService watchedMovieService, WatchlistService watchlistService, GenreService genreService, MovieCreditService movieCreditService, RatingService ratingService, ReviewService reviewService) {
        this.movieService = movieService;
        this.userService = userService;
        this.watchedMovieService = watchedMovieService;
        this.watchlistService = watchlistService;
        this.genreService = genreService;
        this.movieCreditService = movieCreditService;
        this.ratingService = ratingService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ModelAndView getAllMovies(@RequestParam(required = false) UUID genreId) {
        ModelAndView modelAndView = new ModelAndView("movies");

        List<Genre> genres = genreService.findAll();
        List<Movie> movies;
        Genre selectedGenre = null;

        if (genreId != null) {
            selectedGenre = genreService.findById(genreId).orElse(null);
            if (selectedGenre != null) {
                movies = movieService.getTop5MoviesByGenreClosestReleaseDate(genreId);
            } else {
                movies = new ArrayList<>();
            }
        } else {
            movies = movieService.getTop5MostRecentReleases();
        }

        modelAndView.addObject("genres", genres);
        modelAndView.addObject("movies", movies != null ? movies : new ArrayList<>());
        modelAndView.addObject("selectedGenreId", genreId);
        modelAndView.addObject("selectedGenre", selectedGenre);

        return modelAndView;
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getAddMoviePage() {
        List<Genre> genres = genreService.findAll();

        ModelAndView modelAndView = new ModelAndView("movies-add");
        modelAndView.addObject("addMovieRequest", new AddMovieRequest());
        modelAndView.addObject("genres", genres);

        return modelAndView;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView addMovie(@Valid AddMovieRequest addMovieRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("movies-add");
            List<Genre> genres = genreService.findAll();
            modelAndView.addObject("genres", genres);
            return modelAndView;
        }

        Movie movie = movieService.addMovie(addMovieRequest);
        return new ModelAndView("redirect:/movies/" + movie.getId());
    }

    @GetMapping("/{id}")
    public ModelAndView getMoviePage(@PathVariable("id") UUID movieId, @AuthenticationPrincipal UserData userData) {
        Movie movie = movieService.findById(movieId);

        boolean movieIsWatched = false;
        boolean movieIsInWatchlist = false;
        User user = null;
        Integer rating = null;
        Double averageRating = null;
        ReviewResponse userReview = null;

        Integer totalMovieRatingsCount = ratingService.getTotalRatingsCountForAMovie(movieId);
        Integer totalMovieReviewsCount = reviewService.getMovieReviewsCount(movieId);
        List<ReviewResponse> latestFiveReviews = reviewService.getLatestFiveReviewsForAMovie(movieId);
        int usersWatchedCount = watchedMovieService.usersWatchedCount(movieId);
        List<MovieCredit> movieCredits = movieCreditService.getCreditsByMovie(movie);
        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(movie);
        Set<UUID> userIds = reviewService.extractUserIdsFromReviews(latestFiveReviews);
        Map<UUID, String> userIdToUsernameMap = userService.getUsernamesByIds(userIds);

        if (userData != null) {
            user = userService.findById(userData.getUserId());
            movieIsWatched = watchedMovieService.movieIsWatched(movie, user);
            movieIsInWatchlist = watchlistService.movieIsInWatchlist(movie, user);
            rating = ratingService.getRatingByUserAndMovie(user.getId(), movieId);
            averageRating = ratingService.getAverageRatingForAMovie(movieId);
            userReview = reviewService.getReviewByUserAndMovie(user.getId(), movieId);
        }

        ModelAndView modelAndView = new ModelAndView("movie");
        modelAndView.addObject("movie", movie);
        modelAndView.addObject("user", user);
        modelAndView.addObject("isWatched", movieIsWatched);
        modelAndView.addObject("movieIsInWatchlist", movieIsInWatchlist);
        modelAndView.addObject("usersWatchedCount", usersWatchedCount);
        modelAndView.addObject("movieCredits", movieCredits);
        modelAndView.addObject("creditsByRole", creditsByRole);
        modelAndView.addObject("rating", rating);
        modelAndView.addObject("averageRating", averageRating);
        modelAndView.addObject("totalMovieRatingsCount", totalMovieRatingsCount);
        modelAndView.addObject("totalMovieReviewsCount", totalMovieReviewsCount);
        modelAndView.addObject("userReview", userReview);
        modelAndView.addObject("latestFiveReviews", latestFiveReviews);
        modelAndView.addObject("userIdToUsernameMap", userIdToUsernameMap);

        return modelAndView;
    }

    @PostMapping("/{movieId}/watched")
    public ModelAndView addToWatched(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {
        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchedMovieService.addToWatched(movie, user);

        return new ModelAndView("redirect:/movies/" + movieId);
    }

    @DeleteMapping("/{movieId}/delete-watched")
    public ModelAndView removeFromWatched(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {
        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchedMovieService.removeFromWatched(movie, user);

        return new ModelAndView("redirect:/movies/" + movieId);
    }

    @PostMapping("/{movieId}/watchlist")
    public ModelAndView addToWatchlist(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData, jakarta.servlet.http.HttpServletRequest request) {
        request.setAttribute("movieId", movieId);
        
        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchlistService.addToWatchlist(movie, user);

        return new ModelAndView("redirect:/movies/" + movieId);
    }

    @DeleteMapping("/{movieId}/delete-from-watchlist")
    public ModelAndView removeFromWatchlist(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {
        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchlistService.removeFromWatchlist(movie, user);

        return new ModelAndView("redirect:/movies/" + movieId);
    }

    @GetMapping("/{movieId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getEditMoviePage(@PathVariable UUID movieId) {
        ModelAndView modelAndView = new ModelAndView("movies-edit");
        Movie movie = movieService.findById(movieId);
        EditMovieRequest editMovieRequest = DtoMapper.fromMovie(movie);
        List<Genre> genres = genreService.findAll();

        modelAndView.addObject("editMovieRequest", editMovieRequest);
        modelAndView.addObject("genres", genres);
        modelAndView.addObject("movie", movie);

        return modelAndView;
    }

    @PutMapping("/{movieId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView editMovieInfo(@Valid EditMovieRequest editMovieRequest, BindingResult bindingResult, @PathVariable UUID movieId) {
        Movie movie = movieService.findById(movieId);
        List<Genre> genres = genreService.findAll();
        
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("movies-edit");
            modelAndView.addObject("genres", genres);
            modelAndView.addObject("movie", movie);
            return modelAndView;
        }

        movieService.updateMovieInfo(movieId, editMovieRequest);

        return new ModelAndView("redirect:/movies/" + movieId);
    }
}
