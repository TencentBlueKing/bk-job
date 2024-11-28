package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.execute.dao.ScriptAgentTaskDAO;
import com.tencent.bk.job.execute.dao.ScriptExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
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
    private final IdGen idGen;

    @Autowired
    public ScriptExecuteObjectTaskServiceImpl(StepInstanceService stepInstanceService,
                                              ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO,
                                              ScriptAgentTaskDAO scriptAgentTaskDAO,
                                              IdGen idGen) {
        super(stepInstanceService);
        this.scriptExecuteObjectTaskDAO = scriptExecuteObjectTaskDAO;
        this.scriptAgentTaskDAO = scriptAgentTaskDAO;
        this.idGen = idGen;
    }

    @Override
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        tasks.forEach(task -> task.setId(idGen.genGseScriptExecuteObjTaskId()));

        boolean executeObjectSupported = isExecuteObjectSupported(tasks);

        // 任务分批，避免大事务造成 db 主从延迟
        BatchUtil.executeBatch(
            tasks,
            2000,
            batchTasks -> executeSaveTasks(executeObjectSupported, batchTasks)
        );
    }

    private void executeSaveTasks(boolean executeObjectSupported,
                                  Collection<ExecuteObjectTask> tasks) {
        if (executeObjectSupported) {
            scriptExecuteObjectTaskDAO.batchSaveTasks(tasks);
        } else {
            scriptAgentTaskDAO.batchSaveAgentTasks(tasks);
        }
    }

    @Override
    public void batchUpdateTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        boolean executeObjectSupported = isExecuteObjectSupported(tasks);

        // 任务分批，避免大事务造成 db 主从延迟
        BatchUtil.executeBatch(
            tasks,
            2000,
            batchTasks -> executeUpdateTasks(executeObjectSupported, batchTasks)
        );
    }

    private void executeUpdateTasks(boolean executeObjectSupported,
                                    Collection<ExecuteObjectTask> tasks) {
        if (executeObjectSupported) {
            scriptExecuteObjectTaskDAO.batchUpdateTasks(tasks);
        } else {
            scriptAgentTaskDAO.batchUpdateAgentTasks(tasks);
        }
    }

    @Override
    public int getSuccessTaskCount(Long taskInstanceId, long stepInstanceId, int executeCount) {
        if (isStepInstanceRecordExist(taskInstanceId, stepInstanceId)) {
            return scriptExecuteObjectTaskDAO.getSuccessTaskCount(taskInstanceId, stepInstanceId, executeCount);
        } else {
            return scriptAgentTaskDAO.getSuccessAgentTaskCount(taskInstanceId, stepInstanceId, executeCount);
        }
    }

    @Override
    public List<ExecuteObjectTask> listTasks(StepInstanceBaseDTO stepInstance, Integer executeCount, Integer batch) {
        List<ExecuteObjectTask> executeObjectTasks;
        long stepInstanceId = stepInstance.getId();
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasks(stepInstance.getTaskInstanceId(),
                stepInstanceId, executeCount, batch);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasks(stepInstance.getTaskInstanceId(),
                stepInstanceId, executeCount, batch);
        }
        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }

    @Override
    public List<ExecuteObjectTask> listTasksByGseTaskId(StepInstanceBaseDTO stepInstance, Long gseTaskId) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByGseTaskId(
                stepInstance.getTaskInstanceId(), gseTaskId);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasksByGseTaskId(
                stepInstance.getTaskInstanceId(), gseTaskId);
        }
        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }

    @Override
    public ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceBaseDTO stepInstance,
                                                                Integer executeCount,
                                                                Integer batch,
                                                                ExecuteObjectCompositeKey executeObjectCompositeKey) {
        ExecuteObject executeObject =
            stepInstanceService.findExecuteObjectByCompositeKey(stepInstance, executeObjectCompositeKey);
        if (executeObject == null) {
            return null;
        }

        long stepInstanceId = stepInstance.getId();
        ExecuteObjectTask executeObjectTask;
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTask = scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(
                stepInstance.getTaskInstanceId(),
                stepInstanceId, executeCount,
                batch,
                executeObject.getId()
            );
        } else {
            // 兼容老版本不使用执行对象的场景(仅支持主机）
            Long hostId = executeObject.getHost().getHostId();
            executeObjectTask = scriptAgentTaskDAO.getAgentTaskByHostId(
                stepInstance.getTaskInstanceId(),
                stepInstanceId,
                executeCount,
                batch,
                hostId
            );
        }
        fillExecuteObjectForExecuteObjectTask(stepInstance, executeObjectTask);
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

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        resultGroups = groupTasks(executeObjectTasks);

        return resultGroups.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<ResultGroupBaseDTO> listResultGroups(StepInstanceBaseDTO stepInstance,
                                                     int executeCount,
                                                     Integer batch) {
        List<ResultGroupBaseDTO> resultGroups;
        long stepInstanceId = stepInstance.getId();

        if (stepInstance.isSupportExecuteObjectFeature()) {
            resultGroups = scriptExecuteObjectTaskDAO.listResultGroups(
                stepInstance.getTaskInstanceId(),
                stepInstanceId,
                executeCount,
                batch
            );
        } else {
            // 兼容历史数据
            resultGroups = scriptAgentTaskDAO.listResultGroups(stepInstance.getTaskInstanceId(),
                stepInstanceId, executeCount, batch);
        }
        return resultGroups;
    }

    @Override
    public List<ExecuteObjectTask> listTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         Integer status,
                                                         String tag) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(
                stepInstance.getTaskInstanceId(),
                stepInstance.getId(),
                executeCount,
                batch,
                status,
                tag
            );
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getTaskInstanceId(),
                stepInstance.getId(),
                executeCount,
                batch,
                status,
                tag
            );
        }

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }


    public List<ExecuteObjectTask> listTaskByResultGroup(StepInstanceBaseDTO stepInstance,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         Integer status,
                                                         String tag,
                                                         Integer limit,
                                                         String orderField,
                                                         Order order) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(
                stepInstance.getTaskInstanceId(),
                stepInstance.getId(),
                executeCount,
                batch,
                status,
                tag,
                limit,
                orderField,
                order
            );
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getTaskInstanceId(),
                stepInstance.getId(),
                executeCount,
                batch,
                status,
                tag,
                limit,
                orderField,
                order
            );
        }

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }

    private boolean isStepInstanceRecordExist(Long taskInstanceId, long stepInstanceId) {
        return scriptExecuteObjectTaskDAO.isStepInstanceRecordExist(taskInstanceId, stepInstanceId);
    }

    @Override
    public void updateTaskFields(StepInstanceBaseDTO stepInstance,
                                 int executeCount,
                                 Integer batch,
                                 Integer actualExecuteCount,
                                 Long gseTaskId) {
        if (stepInstance.isSupportExecuteObjectFeature()) {
            scriptExecuteObjectTaskDAO.updateTaskFields(stepInstance.getTaskInstanceId(), stepInstance.getId(),
                executeCount, batch, actualExecuteCount, gseTaskId);
        } else {
            // 兼容老版本方式
            scriptAgentTaskDAO.updateAgentTaskFields(stepInstance.getTaskInstanceId(), stepInstance.getId(),
                executeCount, batch, actualExecuteCount, gseTaskId);
        }
    }
}
