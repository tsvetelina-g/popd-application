package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String getAdminPage() {
        return "admin";
    }

    @GetMapping
    @RequestMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ModelAndView list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        ModelAndView modelAndView = new ModelAndView("admin-users");

        // Service handles pagination validation
        Page<User> users = userService.findAll(page, size);
        
        modelAndView.addObject("users", users);
        modelAndView.addObject("page", page);
        modelAndView.addObject("size", size);
        return modelAndView;
    }

    @PatchMapping
    @RequestMapping("users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserStatus(@PathVariable UUID userId, @RequestParam int page, @RequestParam int size) {

        userService.switchStatus(userId);

        return "redirect:/admin/users?page=" + page + "&size=" + size;
    }

    @PatchMapping
    @RequestMapping("users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserRole(@PathVariable UUID userId, @RequestParam int page, @RequestParam int size) {

        userService.switchRole(userId);

        return "redirect:/admin/users?page=" + page + "&size=" + size;
    }


}
