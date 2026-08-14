/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.engine.rolling.scatter;

import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.listener.event.RollingBatchDispatchResumeEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.monitor.metrics.ScatterDispatchMonitor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ScatterDispatchManager} 优雅停机转移未下发批次的 trace 续链单元测试。
 * <p>
 * 覆盖 Issue #4368 可观测性缺陷：{@code stop()} 生命周期线程无 trace 作用域，若直接发转移事件则
 * MQ messaging 埋点无 trace 可透传，接收方在全新消费 trace 下重新入队，与原作业 traceId 断链。
 * 修复后转移事件必须在以「登记时捕获的原始 Span」为父重建的 trace 作用域内发出，保证跨副本日志不断链。
 */
class ScatterDispatchManagerHandoffTraceTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final int EXECUTE_COUNT = 0;
    private static final int BATCH = 2;
    private static final long GSE_TASK_ID = 300L;

    private ScatterBatchDispatcher scatterBatchDispatcher;
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private Tracer tracer;
    private ScatterDispatchMonitor scatterDispatchMonitor;

    private ScatterDispatchManager manager;

    @BeforeEach
    void setUp() {
        scatterBatchDispatcher = mock(ScatterBatchDispatcher.class);
        taskExecuteMQEventDispatcher = mock(TaskExecuteMQEventDispatcher.class);
        tracer = mock(Tracer.class);
        scatterDispatchMonitor = mock(ScatterDispatchMonitor.class);
        JobExecuteConfig jobExecuteConfig = mock(JobExecuteConfig.class);
        // 单 worker，减少测试线程；到点触发依赖 DelayQueue 到期，测试用远期 dispatchTime 规避消费
        when(jobExecuteConfig.getRollingScatterWorkerNum()).thenReturn(1);

        manager = new ScatterDispatchManager(
            scatterBatchDispatcher,
            taskExecuteMQEventDispatcher,
            tracer,
            scatterDispatchMonitor,
            jobExecuteConfig);
    }

    @Test
    @DisplayName("停机转移：在以原始Span为父重建的trace作用域内发出恢复事件，事件携带原批次信息")
    void handoff_dispatchesResumeEventWithinOriginalTraceScope() {
        Span originalSpan = mock(Span.class);
        Span handoffSpan = mock(Span.class);
        Tracer.SpanInScope spanInScope = mock(Tracer.SpanInScope.class);
        // 登记线程捕获的原始作业 Span
        when(tracer.currentSpan()).thenReturn(originalSpan);
        // 以原始 Span 为父重建可续链的子 Span
        when(tracer.nextSpan(originalSpan)).thenReturn(handoffSpan);
        when(handoffSpan.name(any())).thenReturn(handoffSpan);
        when(handoffSpan.start()).thenReturn(handoffSpan);
        when(tracer.withSpan(handoffSpan)).thenReturn(spanInScope);

        manager.start();
        // 远期 dispatchTime，保证 worker 不会到点消费，停机时该批次仍留在队列被转移
        long farFutureDispatchTime = System.currentTimeMillis() + 3_600_000L;
        ScatterDispatchTask task = new ScatterDispatchTask(
            JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, BATCH, farFutureDispatchTime, GSE_TASK_ID);
        manager.addTask(task);

        // 断言：事件必须在 trace 作用域内发出（withSpan 先于发事件，span.end 在其后）
        InOrder inOrder = inOrder(tracer, taskExecuteMQEventDispatcher, handoffSpan);

        manager.stop();

        // 以原始 Span 为父重建 trace（而非新建无关联的根 Span），从而续用原 traceId
        verify(tracer).nextSpan(originalSpan);
        inOrder.verify(tracer).withSpan(handoffSpan);
        ArgumentCaptor<RollingBatchDispatchResumeEvent> eventCaptor =
            ArgumentCaptor.forClass(RollingBatchDispatchResumeEvent.class);
        inOrder.verify(taskExecuteMQEventDispatcher).dispatchRollingBatchDispatchResumeEvent(eventCaptor.capture());
        // 作用域结束后关闭 SpanInScope 并结束 Span，避免 Span 泄漏
        inOrder.verify(handoffSpan).end();

        RollingBatchDispatchResumeEvent event = eventCaptor.getValue();
        assertThat(event.getJobInstanceId()).isEqualTo(JOB_INSTANCE_ID);
        assertThat(event.getStepInstanceId()).isEqualTo(STEP_INSTANCE_ID);
        assertThat(event.getExecuteCount()).isEqualTo(EXECUTE_COUNT);
        assertThat(event.getBatch()).isEqualTo(BATCH);
        assertThat(event.getDispatchTime()).isEqualTo(farFutureDispatchTime);
        assertThat(event.getGseTaskId()).isEqualTo(GSE_TASK_ID);
    }

    @Test
    @DisplayName("停机转移：登记时无trace上下文(如已是转移重建的批次)时新建可关联的根Span后再发事件")
    void handoff_buildsNewRootSpanWhenNoOriginalTraceContext() {
        Span handoffSpan = mock(Span.class);
        Tracer.SpanInScope spanInScope = mock(Tracer.SpanInScope.class);
        // 登记线程无 currentSpan
        when(tracer.currentSpan()).thenReturn(null);
        when(tracer.nextSpan()).thenReturn(handoffSpan);
        when(handoffSpan.name(any())).thenReturn(handoffSpan);
        when(handoffSpan.start()).thenReturn(handoffSpan);
        when(tracer.withSpan(handoffSpan)).thenReturn(spanInScope);

        manager.start();
        long farFutureDispatchTime = System.currentTimeMillis() + 3_600_000L;
        ScatterDispatchTask task = new ScatterDispatchTask(
            JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, BATCH, farFutureDispatchTime, GSE_TASK_ID);
        manager.addTask(task);

        manager.stop();

        // 无父上下文时走 nextSpan()，仍在 trace 作用域内发事件
        verify(tracer).nextSpan();
        verify(tracer).withSpan(handoffSpan);
        verify(taskExecuteMQEventDispatcher).dispatchRollingBatchDispatchResumeEvent(any());
        verify(handoffSpan).end();
    }

    @Test
    @DisplayName("停机转移：单个批次发事件失败不影响span收尾，异常被吞并记录到span")
    void handoff_swallowsExceptionAndEndsSpan() {
        Span originalSpan = mock(Span.class);
        Span handoffSpan = mock(Span.class);
        Tracer.SpanInScope spanInScope = mock(Tracer.SpanInScope.class);
        when(tracer.currentSpan()).thenReturn(originalSpan);
        when(tracer.nextSpan(originalSpan)).thenReturn(handoffSpan);
        when(handoffSpan.name(any())).thenReturn(handoffSpan);
        when(handoffSpan.start()).thenReturn(handoffSpan);
        when(tracer.withSpan(handoffSpan)).thenReturn(spanInScope);
        RuntimeException boom = new RuntimeException("mq send failed");
        doAnswer(invocation -> {
            throw boom;
        }).when(taskExecuteMQEventDispatcher).dispatchRollingBatchDispatchResumeEvent(any());

        manager.start();
        long farFutureDispatchTime = System.currentTimeMillis() + 3_600_000L;
        manager.addTask(new ScatterDispatchTask(
            JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, BATCH, farFutureDispatchTime, GSE_TASK_ID));

        // 转移单批失败不应向上抛出，避免中断停机流程与其它批次转移
        manager.stop();

        verify(handoffSpan).error(boom);
        verify(handoffSpan).end();
    }
}
