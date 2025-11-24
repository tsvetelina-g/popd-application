package app.popdapplication.web;

import app.popdapplication.exception.RatingMicroserviceUnavailableException;
import app.popdapplication.exception.ReviewMicroserviceUnavailableException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.exception.AlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFoundException(NotFoundException e) {

        return "not-found";
    }


    @ExceptionHandler(AlreadyExistsException.class)
    public String handleAlreadyExistsException(
            AlreadyExistsException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/register")) {
            return "redirect:/register";
        }

        Object movieId = request.getAttribute("movieId");
        if (movieId != null) {
            if (referer != null && referer.contains("/credit/")) {
                // Credit errors always come from add page, redirect back there
                return "redirect:/credit/" + movieId + "/add";
            }
            return "redirect:/movie/" + movieId;
        }

        if (referer != null) {
            return "redirect:" + referer;
        }

        return "redirect:/";
    }


    @ExceptionHandler({
            NoResourceFoundException.class,
            AccessDeniedException.class
    })
    public ModelAndView handleNoResourceFoundException() {

        ModelAndView modelAndView = new ModelAndView("not-found");

        return modelAndView;
    }

    @ExceptionHandler(RatingMicroserviceUnavailableException.class)
    public Object handleRatingMicroserviceUnavailableException(
            RatingMicroserviceUnavailableException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Check if this is an AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);

        if (isAjaxRequest) {
            // For AJAX requests, return error response with message
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

        // For normal requests, use redirect with flash attribute
        redirectAttributes.addFlashAttribute("ratingErrorMessage", e.getMessage());

        Object movieId = request.getAttribute("movieId");

        if (movieId != null) {
            return "redirect:/movie/" + movieId;
        }

        return "redirect:/";
    }

    @ExceptionHandler(ReviewMicroserviceUnavailableException.class)
    public Object handleReviewMicroserviceUnavailableException(
            ReviewMicroserviceUnavailableException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        // Check if this is an AJAX request
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);

        if (isAjaxRequest) {
            // For AJAX requests, return error response with message
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

        // For normal requests, use redirect with flash attribute
        redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());

        Object movieId = request.getAttribute("movieId");

        if (movieId != null) {
            return "redirect:/movie/" + movieId;
        }

        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleLeftoverExceptions(Exception e) {

        ModelAndView modelAndView = new ModelAndView("internal-server-error");

        return modelAndView;
    }
}
