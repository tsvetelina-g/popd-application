package app.popdapplication.web;

import app.popdapplication.exception.RatingMicroserviceUnavailableException;
import app.popdapplication.exception.ReviewMicroserviceUnavailableException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.exception.AlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFoundException(NotFoundException e) {
        log.warn("Resource not found: {}", e.getMessage());
        return "not-found";
    }


    @ExceptionHandler(AlreadyExistsException.class)
    public String handleAlreadyExistsException(
            AlreadyExistsException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        log.warn("Resource already exists: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/register")) {
            return "redirect:/register";
        }

        Object movieId = request.getAttribute("movieId");
        if (movieId != null) {
            if (referer != null && referer.contains("/credit/")) {
                return "redirect:/credit/" + movieId + "/add";
            }
            return "redirect:/movies/" + movieId;
        }

        if (referer != null) {
            return "redirect:" + referer;
        }

        return "redirect:/";
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({
            NoResourceFoundException.class,
            AccessDeniedException.class
    })
    public ModelAndView handleNoResourceFoundException(Exception e) {
        if (e instanceof NoResourceFoundException) {
            log.warn("No resource found: {}", e.getMessage());
        } else if (e instanceof AccessDeniedException) {
            log.warn("Access denied: {}", e.getMessage());
        }
        return new ModelAndView("not-found");
    }

    @ExceptionHandler(RatingMicroserviceUnavailableException.class)
    public Object handleRatingMicroserviceUnavailableException(
            RatingMicroserviceUnavailableException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        log.error("Failed to communicate with Rating Microservice: {}", e.getMessage());
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);

        if (isAjaxRequest) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

        redirectAttributes.addFlashAttribute("ratingErrorMessage", e.getMessage());

        Object movieId = request.getAttribute("movieId");

        if (movieId != null) {
            return "redirect:/movies/" + movieId;
        }

        return "redirect:/";
    }

    @ExceptionHandler(ReviewMicroserviceUnavailableException.class)
    public Object handleReviewMicroserviceUnavailableException(
            ReviewMicroserviceUnavailableException e,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        log.error("Failed to communicate with Review Microservice: {}", e.getMessage());
        String requestedWith = request.getHeader("X-Requested-With");
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);

        if (isAjaxRequest) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage());
        }

        redirectAttributes.addFlashAttribute("reviewErrorMessage", e.getMessage());

        Object movieId = request.getAttribute("movieId");

        if (movieId != null) {
            return "redirect:/movies/" + movieId;
        }

        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleLeftoverExceptions(Exception e) {
        log.error("Unhandled exception occurred", e);
        return new ModelAndView("internal-server-error");
    }
}
