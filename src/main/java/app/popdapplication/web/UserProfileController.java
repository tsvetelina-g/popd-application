package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserProfileController {

    private final UserService userService;
    private final WatchedMovieService watchedMovieService;
    private final WatchlistService watchlistService;
    private final RatingService ratingService;
    private final ReviewService reviewService;

    public UserProfileController(UserService userService, WatchedMovieService watchedMovieService, WatchlistService watchlistService, RatingService ratingService, ReviewService reviewService) {
        this.userService = userService;
        this.watchedMovieService = watchedMovieService;
        this.watchlistService = watchlistService;
        this.ratingService = ratingService;
        this.reviewService = reviewService;
    }

    @GetMapping("/{userId}")
    public ModelAndView getUserProfile(@PathVariable UUID userId) {

        ModelAndView modelAndView = new ModelAndView("user-profile");

        User user = userService.findById(userId);
        int watchedMoviesCount = watchedMovieService.countWatchedMovies(user);
        int moviesInWatchlistCount = watchlistService.countMoviesInWatchlist(user);
        Integer ratedMoviesCount = ratingService.getTotalMoviesRatedByUser(user.getId());
        Integer reviewedMoviesCount = reviewService.getTotalMoviesReviewedByUser(user.getId());

        modelAndView.addObject("user", user);
        modelAndView.addObject("moviesWatched", watchedMoviesCount);
        modelAndView.addObject("ratedMoviesCount", ratedMoviesCount);
        modelAndView.addObject("moviesInWatchlistCount", moviesInWatchlistCount);
        modelAndView.addObject("reviewedMoviesCount", reviewedMoviesCount);

        return modelAndView;
    }
}

