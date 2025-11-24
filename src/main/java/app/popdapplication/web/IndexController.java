package app.popdapplication.web;

import app.popdapplication.model.entity.Movie;
import app.popdapplication.service.ActivityService;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.UserService;
import app.popdapplication.web.dto.LoginRequest;
import app.popdapplication.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;


@Controller
public class IndexController {

    private final UserService userService;
    private final ActivityService activityService;
    private final MovieService movieService;

    public IndexController(UserService userService, ActivityService activityService, MovieService movieService) {
        this.userService = userService;
        this.activityService = activityService;
        this.movieService = movieService;
    }

    @GetMapping("/")
    public ModelAndView index() {

        ModelAndView modelAndView = new ModelAndView("index");

        List<UUID> movieIds = activityService.getTopMovieIds();
        List<Movie> movies = movieService.getTopMovies(movieIds);

        modelAndView.addObject("movies", movies);

        return modelAndView;
    }

    @GetMapping("/register")
    public ModelAndView getRegisterPage(){

        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView register(@Valid RegisterRequest registerRequest, BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            return new ModelAndView("register");
        }

        userService.register(registerRequest);
        return new ModelAndView("redirect:/login");
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(name = "loginAttemptMessage", required = false) String message, @RequestParam(name = "error", required = false) String error) {

        ModelAndView modelAndView = new ModelAndView("login");

        modelAndView.addObject("loginRequest", new LoginRequest());
        modelAndView.addObject("loginAttemptMessage", message);
        if (error != null){
            modelAndView.addObject("error", "Invalid username or password");
        }

        return modelAndView;
    }

}
