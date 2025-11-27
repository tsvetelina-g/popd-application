package app.popdapplication.web;

import app.popdapplication.client.RatingDto.Rating;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.Activity;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import app.popdapplication.web.dto.EditProfileRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final WatchedMovieService watchedMovieService;
    private final RatingService ratingService;
    private final WatchlistService watchlistService;
    private final WatchlistMovieService watchlistMovieService;
    private final ReviewService reviewService;
    private final ActivityService activityService;
    private final MovieService movieService;

    public ProfileController(UserService userService, WatchedMovieService watchedMovieService, RatingService ratingService, WatchlistService watchlistService, WatchlistMovieService watchlistMovieService, ReviewService reviewService, ActivityService activityService, MovieService movieService) {
        this.userService = userService;
        this.watchedMovieService = watchedMovieService;
        this.ratingService = ratingService;
        this.watchlistService = watchlistService;
        this.watchlistMovieService = watchlistMovieService;
        this.reviewService = reviewService;
        this.activityService = activityService;
        this.movieService = movieService;
    }

    @GetMapping
    public ModelAndView getProfilePage(@AuthenticationPrincipal UserData userData) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }
        ModelAndView modelAndView = new ModelAndView("profile");
        User user = userService.findById(userData.getUserId());
        int watchedMoviesCount = watchedMovieService.countWatchedMovies(user);
        int moviesInWatchlistCount = watchlistService.countMoviesInWatchlist(user);
        Integer ratedMoviesCount = ratingService.getTotalMoviesRatedByUser(user.getId());
        Integer reviewedMoviesCount = reviewService.getTotalMoviesReviewedByUser(user.getId());
        List<Activity> activities = activityService.returnLatestFiveActivities(user.getId());
        Set<UUID> activityIds = activityService.getMovieIdsFromActivities(activities);
        Map<UUID, String> movieIdToMovieNameMap = movieService.getMovieNamesByIds(activityIds);

        modelAndView.addObject("user", user);
        modelAndView.addObject("moviesWatched", watchedMoviesCount);
        modelAndView.addObject("ratedMoviesCount", ratedMoviesCount);
        modelAndView.addObject("moviesInWatchlistCount", moviesInWatchlistCount);
        modelAndView.addObject("reviewedMoviesCount", reviewedMoviesCount);
        modelAndView.addObject("activities", activities);
        modelAndView.addObject("movieIdToMovieNameMap", movieIdToMovieNameMap);

        return modelAndView;
    }

    @GetMapping("/{id}/edit")
    public ModelAndView getEditProfilePage(@PathVariable UUID id, @AuthenticationPrincipal UserData userData) {
        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        if (!userData.getUserId().equals(id) && !userData.getRole().name().equals("ADMIN")) {
            return new ModelAndView("redirect:/profile");
        }

        ModelAndView modelAndView = new ModelAndView("edit-profile");
        User user = userService.findById(id);
        EditProfileRequest editProfileRequest = DtoMapper.fromUser(user);

        modelAndView.addObject("editProfileRequest", editProfileRequest);
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @PutMapping("/{id}/edit")
    public ModelAndView updateProfilePage(@Valid EditProfileRequest editProfileRequest, BindingResult bindingResult, @PathVariable UUID id, @AuthenticationPrincipal UserData userData) {
        if (!userData.getUserId().equals(id) && !userData.getRole().name().equals("ADMIN")) {
            return new ModelAndView("redirect:/profile");
        }

        if (bindingResult.hasErrors()) {
            User user = userService.findById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("edit-profile");
            modelAndView.addObject("user", user);
            return modelAndView;
        }

        userService.updateProfile(id, editProfileRequest);

        return new ModelAndView("redirect:/profile");
    }

    @GetMapping("/{userId}/watchlist")
    public ModelAndView getUserWatchlist(@PathVariable UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        ModelAndView modelAndView = new ModelAndView("user-watchlist");
        User user = userService.findById(userId);
        Watchlist watchlist = watchlistService.findByUser(user);
        Page<app.popdapplication.model.entity.WatchlistMovie> watchlistMovies = watchlistMovieService.findAllByWatchlistOrderByAddedOnDesc(watchlist, page, size);

        modelAndView.addObject("user", user);
        modelAndView.addObject("watchlist", watchlist);
        modelAndView.addObject("watchlistMovies", watchlistMovies);
        modelAndView.addObject("page", page);
        modelAndView.addObject("size", size);

        return modelAndView;
    }

    @GetMapping("/{userId}/watched")
    public ModelAndView getUserWatchedMovies(@PathVariable UUID userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        ModelAndView modelAndView = new ModelAndView("user-watched");
        User user = userService.findById(userId);
        Page<app.popdapplication.model.entity.WatchedMovie> watchedMovies = watchedMovieService.findAllByUserOrderByCreatedOnDesc(user, page, size);

        modelAndView.addObject("user", user);
        modelAndView.addObject("watchedMovies", watchedMovies);
        modelAndView.addObject("page", page);
        modelAndView.addObject("size", size);

        return modelAndView;
    }

    @GetMapping("/{userId}/latest-ratings")
    public ModelAndView getLatestRatings(@PathVariable UUID userId) {
        ModelAndView modelAndView = new ModelAndView("user-ratings");
        User user = userService.findById(userId);
        List<Rating> ratings = ratingService.getLatestRatingsByUserId(userId);
        Set<UUID> movieIds = ratingService.extractMovieIdsFromRatings(ratings);
        Map<UUID, String> movieIdToMovieNameMap = movieService.getMovieNamesByIds(movieIds);

        modelAndView.addObject("user", user);
        modelAndView.addObject("ratings", ratings);
        modelAndView.addObject("movieIdToMovieNameMap", movieIdToMovieNameMap);

        return modelAndView;
    }

    @GetMapping("/{userId}/latest-reviews")
    public ModelAndView getLatestReviews(@PathVariable UUID userId) {
        ModelAndView modelAndView = new ModelAndView("user-reviews");
        User user = userService.findById(userId);
        List<ReviewResponse> reviews = reviewService.getLatestReviewsByUserId(userId);
        Set<UUID> movieIds = reviewService.extractMovieIdsFromReviews(reviews);
        Map<UUID, String> movieIdToMovieNameMap = movieService.getMovieNamesByIds(movieIds);

        modelAndView.addObject("user", user);
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("movieIdToMovieNameMap", movieIdToMovieNameMap);

        return modelAndView;
    }
}
