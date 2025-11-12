package app.popdapplication.web;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.service.ArtistService;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/find")
public class FindController {

    private final MovieService movieService;
    private final ArtistService artistService;
    private final UserService userService;

    public FindController(MovieService movieService, ArtistService artistService, UserService userService) {
        this.movieService = movieService;
        this.artistService = artistService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getFindPage(@RequestParam("query") String query) {

        if (query == null || query.trim().isEmpty()) {
            ModelAndView modelAndView = new ModelAndView("find");
            modelAndView.addObject("movies", List.of());
            modelAndView.addObject("artists", List.of());
            modelAndView.addObject("users", List.of());
            modelAndView.addObject("query", "");
            return modelAndView;
        }

        List<Movie> movies = movieService.searchByTitle(query).stream().limit(20).toList();
        List<Artist> artists = artistService.searchByName(query).stream().limit(20).toList();
        List<User> users = userService.searchUsers(query).stream().limit(20).toList();

        ModelAndView modelAndView = new ModelAndView("find");
        modelAndView.addObject("movies", movies);
        modelAndView.addObject("artists", artists);
        modelAndView.addObject("users", users);
        modelAndView.addObject("query", query);

        return modelAndView;
    }
}
