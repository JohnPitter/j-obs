package io.github.jobs.spring.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * UI Controller for Service Map page.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}")
public class ServiceMapController {

    /**
     * Renders the service map visualization page.
     */
    @GetMapping("/service-map")
    public String serviceMap() {
        return "service-map";
    }
}
