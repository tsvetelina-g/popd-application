package app.popdapplication.web;

import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.security.UserData;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.RatingService;
import app.popdapplication.service.ReviewService;
import app.popdapplication.service.UserService;
import app.popdapplication.web.dto.EditReviewRequest;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import app.popdapplication.exception.ReviewMicroserviceUnavailableException;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final RatingService ratingService;
    private final MovieService movieService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, RatingService ratingService, MovieService movieService, UserService userService) {
        this.reviewService = reviewService;
        this.ratingService = ratingService;
        this.movieService = movieService;
        this.userService = userService;
    }

    @PostMapping("/review/{movieId}")
    public ModelAndView upsertReview(@PathVariable UUID movieId,
                                     @RequestParam String title,
                                     @RequestParam String content,
                                     @AuthenticationPrincipal UserData userData,
                                     HttpServletRequest request) {

        request.setAttribute("movieId", movieId);

        Integer rating = ratingService.getRatingByUserAndMovie(userData.getUserId(), movieId);

        reviewService.upsertReview(userData.getUserId(), movieId, rating, title, content);
        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @DeleteMapping("/review/{movieId}")
    public ModelAndView deleteReview(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData, HttpServletRequest request) {

        request.setAttribute("movieId", movieId);

        reviewService.deleteReview(userData.getUserId(), movieId);
        return new ModelAndView("redirect:/movie/" + movieId);
    }

    //th:action="@{'/review/' + ${movie.id} + '/edit'}"
    @GetMapping("/review/{movieId}/edit")
    public ModelAndView getEditReviewPage(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        ModelAndView modelAndView = new ModelAndView("review-edit");
        ReviewResponse reviewResponse = reviewService.getReviewByUserAndMovie(userData.getUserId(), movieId);
        EditReviewRequest editReviewRequest = DtoMapper.fromReviewResponse(reviewResponse);

        modelAndView.addObject("editReviewRequest", editReviewRequest);

        return modelAndView;
    }

    @PutMapping("/review/{movieId}/edit")
    public ModelAndView updateReview(@Valid EditReviewRequest editReviewRequest, BindingResult bindingResult, @PathVariable UUID movieId, @AuthenticationPrincipal UserData userData, HttpServletRequest request) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("review-edit");
        }

        request.setAttribute("movieId", movieId);

        Integer rating = ratingService.getRatingByUserAndMovie(userData.getUserId(), movieId);
        reviewService.upsertReview(userData.getUserId(), movieId, rating, editReviewRequest.getTitle(), editReviewRequest.getContent());

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @GetMapping("/reviews/{movieId}")
    public ModelAndView getAllReviewsForMovie(
            @PathVariable UUID movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            RedirectAttributes redirectAttributes) {

        try {
            ModelAndView modelAndView = new ModelAndView("reviews");

            Movie movie = movieService.findById(movieId);

            // Service handles pagination validation
            Page<ReviewResponse> reviews = reviewService.getReviewsForMovie(movieId, page, size);

            Set<UUID> userIds = reviewService.extractUserIdsFromReviews(reviews.getContent());
            Map<UUID, String> userIdToUsernameMap = userService.getUsernamesByIds(userIds);

            modelAndView.addObject("movie", movie);
            modelAndView.addObject("reviews", reviews);
            modelAndView.addObject("userIdToUsernameMap", userIdToUsernameMap);

            return modelAndView;
        } catch (ReviewMicroserviceUnavailableException e) {
            redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());
            return new ModelAndView("redirect:/movie/" + movieId);
        }
    }
}
