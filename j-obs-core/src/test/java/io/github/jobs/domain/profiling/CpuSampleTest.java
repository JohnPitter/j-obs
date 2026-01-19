package io.github.jobs.domain.profiling;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CpuSampleTest {

    @Test
    void shouldCreateCpuSample() {
        List<CpuSample.StackFrame> frames = List.of(
                new CpuSample.StackFrame("com.example.Service", "process", "Service.java", 42),
                new CpuSample.StackFrame("com.example.Controller", "handle", "Controller.java", 20)
        );

        CpuSample sample = new CpuSample(frames, 100, 25.5);

        assertEquals(frames, sample.stackTrace());
        assertEquals(100, sample.sampleCount());
        assertEquals(25.5, sample.percentage());
    }

    @Test
    void shouldGetTopFrame() {
        CpuSample.StackFrame top = new CpuSample.StackFrame("Top", "method", null, 0);
        CpuSample.StackFrame bottom = new CpuSample.StackFrame("Bottom", "main", null, 0);

        CpuSample sample = new CpuSample(List.of(top, bottom), 10, 5.0);

        assertEquals(top, sample.topFrame());
    }

    @Test
    void shouldGetBottomFrame() {
        CpuSample.StackFrame top = new CpuSample.StackFrame("Top", "method", null, 0);
        CpuSample.StackFrame bottom = new CpuSample.StackFrame("Bottom", "main", null, 0);

        CpuSample sample = new CpuSample(List.of(top, bottom), 10, 5.0);

        assertEquals(bottom, sample.bottomFrame());
    }

    @Test
    void shouldReturnNullForEmptyStackTrace() {
        CpuSample sample = new CpuSample(List.of(), 0, 0.0);

        assertNull(sample.topFrame());
        assertNull(sample.bottomFrame());
    }

    @Test
    void shouldRejectNullStackTrace() {
        assertThrows(NullPointerException.class, () ->
                new CpuSample(null, 0, 0.0));
    }

    @Test
    void shouldRejectNegativeSampleCount() {
        assertThrows(IllegalArgumentException.class, () ->
                new CpuSample(List.of(), -1, 0.0));
    }

    @Test
    void shouldRejectInvalidPercentage() {
        assertThrows(IllegalArgumentException.class, () ->
                new CpuSample(List.of(), 0, -1.0));

        assertThrows(IllegalArgumentException.class, () ->
                new CpuSample(List.of(), 0, 101.0));
    }

    @Test
    void shouldFormatStackFrame() {
        CpuSample.StackFrame frame = new CpuSample.StackFrame(
                "com.example.Service", "process", "Service.java", 42);

        assertEquals("com.example.Service.process(Service.java:42)", frame.format());
    }

    @Test
    void shouldFormatStackFrameWithoutLineNumber() {
        CpuSample.StackFrame frame = new CpuSample.StackFrame(
                "com.example.Service", "process", "Service.java", 0);

        assertEquals("com.example.Service.process(Service.java)", frame.format());
    }

    @Test
    void shouldFormatStackFrameWithoutFileName() {
        CpuSample.StackFrame frame = new CpuSample.StackFrame(
                "com.example.Service", "process", null, 0);

        assertEquals("com.example.Service.process", frame.format());
    }

    @Test
    void shouldGetSimpleClassName() {
        CpuSample.StackFrame frame = new CpuSample.StackFrame(
                "com.example.Service", "process", null, 0);

        assertEquals("Service", frame.simpleClassName());
    }

    @Test
    void shouldCreateFromStackTraceElement() {
        StackTraceElement element = new StackTraceElement(
                "com.example.Service", "process", "Service.java", 42);

        CpuSample.StackFrame frame = CpuSample.StackFrame.from(element);

        assertEquals("com.example.Service", frame.className());
        assertEquals("process", frame.methodName());
        assertEquals("Service.java", frame.fileName());
        assertEquals(42, frame.lineNumber());
    }
}
