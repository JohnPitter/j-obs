package io.github.jobs.spring.web;

import io.github.jobs.application.ServiceMapBuilder;
import io.github.jobs.domain.servicemap.ServiceConnection;
import io.github.jobs.domain.servicemap.ServiceMap;
import io.github.jobs.domain.servicemap.ServiceNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for Service Map operations.
 */
@RestController
@RequestMapping("${j-obs.path:/j-obs}/api/service-map")
public class ServiceMapApiController {

    private final ServiceMapBuilder serviceMapBuilder;

    public ServiceMapApiController(ServiceMapBuilder serviceMapBuilder) {
        this.serviceMapBuilder = serviceMapBuilder;
    }

    /**
     * Returns the current service map.
     */
    @GetMapping
    public ResponseEntity<ServiceMapResponse> getServiceMap(
            @RequestParam(defaultValue = "1h") String window) {
        Duration windowDuration = parseDuration(window);
        ServiceMap map = serviceMapBuilder.build(windowDuration);
        return ResponseEntity.ok(ServiceMapResponse.from(map));
    }

    /**
     * Forces a refresh of the service map.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ServiceMapResponse> refreshServiceMap() {
        ServiceMap map = serviceMapBuilder.refresh();
        return ResponseEntity.ok(ServiceMapResponse.from(map));
    }

    /**
     * Returns service map statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<ServiceMap.ServiceMapStats> getStats() {
        ServiceMap map = serviceMapBuilder.getCached();
        return ResponseEntity.ok(map.getStats());
    }

    /**
     * Returns all nodes in the service map.
     */
    @GetMapping("/nodes")
    public ResponseEntity<Collection<ServiceNode>> getNodes() {
        ServiceMap map = serviceMapBuilder.getCached();
        return ResponseEntity.ok(map.getNodes());
    }

    /**
     * Returns a specific node by ID.
     */
    @GetMapping("/nodes/{id}")
    public ResponseEntity<NodeDetailResponse> getNode(@PathVariable String id) {
        ServiceMap map = serviceMapBuilder.getCached();
        return map.getNode(id)
                .map(node -> {
                    List<ServiceConnection> incoming = map.getIncomingConnections(id);
                    List<ServiceConnection> outgoing = map.getOutgoingConnections(id);
                    return ResponseEntity.ok(new NodeDetailResponse(node, incoming, outgoing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns all connections in the service map.
     */
    @GetMapping("/connections")
    public ResponseEntity<Collection<ServiceConnection>> getConnections() {
        ServiceMap map = serviceMapBuilder.getCached();
        return ResponseEntity.ok(map.getConnections());
    }

    /**
     * Returns nodes that have issues.
     */
    @GetMapping("/nodes/issues")
    public ResponseEntity<List<ServiceNode>> getNodesWithIssues() {
        ServiceMap map = serviceMapBuilder.getCached();
        return ResponseEntity.ok(map.getNodesWithIssues());
    }

    private Duration parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return Duration.ofHours(1);
        }
        try {
            if (duration.endsWith("m")) {
                return Duration.ofMinutes(Long.parseLong(duration.replace("m", "")));
            } else if (duration.endsWith("h")) {
                return Duration.ofHours(Long.parseLong(duration.replace("h", "")));
            } else if (duration.endsWith("d")) {
                return Duration.ofDays(Long.parseLong(duration.replace("d", "")));
            }
            return Duration.parse("PT" + duration.toUpperCase());
        } catch (Exception e) {
            return Duration.ofHours(1);
        }
    }

    /**
     * Response DTO for service map.
     */
    public record ServiceMapResponse(
            Collection<ServiceNode> nodes,
            Collection<ServiceConnection> connections,
            ServiceMap.ServiceMapStats stats
    ) {
        public static ServiceMapResponse from(ServiceMap map) {
            return new ServiceMapResponse(
                    map.getNodes(),
                    map.getConnections(),
                    map.getStats()
            );
        }
    }

    /**
     * Response DTO for node details.
     */
    public record NodeDetailResponse(
            ServiceNode node,
            List<ServiceConnection> incomingConnections,
            List<ServiceConnection> outgoingConnections
    ) {}
}
