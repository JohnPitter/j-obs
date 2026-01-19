package io.github.jobs.spring.web;

import io.github.jobs.application.DependencyChecker;
import io.github.jobs.domain.DependencyCheckResult;
import io.github.jobs.domain.DependencyStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for J-Obs.
 * Provides JSON endpoints for programmatic access to dependency status and other data.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api")
public class JObsApiController {

    private final DependencyChecker dependencyChecker;

    public JObsApiController(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    @GetMapping(value = "/requirements", produces = MediaType.APPLICATION_JSON_VALUE)
    public RequirementsResponse getRequirements() {
        DependencyCheckResult result = dependencyChecker.check();
        return RequirementsResponse.from(result);
    }

    @PostMapping(value = "/requirements/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public RequirementsResponse refreshRequirements() {
        DependencyCheckResult result = dependencyChecker.checkFresh();
        return RequirementsResponse.from(result);
    }

    @GetMapping(value = "/requirements/refresh", produces = MediaType.TEXT_HTML_VALUE)
    public String refreshRequirementsHtml() {
        // This endpoint is used by HTMX to refresh the page
        dependencyChecker.checkFresh();
        return "<script>window.location.reload();</script>";
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getStatus() {
        DependencyCheckResult result = dependencyChecker.check();
        Map<String, Object> status = new HashMap<>();
        status.put("status", result.isComplete() ? "UP" : "DEGRADED");
        status.put("dependencies", Map.of(
            "status", result.isComplete() ? "UP" : "DOWN",
            "found", result.foundCount(),
            "missing", result.missingRequiredCount()
        ));
        status.put("timestamp", Instant.now().toString());
        return status;
    }

    // Response DTOs

    public record RequirementsResponse(
        String status,
        Instant checkedAt,
        List<DependencyDto> dependencies,
        List<DependencyDto> missingRequired,
        List<DependencyDto> missingOptional,
        Map<String, InstallInstructions> instructions
    ) {
        public static RequirementsResponse from(DependencyCheckResult result) {
            Map<String, InstallInstructions> instructions = new HashMap<>();
            result.missingRequired().forEach(s -> {
                instructions.put(s.dependency().artifactId(), InstallInstructions.from(s.dependency()));
            });

            return new RequirementsResponse(
                result.overallStatus().name(),
                result.checkedAt(),
                result.statuses().stream().map(DependencyDto::from).toList(),
                result.missingRequired().stream().map(DependencyDto::from).toList(),
                result.missingOptional().stream().map(DependencyDto::from).toList(),
                instructions
            );
        }
    }

    public record DependencyDto(
        String name,
        String groupId,
        String artifactId,
        String version,
        boolean required,
        String status,
        String description
    ) {
        public static DependencyDto from(DependencyStatus status) {
            return new DependencyDto(
                status.dependency().displayName(),
                status.dependency().groupId(),
                status.dependency().artifactId(),
                status.version().orElse(null),
                status.dependency().isRequired(),
                status.status().name(),
                status.dependency().description()
            );
        }
    }

    public record InstallInstructions(
        String maven,
        String gradle,
        String documentationUrl
    ) {
        public static InstallInstructions from(io.github.jobs.domain.Dependency dep) {
            String maven = String.format("""
                <dependency>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                </dependency>""", dep.groupId(), dep.artifactId());

            String gradle = String.format("implementation '%s:%s'", dep.groupId(), dep.artifactId());

            return new InstallInstructions(maven, gradle, dep.documentationUrl());
        }
    }
}
