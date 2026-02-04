package io.github.jobs.samples.slo;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller to manage traffic simulation.
 */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final TrafficSimulator trafficSimulator;

    public TrafficController(TrafficSimulator trafficSimulator) {
        this.trafficSimulator = trafficSimulator;
    }

    @PostMapping("/enable")
    public Map<String, Object> enableTraffic() {
        trafficSimulator.setEnabled(true);
        return Map.of(
            "enabled", true,
            "message", "Traffic simulator enabled"
        );
    }

    @PostMapping("/disable")
    public Map<String, Object> disableTraffic() {
        trafficSimulator.setEnabled(false);
        return Map.of(
            "enabled", false,
            "message", "Traffic simulator disabled"
        );
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "enabled", trafficSimulator.isEnabled()
        );
    }
}
