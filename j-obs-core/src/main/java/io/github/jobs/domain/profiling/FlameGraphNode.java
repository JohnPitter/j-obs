package io.github.jobs.domain.profiling;

import java.util.*;

/**
 * Represents a node in a flame graph.
 * Each node represents a method/function and its children represent callees.
 */
public class FlameGraphNode {
    private final String name;
    private final String className;
    private final String methodName;
    private long value;
    private final Map<String, FlameGraphNode> children;

    public FlameGraphNode(String name, String className, String methodName) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.className = className;
        this.methodName = methodName;
        this.value = 0;
        this.children = new LinkedHashMap<>();
    }

    /**
     * Creates a root node for the flame graph.
     */
    public static FlameGraphNode root() {
        return new FlameGraphNode("root", "", "");
    }

    /**
     * Adds a stack trace sample to this flame graph.
     *
     * @param stackTrace the stack trace (bottom to top, i.e., entry point first)
     * @param count      the sample count
     */
    public void addSample(List<CpuSample.StackFrame> stackTrace, long count) {
        FlameGraphNode current = this;
        current.value += count;

        // Process stack trace from bottom to top (entry point to hot method)
        for (int i = stackTrace.size() - 1; i >= 0; i--) {
            CpuSample.StackFrame frame = stackTrace.get(i);
            String key = frame.className() + "." + frame.methodName();

            FlameGraphNode child = current.children.computeIfAbsent(key,
                    k -> new FlameGraphNode(frame.format(), frame.className(), frame.methodName()));
            child.value += count;
            current = child;
        }
    }

    public String name() {
        return name;
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    public long value() {
        return value;
    }

    public Collection<FlameGraphNode> children() {
        return Collections.unmodifiableCollection(children.values());
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Calculates the percentage of total samples for this node.
     */
    public double percentage(long totalSamples) {
        if (totalSamples == 0) return 0;
        return (value * 100.0) / totalSamples;
    }

    /**
     * Returns the total number of samples (value of root node).
     */
    public long totalSamples() {
        return value;
    }

    /**
     * Finds the top N hottest methods.
     */
    public List<HotMethod> findHotMethods(int limit) {
        List<HotMethod> hotMethods = new ArrayList<>();
        collectHotMethods(this, hotMethods);

        hotMethods.sort((a, b) -> Long.compare(b.selfTime(), a.selfTime()));

        if (hotMethods.size() > limit) {
            return hotMethods.subList(0, limit);
        }
        return hotMethods;
    }

    private void collectHotMethods(FlameGraphNode node, List<HotMethod> result) {
        if (!node.name.equals("root")) {
            long childTotal = node.children.values().stream()
                    .mapToLong(FlameGraphNode::value)
                    .sum();
            long selfTime = node.value - childTotal;

            if (selfTime > 0) {
                result.add(new HotMethod(
                        node.className,
                        node.methodName,
                        node.name,
                        selfTime,
                        node.value,
                        percentage(value)
                ));
            }
        }

        for (FlameGraphNode child : node.children.values()) {
            collectHotMethods(child, result);
        }
    }

    /**
     * Represents a hot method found in the profile.
     */
    public record HotMethod(
            String className,
            String methodName,
            String fullName,
            long selfTime,
            long totalTime,
            double percentage
    ) {
        public String simpleClassName() {
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }
    }
}
