package tn.campusride.ride.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Profile("crud")
@Controller
public class CrudPageController {
    @GetMapping({"/crud", "/crud/"})
    public String page() { return "forward:/city-crud/index.html"; }
}
