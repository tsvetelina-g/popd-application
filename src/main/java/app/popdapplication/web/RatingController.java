package app.popdapplication.web;

import app.popdapplication.security.UserData;
import app.popdapplication.service.RatingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/rating")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/{movieId}/add")
    public ModelAndView addRating(@PathVariable UUID movieId, @RequestParam int value, @AuthenticationPrincipal UserData userData,  HttpServletRequest request) {

        request.setAttribute("movieId", movieId);

        ratingService.upsertRating(userData.getUserId(), movieId, value);

        return new ModelAndView("redirect:/movie/" + movieId);
    }


    @DeleteMapping("/{movieId}/delete")
    public ModelAndView deleteRating(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        ratingService.deleteRating(userData.getUserId(), movieId);

        return new ModelAndView("redirect:/movie/" + movieId);
    }


}
