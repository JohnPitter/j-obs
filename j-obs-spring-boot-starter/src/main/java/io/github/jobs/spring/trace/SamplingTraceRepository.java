package io.github.jobs.spring.trace;

import io.github.jobs.application.TraceRepository;
import io.github.jobs.domain.trace.Span;
import io.github.jobs.domain.trace.Trace;
import io.github.jobs.domain.trace.TraceQuery;

import java.io.Closeable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorator that applies sampling to a {@link TraceRepository}.
 * Sampled-out traces are silently dropped, reducing storage and processing overhead.
 * <p>
 * Once a trace is sampled in, all subsequent spans for that trace are also accepted,
 * ensuring trace completeness. The sampling decision is deterministic per trace ID
 * via {@link TraceSampler#shouldSample(String)}.
 * <p>
 * Thread-safe: uses {@link ConcurrentHashMap}-backed set for sampled trace tracking.
 */
public class SamplingTraceRepository implements TraceRepository, Closeable {

    private final TraceRepository delegate;
    private final TraceSampler sampler;
    private final Set<String> sampledTraceIds = ConcurrentHashMap.newKeySet();

    public SamplingTraceRepository(TraceRepository delegate, TraceSampler sampler) {
        this.delegate = delegate;
        this.sampler = sampler;
    }

    @Override
    public void addSpan(Span span) {
        String traceId = span.traceId();

        // If we already sampled this trace in, accept the span
        if (sampledTraceIds.contains(traceId)) {
            delegate.addSpan(span);
            return;
        }

        // New trace - make sampling decision
        if (sampler.shouldSample(traceId)) {
            sampledTraceIds.add(traceId);
            delegate.addSpan(span);
        }
        // Else: silently drop
    }

    @Override
    public Optional<Trace> findByTraceId(String traceId) {
        return delegate.findByTraceId(traceId);
    }

    @Override
    public List<Trace> query(TraceQuery query) {
        return delegate.query(query);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public long count(TraceQuery query) {
        return delegate.count(query);
    }

    @Override
    public void clear() {
        sampledTraceIds.clear();
        delegate.clear();
    }

    @Override
    public TraceStats stats() {
        return delegate.stats();
    }

    @Override
    public void close() {
        sampledTraceIds.clear();
        if (delegate instanceof Closeable c) {
            try {
                c.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public double getSampleRate() {
        return sampler.getSampleRate();
    }
}
