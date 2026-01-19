package io.github.jobs.spring.web;

import io.github.jobs.application.AnomalyDetector;
import io.github.jobs.spring.autoconfigure.JObsProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * UI controller for anomaly detection pages.
 */
@Controller
@RequestMapping("${j-obs.path:/j-obs}")
public class AnomalyController {

    private final AnomalyDetector anomalyDetector;
    private final JObsProperties properties;

    public AnomalyController(AnomalyDetector anomalyDetector, JObsProperties properties) {
        this.anomalyDetector = anomalyDetector;
        this.properties = properties;
    }

    @GetMapping("/anomalies")
    public String anomaliesPage(Model model) {
        model.addAttribute("basePath", properties.getPath());
        model.addAttribute("stats", anomalyDetector.getStats());
        return "anomalies";
    }
}
