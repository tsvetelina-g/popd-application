package app.popdapplication.web;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.*;
import app.popdapplication.web.dto.AddMovieRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/movie")
public class MovieController {

    private final MovieService movieService;
    private final UserService userService;
    private final WatchedMovieService watchedMovieService;
    private final WatchlistService watchlistService;
    private final GenreService genreService;
    private final MovieCreditService movieCreditService;

    public MovieController(MovieService movieService, UserService userService, WatchedMovieService watchedMovieService, WatchlistService watchlistService, GenreService genreService, MovieCreditService movieCreditService) {
        this.movieService = movieService;
        this.userService = userService;
        this.watchedMovieService = watchedMovieService;
        this.watchlistService = watchlistService;
        this.genreService = genreService;
        this.movieCreditService = movieCreditService;
    }

    @GetMapping
    @RequestMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getAddMoviePage() {

        List<Genre> genres = genreService.findAll();

        ModelAndView modelAndView = new ModelAndView("movies-add");
        modelAndView.addObject("addMovieRequest", new AddMovieRequest());
        modelAndView.addObject("genres", genres);

        return modelAndView;
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView addMovie(@Valid AddMovieRequest addMovieRequest, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("movies-add");
            List<Genre> genres = genreService.findAll();
            modelAndView.addObject("genres", genres);
            return modelAndView;
        }

        Movie movie = movieService.addMovie(addMovieRequest);
        return new ModelAndView("redirect:/movie/" + movie.getId());
    }

    @GetMapping
    @RequestMapping("/{id}")
    public ModelAndView getMoviePage(@PathVariable UUID id, @AuthenticationPrincipal UserData userData) {

        Movie movie = movieService.findById(id);

        boolean movieIsWatched = false;
        boolean movieIsInWatchlist = false;
        User user = null;

        if (userData != null) {
            user = userService.findById(userData.getUserId());
            movieIsWatched = watchedMovieService.movieIsWatched(movie, user);
            movieIsInWatchlist = watchlistService.movieIsInWatchlist(movie, user);
        }

        int usersWatchedCount = watchedMovieService.usersWatchedCount(id);
        List<MovieCredit> movieCredits = movieCreditService.getCreditsByMovie(movie);
        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCredits.stream()
                .collect(Collectors.groupingBy(MovieCredit::getRoleType));

        ModelAndView modelAndView = new ModelAndView("movie");
        modelAndView.addObject("movie", movie);
        modelAndView.addObject("user", user);
        modelAndView.addObject("isWatched", movieIsWatched);
        modelAndView.addObject("movieIsInWatchlist", movieIsInWatchlist);
        modelAndView.addObject("usersWatchedCount", usersWatchedCount);
        modelAndView.addObject("movieCredits", movieCredits);
        modelAndView.addObject("creditsByRole", creditsByRole);

        return modelAndView;
    }

    @PostMapping
    @RequestMapping("/{movieId}/watched")
    public ModelAndView addToWatched(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchedMovieService.addToWatched(movie, user);

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @DeleteMapping
    @RequestMapping("/{movieId}/delete-watched")
    public ModelAndView removeFromWatched(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchedMovieService.removeFromWatched(movie, user);

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @PostMapping
    @RequestMapping("/{movieId}/watchlist")
    public ModelAndView addToWatchlist(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchlistService.addToWatchlist(movie, user);

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @DeleteMapping
    @RequestMapping("/{movieId}/delete-from-watchlist")
    public ModelAndView removeFromWatchlist(@PathVariable UUID movieId, @AuthenticationPrincipal UserData userData) {

        Movie movie = movieService.findById(movieId);
        User user = userService.findById(userData.getUserId());

        watchlistService.removeFromWatchlist(movie, user);

        return new ModelAndView("redirect:/movie/" + movieId);
    }

    @GetMapping("/{movieId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView getEditMoviePage(@PathVariable UUID movieId) {

        ModelAndView modelAndView = new ModelAndView("movies-edit");
        Movie movie = movieService.findById(movieId);
        EditMovieRequest editMovieRequest = DtoMapper.fromMovie(movie);
        List<Genre> genres = genreService.findAll();

        modelAndView.addObject("editMovieRequest", editMovieRequest);
        modelAndView.addObject("genres", genres);
        modelAndView.addObject("movie", movie);

        return modelAndView;
    }

    @PutMapping("/{movieId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView editMovieInfo(@Valid EditMovieRequest editMovieRequest, BindingResult bindingResult, @PathVariable UUID movieId) {

        Movie movie = movieService.findById(movieId);
        List<Genre> genres = genreService.findAll();

        if (bindingResult.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("movies-edit");
            modelAndView.addObject("genres", genres);
            modelAndView.addObject("movie", movie);
            return modelAndView;
        }

        movieService.updateMovieInfo(movieId, editMovieRequest);

        return new ModelAndView("redirect:/movie/" + movieId);
    }
}
