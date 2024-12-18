package com.tencent.bk.job.execute.util;

import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 支持上下文传播的 ExecutorService
 */
public class ContextExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    private ContextExecutorService(ExecutorService delegate) {
        this.delegate = delegate;
    }

    public static ContextExecutorService wrap(ExecutorService delegate) {
        return new ContextExecutorService(delegate);
    }


    public <T> Future<T> submit(Callable<T> task) {
        return this.delegate.submit(ContextCallable.wrap(task));
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return this.delegate.submit(ContextRunnable.wrap(task), result);
    }

    public Future<?> submit(Runnable task) {
        return this.delegate.submit(ContextRunnable.wrap(task));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.delegate.invokeAll(ContextCallable.wrap(tasks));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        return this.delegate.invokeAll(ContextCallable.wrap(tasks), timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return this.delegate.invokeAny(ContextCallable.wrap(tasks));
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.delegate.invokeAny(ContextCallable.wrap(tasks), timeout, unit);
    }

    public void execute(Runnable command) {
        this.delegate.execute(ContextRunnable.wrap(command));
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

    private static class ContextCallable<T> implements Callable<T> {

        private final JobExecuteContext context;
        private final Callable<T> delegate;


        private ContextCallable(Callable<T> delegate) {
            this.context = JobExecuteContextThreadLocalRepo.get();
            this.delegate = delegate;
        }

        public static <T> ContextCallable<T> wrap(Callable<T> delegate) {
            return new ContextCallable<>(delegate);
        }

        public static <T> List<ContextCallable<T>> wrap(Collection<? extends Callable<T>> delegates) {
            List<ContextCallable<T>> contextCallables = new ArrayList<>(delegates.size());
            delegates.forEach(delegate -> contextCallables.add(new ContextCallable<>(delegate)));
            return contextCallables;
        }

        @Override
        public T call() throws Exception {
            try {
                JobExecuteContextThreadLocalRepo.set(context);
                return delegate.call();
            } finally {
                JobExecuteContextThreadLocalRepo.unset();
            }
        }
    }

    private static class ContextRunnable implements Runnable {

        private final JobExecuteContext context;
        private final Runnable delegate;


        private ContextRunnable(Runnable delegate) {
            this.context = JobExecuteContextThreadLocalRepo.get();
            this.delegate = delegate;
        }

        public static ContextRunnable wrap(Runnable delegate) {
            return new ContextRunnable(delegate);
        }

        public static List<ContextRunnable> wrap(Collection<Runnable> delegates) {
            List<ContextRunnable> contextRunnableList = new ArrayList<>(delegates.size());
            delegates.forEach(delegate -> contextRunnableList.add(new ContextRunnable(delegate)));
            return contextRunnableList;
        }

        @Override
        public void run() {
            try {
                JobExecuteContextThreadLocalRepo.set(context);
                delegate.run();
            } finally {
                JobExecuteContextThreadLocalRepo.unset();
            }
        }
    }
}

