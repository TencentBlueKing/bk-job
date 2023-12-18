package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.ScriptAgentTaskDAO;
import com.tencent.bk.job.execute.dao.ScriptExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ExecuteObjectTaskDetail;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceExecuteObjectService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ScriptExecuteObjectTaskServiceImpl
    extends AbstractExecuteObjectTaskServiceImpl
    implements ScriptExecuteObjectTaskService {

    private final ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO;
    private final ScriptAgentTaskDAO scriptAgentTaskDAO;

    @Autowired
    public ScriptExecuteObjectTaskServiceImpl(ScriptExecuteObjectTaskDAO scriptExecuteObjectTaskDAO,
                                              StepInstanceService stepInstanceService,
                                              ScriptAgentTaskDAO scriptAgentTaskDAO,
                                              TaskInstanceExecuteObjectService taskInstanceExecuteObjectService) {
        super(stepInstanceService, taskInstanceExecuteObjectService);
        this.scriptExecuteObjectTaskDAO = scriptExecuteObjectTaskDAO;
        this.scriptAgentTaskDAO = scriptAgentTaskDAO;
    }

    @Override
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);

        if (Objects.requireNonNull(anyTask).getExecuteObjId() != null) {
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
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);
        if (Objects.requireNonNull(anyTask).getExecuteObjId() != null) {
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
        if (isUsingExecuteObject(stepInstance)) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasks(stepInstanceId, executeCount, batch);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch);
        }
        return executeObjectTasks;
    }

    /**
     * 判断是否使用执行对象的方式存储
     *
     * @param stepInstance 步骤实例
     */
    private boolean isUsingExecuteObject(StepInstanceBaseDTO stepInstance) {
        ServersDTO servers = stepInstance.getTargetServers();
        if (CollectionUtils.isNotEmpty(servers.getExecuteObjects())) {
            ExecuteObject executeObject = servers.getExecuteObjects().stream().findAny().orElse(null);
            return Objects.requireNonNull(executeObject).getId() != null;
        } else {
            return false;
        }
    }

    @Override
    public List<ExecuteObjectTask> listTasksByGseTaskId(StepInstanceBaseDTO stepInstance, Long gseTaskId) {
        List<ExecuteObjectTask> executeObjectTasks;

        if (isUsingExecuteObject(stepInstance)) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByGseTaskId(gseTaskId);
        } else {
            // 兼容老版本数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
        }
        return executeObjectTasks;
    }

    @Override
    public ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceBaseDTO stepInstance,
                                                                Integer executeCount,
                                                                Integer batch,
                                                                ExecuteObjectCompositeKey executeObjectCompositeKey) {
        ExecuteObjectTask executeObjectTask = null;
        long stepInstanceId = stepInstance.getId();

        if (isUsingExecuteObject(stepInstance)) {
            String executeObjectResourceId = executeObjectCompositeKey.getResourceId();
            if (executeObjectResourceId != null) {
                ExecuteObject executeObject = taskInstanceExecuteObjectService.getExecuteObject(
                    executeObjectCompositeKey.getExecuteObjectType(), executeObjectCompositeKey.getResourceId());
                if (executeObject == null) {
                    return null;
                }
                executeObjectTask = scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstanceId, executeCount,
                    batch, executeObject.getId());
            } else {
                // 兼容使用<云区域+ip> 的方式查询主机执行任务
                ExecuteObject executeObject = getStepHostExecuteObjectByCloudIp(
                    stepInstance, executeObjectCompositeKey.getCloudIp());
                executeObjectTask = scriptExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstance.getId(),
                    executeCount, batch, executeObject.getId());
            }
        } else {
            // 兼容老版本不使用执行对象的场景(仅支持主机）
            Long hostId = executeObjectCompositeKey.getHostId();
            if (hostId != null) {
                executeObjectTask = scriptAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                    batch, hostId);
            } else {
                // 兼容使用<云区域+ip> 的方式查询主机执行任务
                HostDTO host = getStepHostByCloudIp(stepInstance, executeObjectCompositeKey.getCloudIp());
                if (host != null) {
                    executeObjectTask = scriptAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                        batch, host.getHostId());
                }
            }

        }
        return executeObjectTask;
    }

    private ExecuteObject getStepHostExecuteObjectByCloudIp(StepInstanceBaseDTO stepInstance, String cloudIp) {
        return stepInstance.getTargetServers().getExecuteObjects()
            .stream()
            .filter(executeObject -> {
                HostDTO host = executeObject.getHost();
                if (host == null) {
                    return false;
                }
                return cloudIp.equals(host.toCloudIp());
            })
            .findFirst()
            .orElse(null);
    }

    private HostDTO getStepHostByCloudIp(StepInstanceBaseDTO stepInstance,
                                         String cloudIp) {
        return stepInstance.getTargetServers().getIpList()
            .stream()
            .filter(targetHost -> cloudIp.equals(targetHost.toCloudIp()))
            .findFirst()
            .orElse(null);
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

        List<ExecuteObjectTaskDetail> agentTaskDetailList = fillExecuteObjectDetail(stepInstance, executeObjectTasks);
        resultGroups = groupTasks(agentTaskDetailList);

        return resultGroups.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<ResultGroupBaseDTO> listResultGroups(StepInstanceBaseDTO stepInstance,
                                                     int executeCount,
                                                     Integer batch) {
        List<ResultGroupBaseDTO> resultGroups;
        long stepInstanceId = stepInstance.getId();

        if (isUsingExecuteObject(stepInstance)) {
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

        if (isUsingExecuteObject(stepInstance)) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(stepInstance.getId(),
                executeCount, batch, status, tag);
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getId(), executeCount, batch, status, tag);
        }

        return fillExecuteObjectDetail(stepInstance, executeObjectTasks);
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

        if (isUsingExecuteObject(stepInstance)) {
            executeObjectTasks = scriptExecuteObjectTaskDAO.listTasksByResultGroup(stepInstance.getId(),
                executeCount, batch, status, tag, limit, orderField, order);
        } else {
            // 兼容历史数据
            executeObjectTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount,
                batch, status, tag, limit, orderField, order);
        }

        return fillExecuteObjectDetail(stepInstance, executeObjectTasks);
    }

    @Override
    public List<ExecuteObjectTaskDetail> listTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<ExecuteObjectTask> executeObjectTasks = listTasks(stepInstance, executeCount, batch);
        return fillExecuteObjectDetail(stepInstance, executeObjectTasks);
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
        if (isUsingExecuteObject(stepInstance)) {
            scriptExecuteObjectTaskDAO.updateTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        } else {
            // 兼容老版本方式
            scriptAgentTaskDAO.updateAgentTaskFields(stepInstance.getId(), executeCount, batch,
                actualExecuteCount, gseTaskId);
        }
    }
}
