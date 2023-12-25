package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.ScriptAgentTaskDAO;
import com.tencent.bk.job.execute.dao.ScriptExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ExecuteObjectTaskDetail;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScriptExecuteObjectTaskServiceImpl
    extends AbstractExecuteObjectTaskServiceImpl
    implements ScriptExecuteObjectTaskService {

    private final ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO;
    private final ScriptAgentTaskDAO scriptAgentTaskDAO;

    @Autowired
    public ScriptExecuteObjectTaskServiceImpl(StepInstanceService stepInstanceService,
                                              ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO,
                                              ScriptAgentTaskDAO scriptAgentTaskDAO) {
        super(stepInstanceService);
        this.scriptExecuteObjectTaskDAO = scriptExecuteObjectTaskDAO;
        this.scriptAgentTaskDAO = scriptAgentTaskDAO;
    }

    @Override
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        if (isSaveTasksUsingExecuteObjectMode(tasks)) {
            scriptExecuteObjectTaskDAO.batchSaveTasks(tasks);
        } else {
            scriptAgentTaskDAO.batchSaveAgentTasks(tasks);
        }
    }

    @Override
    public void batchUpdateTasks(Collection<? extends ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        if (isSaveTasksUsingExecuteObjectMode(tasks)) {
            scriptExecuteObjectTaskDAO.batchUpdateTasks(tasks);
        } else {
            scriptAgentTaskDAO.batchUpdateAgentTasks(tasks);
        }
    }

    @Override
    public int getSuccessTaskCount(long stepInstanceId, int executeCount) {
        if (isStepInstanceRecordExist(stepInstanceId)) {
            return scriptExecuteObjectTaskDAO.getSuccessTaskCount(stepInstanceId, executeCount);
        } else {
            return scriptAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        }
    }

    @Override
    public List<ExecuteObjectTask> listTasks(StepInstanceBaseDTO stepInstance, Integer executeCount, Integer batch) {
        List<ExecuteObjectTask> executeObjectTasks;
        long stepInstanceId = stepInstance.getId();
        if (stepInstance.isSupportExecuteObject()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasks(stepInstanceId, executeCount, batch);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch);
        }
        return executeObjectTasks;
    }

    @Override
    public List<ExecuteObjectTaskDetail> listTasksByGseTaskId(StepInstanceBaseDTO stepInstance, Long gseTaskId) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObject()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByGseTaskId(gseTaskId);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
        }
        return convertToExecuteObjectDetailList(stepInstance, executeObjectTasks);
    }

    @Override
    public ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceBaseDTO stepInstance,
                                                                Integer executeCount,
                                                                Integer batch,
                                                                ExecuteObjectCompositeKey executeObjectCompositeKey) {
        ExecuteObject executeObject =
            stepInstance.findExecuteObjectByCompositeKey(executeObjectCompositeKey);
        if (executeObject == null) {
            return null;
        }

        long stepInstanceId = stepInstance.getId();
        ExecuteObjectTask executeObjectTask;
        if (stepInstance.isSupportExecuteObject()) {
            executeObjectTask = scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstanceId, executeCount,
                batch, executeObject.getId());
        } else {
            // 兼容老版本不使用执行对象的场景(仅支持主机）
            Long hostId = executeObject.getHost().getHostId();
            executeObjectTask = scriptAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                batch, hostId);
        }
        return executeObjectTask;
    }

    @Override
    public List<ResultGroupDTO> listAndGroupTasks(StepInstanceBaseDTO stepInstance,
                                                  int executeCount,
                                                  Integer batch) {
        List<ResultGroupDTO> resultGroups = new ArrayList<>();
        List<ExecuteObjectTask> executeObjectTasks = listTasks(stepInstance, executeCount, batch);
        if (CollectionUtils.isEmpty(executeObjectTasks)) {
            return resultGroups;
        }

        List<ExecuteObjectTaskDetail> agentTaskDetailList = convertToExecuteObjectDetailList(stepInstance, executeObjectTasks);
        resultGroups = groupTasks(agentTaskDetailList);

        return resultGroups.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<ResultGroupBaseDTO> listResultGroups(StepInstanceBaseDTO stepInstance,
                                                     int executeCount,
                                                     Integer batch) {
        List<ResultGroupBaseDTO> resultGroups;
        long stepInstanceId = stepInstance.getId();

        if (stepInstance.isSupportExecuteObject()) {
            resultGroups = scriptExecuteObjectTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
        } else {
            // 兼容历史数据
            resultGroups = scriptAgentTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
        }
        return resultGroups;
    }

    @Override
    public List<ExecuteObjectTaskDetail> listTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer status,
                                                                     String tag) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObject()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(stepInstance.getId(),
                executeCount, batch, status, tag);
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getId(), executeCount, batch, status, tag);
        }

        return convertToExecuteObjectDetailList(stepInstance, executeObjectTasks);
    }


    @Override
    public List<ExecuteObjectTaskDetail> listTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer status,
                                                                     String tag,
                                                                     Integer limit,
                                                                     String orderField,
                                                                     Order order) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObject()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(stepInstance.getId(),
                executeCount, batch, status, tag, limit, orderField, order);
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount,
                batch, status, tag, limit, orderField, order);
        }

        return convertToExecuteObjectDetailList(stepInstance, executeObjectTasks);
    }

    @Override
    public List<ExecuteObjectTaskDetail> listTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<ExecuteObjectTask> executeObjectTasks = listTasks(stepInstance, executeCount, batch);
        return convertToExecuteObjectDetailList(stepInstance, executeObjectTasks);
    }

    private boolean isStepInstanceRecordExist(long stepInstanceId) {
        return scriptExecuteObjectTaskDAO.isStepInstanceRecordExist(stepInstanceId);
    }

    @Override
    public void updateTaskFields(StepInstanceBaseDTO stepInstance,
                                 int executeCount,
                                 Integer batch,
                                 Integer actualExecuteCount,
                                 Long gseTaskId) {
        if (stepInstance.isSupportExecuteObject()) {
            scriptExecuteObjectTaskDAO.updateTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        } else {
            // 兼容老版本方式
            scriptAgentTaskDAO.updateAgentTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        }
    }
}
