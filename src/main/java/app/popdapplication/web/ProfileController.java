package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.security.UserData;
import app.popdapplication.service.ReviewService;
import app.popdapplication.service.UserService;
import app.popdapplication.service.WatchedMovieService;
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

    public ProfileController(UserService userService, ReviewService reviewService, WatchedMovieService watchedMovieService) {
        this.userService = userService;
        this.reviewService = reviewService;
        this.watchedMovieService = watchedMovieService;
    }

    @GetMapping
    public ModelAndView getProfilePage(@AuthenticationPrincipal UserData userData){

        if (userData == null) {
            return new ModelAndView("redirect:/login");
        }

        ModelAndView modelAndView = new ModelAndView("profile");
        User user = userService.findById(userData.getUserId());
//        int moviesRatedCount = ratingService.countMoviesRated(user);
        int reviewedMoviesCount = reviewService.countMoviesReviewed(user);
        int watchedMoviesCount = watchedMovieService.countWatchedMovies(user);

        modelAndView.addObject("user", user);
//        modelAndView.addObject("moviesRated", moviesRatedCount);
        modelAndView.addObject("moviesReviewed", reviewedMoviesCount);
        modelAndView.addObject("moviesWatched", watchedMoviesCount);

        return modelAndView;
    }

    @GetMapping("/{id}/edit")
    public ModelAndView getEditProfilePage(@PathVariable UUID id){

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
