package app.popdapplication.web;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.service.ArtistService;
import app.popdapplication.service.MovieCreditService;
import app.popdapplication.service.MovieService;
import app.popdapplication.web.dto.AddCreditRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/credit")
public class CreditController {

    private final MovieService movieService;
    private final ArtistService artistService;
    private final MovieCreditService movieCreditService;

    public CreditController(MovieService movieService, ArtistService artistService, MovieCreditService movieCreditService) {
        this.movieService = movieService;
        this.artistService = artistService;
        this.movieCreditService = movieCreditService;
    }

    @GetMapping("/{movieId}/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getAddCreditPage(@PathVariable UUID movieId) {
        Movie movie = movieService.findById(movieId);

        ModelAndView modelAndView = new ModelAndView("credit-add");
        modelAndView.addObject("movie", movie);
        modelAndView.addObject("addCreditRequest", new AddCreditRequest());
        modelAndView.addObject("roles", ArtistRole.values());

        return modelAndView;
    }

    @GetMapping("/artists/search")
    @ResponseBody
    public ResponseEntity<List<String>> searchArtists(@RequestParam(required = false, defaultValue = "") String query) {
        List<String> artistNames = artistService.searchArtistNames(query, 50);
        return ResponseEntity.ok(artistNames);
    }

    @PostMapping("/{movieId}/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView addCredit(
            @PathVariable UUID movieId,
            @Valid AddCreditRequest addCreditRequest,
            BindingResult bindingResult,
            jakarta.servlet.http.HttpServletRequest request) {
        request.setAttribute("movieId", movieId);
        
        Movie movie = movieService.findById(movieId);

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("credit-add");
            modelAndView.addObject("movie", movie);
            modelAndView.addObject("roles", ArtistRole.values());
            return modelAndView;
        }

        movieCreditService.saveCredit(addCreditRequest, movieId);

        return new ModelAndView("redirect:/credit/" + movieId + "/edit");
    }

    @GetMapping("/{movieId}/edit")
    public ModelAndView getEditCreditsPage(@PathVariable UUID movieId) {
        Movie movie = movieService.findById(movieId);
        List<MovieCredit> movieCredits = movieCreditService.getCreditsByMovie(movie);
        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByMovieGroupedByRole(movie);

        ModelAndView modelAndView = new ModelAndView("credit-edit");
        modelAndView.addObject("movie", movie);
        modelAndView.addObject("movieCredits", movieCredits);
        modelAndView.addObject("creditsByRole", creditsByRole);

        return modelAndView;
    }

    @DeleteMapping("/{creditId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView deleteCredit(@PathVariable UUID creditId) {
        MovieCredit movieCredit = movieCreditService.findCreditById(creditId);
        movieCreditService.deleteCredit(movieCredit);

        return new ModelAndView("redirect:/credit/" + movieCredit.getMovie().getId() + "/edit");
    }
}
