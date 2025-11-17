package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import app.popdapplication.web.dto.EditProfileRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final ReviewService reviewService;
    private final WatchedMovieService watchedMovieService;
    private final RatingService ratingService;
    private final WatchlistMovieService watchlistMovieService;
    private final WatchlistService watchlistService;

    public ProfileController(UserService userService, ReviewService reviewService, WatchedMovieService watchedMovieService, RatingService ratingService, WatchlistMovieService watchlistMovieService, WatchlistService watchlistService) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.watchedMovieService = watchedMovieService;
        this.ratingService = ratingService;
        this.watchlistMovieService = watchlistMovieService;
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ModelAndView getProfilePage(@AuthenticationPrincipal UserData userData){

        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView modelAndView = new ModelAndView("profile");
        User user = userService.findById(userData.getUserId());
        int watchedMoviesCount = watchedMovieService.countWatchedMovies(user);
        int moviesInWatchlistCount = watchlistService.countMoviesInWatchlist(user);
        Integer ratedMoviesCount = ratingService.getTotalMoviesRatedByUser(user.getId());

        modelAndView.addObject("user", user);
        modelAndView.addObject("moviesWatched", watchedMoviesCount);
        modelAndView.addObject("ratedMoviesCount", ratedMoviesCount);
        modelAndView.addObject("moviesInWatchlistCount", moviesInWatchlistCount);

        return modelAndView;
    }

    @GetMapping("/{id}/edit")
    public ModelAndView getEditProfilePage(@PathVariable UUID id, @AuthenticationPrincipal UserData userData){

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
    public ModelAndView updateProfilePage(@Valid EditProfileRequest editProfileRequest, BindingResult bindingResult, @PathVariable UUID id){

        if (bindingResult.hasErrors()){
            User user = userService.findById(id);
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("edit-profile");
            modelAndView.addObject("user", user);
            return modelAndView;
        }

        userService.updateProfile(id, editProfileRequest);

        return new ModelAndView("redirect:/profile");
    }
}
