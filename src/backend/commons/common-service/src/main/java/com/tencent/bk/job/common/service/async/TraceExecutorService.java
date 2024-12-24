package com.tencent.bk.job.common.service.async;

import org.springframework.cloud.sleuth.Tracer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 支持trace调用链的 ExecutorService
 */
public class TraceExecutorService implements ExecutorService {

    private final Tracer tracer;

    private final ExecutorService delegate;

    public TraceExecutorService(Tracer tracer, ExecutorService delegate) {
        this.tracer = tracer;
        this.delegate = delegate;
    }

    public <T> Future<T> submit(Callable<T> task) {
        return this.delegate.submit(new TraceCallable<>(tracer, task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return this.delegate.submit(new TraceRunnable(tracer, task), result);
    }

    public Future<?> submit(Runnable task) {
        return this.delegate.submit(new TraceRunnable(tracer, task));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.delegate.invokeAll(wrapCallables(tasks));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return this.delegate.invokeAll(wrapCallables(tasks), timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.delegate.invokeAny(wrapCallables(tasks));
    }

    private <T> List<TraceCallable<T>> wrapCallables(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(task -> new TraceCallable<>(tracer, task)).collect(Collectors.toList());
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.delegate.invokeAny(wrapCallables(tasks), timeout, unit);
    }

    public void execute(Runnable command) {
        this.delegate.execute(new TraceRunnable(tracer, command));
    }

    public final void shutdown() {
        this.delegate.shutdown();
    }

    public final List<Runnable> shutdownNow() {
        return this.delegate.shutdownNow();
    }

    public final boolean isShutdown() {
        return this.delegate.isShutdown();
    }

    public final boolean isTerminated() {
        return this.delegate.isTerminated();
    }

    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.delegate.awaitTermination(timeout, unit);
    }
}

