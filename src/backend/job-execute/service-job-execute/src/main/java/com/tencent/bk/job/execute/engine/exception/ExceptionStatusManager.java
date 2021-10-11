package com.tencent.bk.job.execute.engine.exception;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExceptionStatusManager {

    private final TaskInstanceService taskInstanceService;

    @Autowired
    public ExceptionStatusManager(TaskInstanceService taskInstanceService) {
        this.taskInstanceService = taskInstanceService;
    }

    public void setAbnormalStatusForStep(long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        taskInstanceService.updateStepStatus(stepInstanceId, RunStatusEnum.ABNORMAL_STATE.getValue());
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.ABNORMAL_STATE.getValue());
    }
}
