package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ExecuteObjectTaskDetail;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceExecuteObjectService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 执行对象任务公共实现 Service
 */
public abstract class AbstractExecuteObjectTaskServiceImpl implements ExecuteObjectTaskService {
    protected final StepInstanceService stepInstanceService;
    protected final TaskInstanceExecuteObjectService taskInstanceExecuteObjectService;

    public AbstractExecuteObjectTaskServiceImpl(StepInstanceService stepInstanceService,
                                                TaskInstanceExecuteObjectService taskInstanceExecuteObjectService) {
        this.stepInstanceService = stepInstanceService;
        this.taskInstanceExecuteObjectService = taskInstanceExecuteObjectService;
    }

    protected final List<ExecuteObjectTaskDetail> fillExecuteObjectDetail(StepInstanceBaseDTO stepInstance,
                                                                          List<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyList();
        }

        List<ExecuteObjectTaskDetail> taskList;
        boolean hasExecuteObjectId = tasks.stream().anyMatch(task -> task.getExecuteObjId() != null);
        if (hasExecuteObjectId) {
            taskList = tasks
                .stream()
                .map(ExecuteObjectTaskDetail::new)
                .collect(Collectors.toList());

            long taskInstanceId = stepInstance.getTaskInstanceId();
            Map<Long, ExecuteObject> executeObjectMap = new HashMap<>();
            List<ExecuteObject> executeObjects;
            if (tasks.size() > 1000) {
                executeObjects = taskInstanceExecuteObjectService.listExecuteObjectsByJobInstanceId(taskInstanceId);
            } else {
                // in 查询控制个数在 1000 以内
                Set<Long> executeObjectIds =
                    tasks.stream().map(ExecuteObjectTask::getExecuteObjId).collect(Collectors.toSet());
                executeObjects = taskInstanceExecuteObjectService.listExecuteObjectsByIds(executeObjectIds);
            }
            if (CollectionUtils.isEmpty(executeObjects)) {
                return taskList;
            }
            executeObjects.forEach(executeObject -> executeObjectMap.put(executeObject.getId(), executeObject));

            taskList.forEach(
                task -> task.setExecuteObject(executeObjectMap.get(task.getExecuteObjId())));
        } else {
            // 兼容老版本不支持执行对象的数据
            Map<Long, HostDTO> hosts = stepInstanceService.computeStepHosts(stepInstance, HostDTO::getHostId);
            taskList = tasks
                .stream()
                .map(task -> {
                    ExecuteObjectTaskDetail taskDetail = new ExecuteObjectTaskDetail(task);
                    HostDTO host = hosts.get(task.getHostId());
                    ExecuteObject executeObject = new ExecuteObject(null, host);
                    taskDetail.setExecuteObject(executeObject);
                    return taskDetail;
                }).collect(Collectors.toList());
        }
        return taskList;
    }

    protected final List<ResultGroupDTO> groupTasks(List<ExecuteObjectTaskDetail> tasks) {
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


}
