package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 执行对象任务公共实现 Service
 */
public abstract class AbstractExecuteObjectTaskServiceImpl implements ExecuteObjectTaskService {
    protected final StepInstanceService stepInstanceService;

    public AbstractExecuteObjectTaskServiceImpl(StepInstanceService stepInstanceService) {
        this.stepInstanceService = stepInstanceService;
    }

    protected final void fillExecuteObjectForExecuteObjectTasks(
        StepInstanceBaseDTO stepInstance,
        List<ExecuteObjectTask> tasks
    ) {

        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        if (stepInstance.isSupportExecuteObject()) {
            Map<String, ExecuteObject> executeObjectMap =
                stepInstanceService.computeStepExecuteObjects(stepInstance, ExecuteObject::getId);
            tasks.forEach(
                task -> task.setExecuteObject(executeObjectMap.get(task.getExecuteObjectId())));
        } else {
            // 兼容老版本不支持执行对象的数据
            Map<Long, HostDTO> hosts = stepInstanceService.computeStepHosts(stepInstance, HostDTO::getHostId);
            tasks.forEach(task -> {
                HostDTO host = hosts.get(task.getHostId());
                ExecuteObject executeObject = new ExecuteObject(host);
                task.setExecuteObject(executeObject);
            });
        }
    }

    protected final void fillExecuteObjectForExecuteObjectTask(
        StepInstanceBaseDTO stepInstance,
        ExecuteObjectTask task
    ) {
        ExecuteObject executeObject;
        if (stepInstance.isSupportExecuteObject()) {
            executeObject = stepInstance.findExecuteObjectByCompositeKey(
                ExecuteObjectCompositeKey.ofExecuteObjectId(task.getExecuteObjectId()));
        } else {
            executeObject = stepInstance.findExecuteObjectByCompositeKey(
                ExecuteObjectCompositeKey.ofHostId(task.getHostId()));
        }
        task.setExecuteObject(executeObject);
    }

    protected final List<ResultGroupDTO> groupTasks(List<ExecuteObjectTask> tasks) {
        List<ResultGroupDTO> resultGroups = new ArrayList<>();
        tasks.stream()
            .collect(Collectors.groupingBy(
                task -> new ResultGroupDTO(task.getStatus().getValue(), task.getTag())))
            .forEach((resultGroup, groupedExecuteObjectTasks) -> {
                resultGroup.setTotal(groupedExecuteObjectTasks.size());
                resultGroup.setExecuteObjectTasks(groupedExecuteObjectTasks);
                resultGroups.add(resultGroup);
            });
        return resultGroups;
    }

    /**
     * 判断任务是否需要按照<执行对象>的方式保存到 DB
     *
     * @param tasks 需要被保存的任务
     */
    protected boolean isSaveTasksUsingExecuteObjectMode(Collection<ExecuteObjectTask> tasks) {
        // 根据执行对象任务模型中的 executeObjectId 参数判断是否支持执行对象
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);
        return Objects.requireNonNull(anyTask).getExecuteObjectId() != null;
    }

}
