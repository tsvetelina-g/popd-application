package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ModelAndView getUserProfile(@PathVariable UUID id) {
        User user = userService.findById(id);

        ModelAndView modelAndView = new ModelAndView("user-profile");
        modelAndView.addObject("user", user);

        return modelAndView;
    }
}

