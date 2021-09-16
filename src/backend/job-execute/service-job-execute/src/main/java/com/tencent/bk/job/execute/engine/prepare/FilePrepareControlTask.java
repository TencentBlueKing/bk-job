package com.tencent.bk.job.execute.engine.prepare;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.result.ContinuousScheduledTask;
import com.tencent.bk.job.execute.engine.result.ScheduleStrategy;
import com.tencent.bk.job.execute.engine.result.StopTaskCounter;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;

import java.util.concurrent.atomic.AtomicBoolean;

public class FilePrepareControlTask implements ContinuousScheduledTask {

    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;
    volatile AtomicBoolean isDoneWrapper = new AtomicBoolean(false);
    private final FilePrepareService filePrepareService;
    private final TaskInstanceService taskInstanceService;
    private final StepInstanceDTO stepInstance;

    public FilePrepareControlTask(
        FilePrepareService filePrepareService,
        TaskInstanceService taskInstanceService,
        StepInstanceDTO stepInstance
    ) {
        this.filePrepareService = filePrepareService;
        this.taskInstanceService = taskInstanceService;
        this.stepInstance = stepInstance;
    }

    public void setDoneStatus() {
        isDoneWrapper.set(true);
    }

    @Override
    public boolean isFinished() {
        return isDoneWrapper.get();
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        // 每秒检查一次是否需要停止
        return () -> 1000;
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                StopTaskCounter.getInstance().decrement(getTaskId());
                this.isStopped = true;
            }
        }
    }

    private boolean needToStop(StepInstanceDTO stepInstance) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 刷新步骤状态
        stepInstance = taskInstanceService.getStepInstanceDetail(stepInstance.getId());
        // 如果任务处于“终止中”状态，触发任务终止
        if (taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
            if (RunStatusEnum.STOPPING.getValue().equals(stepInstance.getStatus())
                || RunStatusEnum.STOP_SUCCESS.getValue().equals(stepInstance.getStatus())) {
                // 已经发送过停止命令的就不再重复发送了
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public void execute() {
        if (needToStop(stepInstance)) {
            filePrepareService.stopPrepareFile(stepInstance.getId());
            setDoneStatus();
        }
    }

    @Override
    public String getTaskId() {
        return "FilePrepareControlTask-" + stepInstance.getId() + "_" + stepInstance.getExecuteCount();
    }
}
