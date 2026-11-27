package tn.campusride.booking.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Profile("crud")
@Controller
public class CrudPageController {
    @GetMapping({"/crud", "/crud/"})
    public String page() { return "forward:/booking-crud/reservation.html"; }

    @GetMapping({"/crud/tables", "/crud/tables/"})
    public String tables() { return "forward:/booking-crud/index.html"; }
}
