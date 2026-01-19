package io.github.jobs.domain.profiling;

import java.util.List;
import java.util.Objects;

/**
 * Represents a CPU sample from profiling.
 * Contains a stack trace and the number of times it was sampled.
 */
public record CpuSample(
        List<StackFrame> stackTrace,
        long sampleCount,
        double percentage
) {
    public CpuSample {
        Objects.requireNonNull(stackTrace, "stackTrace cannot be null");
        if (sampleCount < 0) {
            throw new IllegalArgumentException("sampleCount cannot be negative");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("percentage must be between 0 and 100");
        }
    }

    /**
     * Returns the top frame of the stack trace.
     */
    public StackFrame topFrame() {
        return stackTrace.isEmpty() ? null : stackTrace.get(0);
    }

    /**
     * Returns the bottom frame (entry point) of the stack trace.
     */
    public StackFrame bottomFrame() {
        return stackTrace.isEmpty() ? null : stackTrace.get(stackTrace.size() - 1);
    }

    /**
     * Represents a single frame in a stack trace.
     */
    public record StackFrame(
            String className,
            String methodName,
            String fileName,
            int lineNumber
    ) {
        public StackFrame {
            Objects.requireNonNull(className, "className cannot be null");
            Objects.requireNonNull(methodName, "methodName cannot be null");
        }

        /**
         * Returns a formatted string representation of this frame.
         */
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(className).append(".").append(methodName);
            if (fileName != null && lineNumber > 0) {
                sb.append("(").append(fileName).append(":").append(lineNumber).append(")");
            } else if (fileName != null) {
                sb.append("(").append(fileName).append(")");
            }
            return sb.toString();
        }

        /**
         * Returns just the simple class name without package.
         */
        public String simpleClassName() {
            int lastDot = className.lastIndexOf('.');
            return lastDot >= 0 ? className.substring(lastDot + 1) : className;
        }

        /**
         * Creates a StackFrame from a StackTraceElement.
         */
        public static StackFrame from(StackTraceElement element) {
            return new StackFrame(
                    element.getClassName(),
                    element.getMethodName(),
                    element.getFileName(),
                    element.getLineNumber()
            );
        }
    }
}
