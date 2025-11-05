package app.popdapplication.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/credit")
public class CreditController {

    @GetMapping("/{movieId}/add")
    public ModelAndView getAddCreditPage(@PathVariable UUID movieId) {

        ModelAndView modelAndView = new ModelAndView("credit-add");

        return modelAndView;
    }

}
