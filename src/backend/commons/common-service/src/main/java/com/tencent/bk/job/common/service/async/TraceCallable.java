package com.tencent.bk.job.common.service.async;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

import java.util.concurrent.Callable;

public class TraceCallable<V> implements Callable<V> {
    private final Tracer tracer;
    private final Callable<V> delegate;
    private final Span parent;

    public TraceCallable(Tracer tracer, Callable<V> delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
        this.parent = tracer.currentSpan();
    }

    @Override
    public V call() throws Exception {
        Span childSpan = createChildSpan();

        try {
            Tracer.SpanInScope ws = this.tracer.withSpan(childSpan.start());
            Throwable throwable = null;

            try {
                return delegate.call();
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                if (ws != null) {
                    if (throwable != null) {
                        try {
                            ws.close();
                        } catch (Throwable t) {
                            throwable.addSuppressed(t);
                        }
                    } else {
                        ws.close();
                    }
                }

            }
        } catch (Error | Exception e) {
            childSpan.error(e);
            throw e;
        } finally {
            childSpan.end();
        }
    }

    private Span createChildSpan() {
        return this.tracer.nextSpan(parent).name("async-task");
    }
}
