package com.support.ticket.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA fallback: client routes are served index.html so browser refresh works.
 * Does not map /api/** — REST controllers own those paths.
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/dashboard",
            "/tickets",
            "/tickets/{*path}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
