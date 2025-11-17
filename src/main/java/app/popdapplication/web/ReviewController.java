package app.popdapplication.web;

import app.popdapplication.client.ReviewDto.ReviewResponse;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.security.UserData;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.RatingService;
import app.popdapplication.service.ReviewService;
import app.popdapplication.web.dto.EditReviewRequest;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.UUID;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final RatingService ratingService;
    private final MovieService movieService;

    public ReviewController(ReviewService reviewService, RatingService ratingService, MovieService movieService) {
        this.reviewService = reviewService;
        this.ratingService = ratingService;
        this.movieService = movieService;
    }

    @PostMapping("/review/{movieId}")
    public ModelAndView upsertReview(@PathVariable UUID movieId,
                                     @RequestParam String title,
                                     @RequestParam String content,
                                     @AuthenticationPrincipal UserData userData) {

        Integer rating = ratingService.getRatingByUserAndMovie(userData.getUserId(), movieId);

        reviewService.upsertReview(userData.getUserId(), movieId, rating, title, content);
        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @DeleteMapping("/review/{movieId}")
    public ModelAndView deleteReview(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

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
    public ModelAndView updateReview(@Valid EditReviewRequest editReviewRequest, BindingResult bindingResult, @PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        if (bindingResult.hasErrors()) {
            return new ModelAndView("review-edit");
        }

        Integer rating = ratingService.getRatingByUserAndMovie(userData.getUserId(), movieId);
        reviewService.upsertReview(userData.getUserId(), movieId, rating, editReviewRequest.getTitle(), editReviewRequest.getContent());

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @GetMapping("/reviews/{movieId}")
    public ModelAndView getAllReviewsForMovie(
            @PathVariable UUID movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        ModelAndView modelAndView = new ModelAndView("reviews");

        // Get movie info (you'll need to inject MovieService)
        Movie movie = movieService.findById(movieId);

        // Get paginated reviews
        Page<ReviewResponse> reviews = reviewService.getReviewsForMovie(movieId, PageRequest.of(page, size));

        // Get usernames for reviews
        Map<UUID, String> userIdToUsernameMap = reviewService.getUsernamesForReviews(reviews.getContent());

        modelAndView.addObject("movie", movie);
        modelAndView.addObject("reviews", reviews);
        modelAndView.addObject("userIdToUsernameMap", userIdToUsernameMap);

        return modelAndView;
    }
}
