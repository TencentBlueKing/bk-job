package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 执行对象任务公共实现 Service
 */
@Slf4j
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

        if (stepInstance.isSupportExecuteObjectFeature()) {
            Map<String, ExecuteObject> executeObjectMap =
                stepInstanceService.computeStepExecuteObjects(stepInstance, ExecuteObject::getId);
            tasks.forEach(task -> {
                ExecuteObject executeObject = executeObjectMap.get(task.getExecuteObjectId());
                if (executeObject == null) {
                    log.error("Can not find execute object for execute object task! stepInstanceId: {}, " +
                        "executeObjectId: {}", stepInstance.getId(), task.getExecuteObjectId());
                }
                task.setExecuteObject(executeObject);
            });
        } else {
            // 兼容老版本不支持执行对象的数据
            Map<Long, ExecuteObject> hostExecuteObjects = stepInstanceService.computeStepExecuteObjects(
                stepInstance, executeObject -> executeObject.getHost().getHostId());
            tasks.forEach(task -> {
                ExecuteObject hostExecuteObject = hostExecuteObjects.get(task.getHostId());
                if (hostExecuteObject == null) {
                    log.error("Can not find host for execute object task! stepInstanceId: {}, " +
                        "hostId: {}", stepInstance.getId(), task.getHostId());
                }
                task.setExecuteObject(hostExecuteObject);
            });
        }
    }

    protected final void fillExecuteObjectForExecuteObjectTask(StepInstanceBaseDTO stepInstance,
                                                               ExecuteObjectTask task) {
        if (task != null) {
            fillExecuteObjectForExecuteObjectTasks(stepInstance, Collections.singletonList(task));
        }
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
    protected boolean isExecuteObjectSupported(Collection<ExecuteObjectTask> tasks) {
        // 根据执行对象任务模型中的 executeObjectId 参数判断是否支持执行对象
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);
        return Objects.requireNonNull(anyTask).getExecuteObjectId() != null;
    }

}
