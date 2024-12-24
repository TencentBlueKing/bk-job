package com.tencent.bk.job.common.service.async;

import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;

public class TraceRunnable implements Runnable {
    private final Tracer tracer;
    private final Runnable delegate;
    private final Span parent;

    public TraceRunnable(Tracer tracer, Runnable delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
        this.parent = tracer.currentSpan();
    }

    @Override
    public void run() {
        Span childSpan = createChildSpan();

        try {
            Tracer.SpanInScope ws = this.tracer.withSpan(childSpan.start());
            Throwable throwable = null;

            try {
                this.delegate.run();
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
