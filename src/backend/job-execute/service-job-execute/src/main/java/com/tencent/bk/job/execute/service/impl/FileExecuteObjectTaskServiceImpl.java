package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.dao.FileExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
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
public class FileExecuteObjectTaskServiceImpl
    extends AbstractExecuteObjectTaskServiceImpl
    implements FileExecuteObjectTaskService {

    private final FileExecuteObjectTaskDAO fileExecuteObjectTaskDAO;
    private final FileAgentTaskDAO fileAgentTaskDAO;
    private final TaskInstanceService taskInstanceService;

    @Autowired
    public FileExecuteObjectTaskServiceImpl(StepInstanceService stepInstanceService,
                                            FileExecuteObjectTaskDAO fileExecuteObjectTaskDAO,
                                            FileAgentTaskDAO fileAgentTaskDAO,
                                            TaskInstanceService taskInstanceService) {
        super(stepInstanceService);
        this.fileAgentTaskDAO = fileAgentTaskDAO;
        this.fileExecuteObjectTaskDAO = fileExecuteObjectTaskDAO;
        this.taskInstanceService = taskInstanceService;
    }

    @Override
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        if (isSaveTasksUsingExecuteObjectMode(tasks)) {
            fileExecuteObjectTaskDAO.batchSaveTasks(tasks);
        } else {
            fileAgentTaskDAO.batchSaveAgentTasks(tasks);
        }
    }

    @Override
    public void batchUpdateTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }

        if (isSaveTasksUsingExecuteObjectMode(tasks)) {
            fileExecuteObjectTaskDAO.batchUpdateTasks(tasks);
        } else {
            fileAgentTaskDAO.batchUpdateAgentTasks(tasks);
        }
    }

    @Override
    public int getSuccessTaskCount(long stepInstanceId, int executeCount) {
        if (isStepInstanceRecordExist(stepInstanceId)) {
            return fileExecuteObjectTaskDAO.getSuccessTaskCount(stepInstanceId, executeCount);
        } else {
            return fileAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        }
    }

    @Override
    public List<ExecuteObjectTask> listTasks(StepInstanceBaseDTO stepInstance, Integer executeCount, Integer batch) {
        return listTasks(stepInstance, executeCount, batch, null);
    }

    @Override
    public List<ExecuteObjectTask> listTasksByGseTaskId(StepInstanceBaseDTO stepInstance, Long gseTaskId) {
        List<ExecuteObjectTask> executeObjectTasks;
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = fileExecuteObjectTaskDAO.listTasksByGseTaskId(gseTaskId);
        } else {
            // 兼容老版本数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
        }

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }

    @Override
    public ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceBaseDTO stepInstance,
                                                                Integer executeCount,
                                                                Integer batch,
                                                                FileTaskModeEnum fileTaskMode,
                                                                ExecuteObjectCompositeKey executeObjectCompositeKey) {
        ExecuteObject executeObject =
            stepInstanceService.findExecuteObjectByCompositeKey(stepInstance, executeObjectCompositeKey);
        if (executeObject == null) {
            return null;
        }

        long stepInstanceId = stepInstance.getId();
        ExecuteObjectTask executeObjectTask;
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTask = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstanceId, executeCount,
                batch, fileTaskMode, executeObject.getId());
        } else {
            // 兼容老版本不使用执行对象的场景(仅支持主机）
            Long hostId = executeObject.getHost().getHostId();
            executeObjectTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                batch, fileTaskMode, hostId);
        }
        executeObjectTask.setExecuteObject(executeObject);
        return executeObjectTask;
    }

    @Override
    public List<ResultGroupDTO> listAndGroupTasks(StepInstanceBaseDTO stepInstance,
                                                  int executeCount,
                                                  Integer batch) {
        List<ResultGroupDTO> resultGroups = new ArrayList<>();
        List<ExecuteObjectTask> executeObjectTasks = listTasks(stepInstance, executeCount,
            batch, FileTaskModeEnum.DOWNLOAD);
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
            resultGroups = fileExecuteObjectTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
        } else {
            // 兼容历史数据
            resultGroups = fileAgentTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
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
            executeObjectTasks = fileExecuteObjectTaskDAO.listTaskByResultGroup(stepInstance.getId(),
                executeCount, batch, status);
        } else {
            // 兼容历史数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getId(), executeCount, batch, status);
        }

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }


    @Override
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
            executeObjectTasks = fileExecuteObjectTaskDAO.listTaskByResultGroup(stepInstance.getId(),
                executeCount, batch, status, limit, orderField, order);
        } else {
            // 兼容历史数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount,
                batch, status, limit, orderField, order);
        }

        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }

    private boolean isStepInstanceRecordExist(long stepInstanceId) {
        return fileExecuteObjectTaskDAO.isStepInstanceRecordExist(stepInstanceId);
    }

    @Override
    public void updateTaskFields(StepInstanceBaseDTO stepInstance,
                                 int executeCount,
                                 Integer batch,
                                 Integer actualExecuteCount,
                                 Long gseTaskId) {
        if (stepInstance.isSupportExecuteObjectFeature()) {
            fileExecuteObjectTaskDAO.updateTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        } else {
            // 兼容老版本方式
            fileAgentTaskDAO.updateAgentTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        }
    }

    @Override
    public List<ExecuteObjectTask> listTasks(StepInstanceBaseDTO stepInstance,
                                             Integer executeCount,
                                             Integer batch,
                                             FileTaskModeEnum fileTaskMode) {
        List<ExecuteObjectTask> executeObjectTasks;
        long stepInstanceId = stepInstance.getId();
        if (stepInstance.isSupportExecuteObjectFeature()) {
            executeObjectTasks = fileExecuteObjectTaskDAO.listTasks(stepInstanceId, executeCount, batch, fileTaskMode);
        } else {
            // 兼容老版本数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch, fileTaskMode);
        }
        fillExecuteObjectForExecuteObjectTasks(stepInstance, executeObjectTasks);
        return executeObjectTasks;
    }
}
