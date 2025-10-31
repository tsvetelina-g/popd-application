package app.popdapplication.web;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.User;
import app.popdapplication.service.GenreService;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.UserService;
import app.popdapplication.web.dto.AddMovieRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final MovieService movieService;
    private final GenreService genreService;

    public AdminController(UserService userService, MovieService movieService, GenreService genreService) {
        this.userService = userService;
        this.movieService = movieService;
        this.genreService = genreService;
    }

    @GetMapping
    public String getAdminPage() {
        return "admin";
    }

    @GetMapping
    @RequestMapping("/users")
    public ModelAndView list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        ModelAndView modelAndView = new ModelAndView("admin-users");

        Page<User> users = userService.findAll(PageRequest.of(page, size));
        modelAndView.addObject("users", users);
        modelAndView.addObject("page", page);
        modelAndView.addObject("size", size);
        return modelAndView;
    }

    @PatchMapping
    @RequestMapping("users/{userId}/status")
    public String changeUserStatus(@PathVariable UUID userId, @RequestParam int page, @RequestParam int size){

        userService.switchStatus(userId);

        return "redirect:/admin/users?page=" + page + "&size=" + size;
    }

    @PatchMapping
    @RequestMapping("users/{userId}/role")
    public String changeUserRole(@PathVariable UUID userId, @RequestParam int page, @RequestParam int size){

        userService.switchRole(userId);

        return "redirect:/admin/users?page=" + page + "&size=" + size;
    }

    @GetMapping
    @RequestMapping("/movies/add")
    public ModelAndView addMovie(){

        List<Genre> genres = genreService.findAll();

        ModelAndView modelAndView = new ModelAndView("movies-add");
        modelAndView.addObject("addMovieRequest", new AddMovieRequest());
        modelAndView.addObject("genres", genres);

        return modelAndView;
    }

    @PostMapping("/movies/add")
    public ModelAndView register(@Valid AddMovieRequest addMovieRequest, BindingResult bindingResult){

        if (bindingResult.hasErrors()){
            ModelAndView modelAndView = new ModelAndView("/movies/add");
            List<Genre> genres = genreService.findAll();
            modelAndView.addObject("genres", genres);
            return modelAndView;
        }

        movieService.addMovie(addMovieRequest);
        return new ModelAndView("redirect:/login");
    }
}
