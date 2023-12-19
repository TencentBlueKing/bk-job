package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.dao.FileExecuteObjectTaskDAO;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.ExecuteObjectTaskDetail;
import com.tencent.bk.job.execute.model.ExecuteObjectsDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceExecuteObjectService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
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
public class FileExecuteObjectTaskServiceImpl
    extends AbstractExecuteObjectTaskServiceImpl
    implements FileExecuteObjectTaskService {

    private final FileExecuteObjectTaskDAO fileExecuteObjectTaskDAO;
    private final FileAgentTaskDAO fileAgentTaskDAO;

    @Autowired
    public FileExecuteObjectTaskServiceImpl(FileExecuteObjectTaskDAO fileExecuteObjectTaskDAO,
                                            StepInstanceService stepInstanceService,
                                            FileAgentTaskDAO fileAgentTaskDAO,
                                            TaskInstanceExecuteObjectService taskInstanceExecuteObjectService) {
        super(stepInstanceService, taskInstanceExecuteObjectService);
        this.fileAgentTaskDAO = fileAgentTaskDAO;
        this.fileExecuteObjectTaskDAO = fileExecuteObjectTaskDAO;
    }

    @Override
    public void batchSaveTasks(Collection<ExecuteObjectTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return;
        }
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);

        if (Objects.requireNonNull(anyTask).getExecuteObjId() != null) {
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
        ExecuteObjectTask anyTask = tasks.stream().findAny().orElse(null);
        if (Objects.requireNonNull(anyTask).getExecuteObjId() != null) {
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

    /**
     * 判断是否使用执行对象的方式存储
     *
     * @param stepInstance 步骤实例
     */
    private boolean isUsingExecuteObject(StepInstanceBaseDTO stepInstance) {
        ExecuteObjectsDTO servers = stepInstance.getTargetServers();
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
            executeObjectTasks = fileExecuteObjectTaskDAO.listTasksByGseTaskId(gseTaskId);
        } else {
            // 兼容老版本数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
        }
        return executeObjectTasks;
    }

    @Override
    public ExecuteObjectTask getTaskByExecuteObjectCompositeKey(StepInstanceDTO stepInstance,
                                                                Integer executeCount,
                                                                Integer batch,
                                                                FileTaskModeEnum fileTaskMode,
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
                executeObjectTask = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstanceId, executeCount,
                    batch, fileTaskMode, executeObject.getId());
            } else {
                // 兼容使用<云区域+ip> 的方式查询主机执行任务
                ExecuteObject executeObject = getStepHostExecuteObjectByCloudIp(
                    stepInstance, executeObjectCompositeKey.getCloudIp());
                executeObjectTask = fileExecuteObjectTaskDAO.getTaskByExecuteObjectId(stepInstance.getId(),
                    executeCount, batch, fileTaskMode, executeObject.getId());
            }
        } else {
            // 兼容老版本不使用执行对象的场景(仅支持主机）
            Long hostId = executeObjectCompositeKey.getHostId();
            if (hostId != null) {
                executeObjectTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                    batch, fileTaskMode, hostId);
            } else {
                // 兼容使用<云区域+ip> 的方式查询主机执行任务
                HostDTO host = getStepHostByCloudIp(stepInstance, executeObjectCompositeKey.getCloudIp());
                if (host != null) {
                    executeObjectTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount,
                        batch, fileTaskMode, host.getHostId());
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

    private HostDTO getStepHostByCloudIp(StepInstanceDTO stepInstance,
                                         String cloudIp) {
        HostDTO matchHost = stepInstance.getTargetServers().getIpList()
            .stream()
            .filter(host -> cloudIp.equals(host.toCloudIp()))
            .findFirst()
            .orElse(null);
        if (matchHost == null) {
            if (CollectionUtils.isNotEmpty(stepInstance.getFileSourceList())) {
                for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
                    if (fileSource.getServers() != null
                        && CollectionUtils.isNotEmpty(fileSource.getServers().getIpList())) {
                        matchHost = fileSource.getServers().getIpList().stream()
                            .filter(sourceHost -> cloudIp.equals(sourceHost.toCloudIp()))
                            .findFirst()
                            .orElse(null);
                        if (matchHost != null) {
                            break;
                        }
                    }
                }
            }
        }

        return matchHost;
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
            resultGroups = fileExecuteObjectTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
        } else {
            // 兼容历史数据
            resultGroups = fileAgentTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
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
            executeObjectTasks = fileExecuteObjectTaskDAO.listTaskByResultGroup(stepInstance.getId(),
                executeCount, batch, status);
        } else {
            // 兼容历史数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(
                stepInstance.getId(), executeCount, batch, status);
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
            executeObjectTasks = fileExecuteObjectTaskDAO.listTaskByResultGroup(stepInstance.getId(),
                executeCount, batch, status, limit, orderField, order);
        } else {
            // 兼容历史数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount,
                batch, status, limit, orderField, order);
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
        return fileExecuteObjectTaskDAO.isStepInstanceRecordExist(stepInstanceId);
    }

    @Override
    public void updateTaskFields(StepInstanceBaseDTO stepInstance,
                                 int executeCount,
                                 Integer batch,
                                 Integer actualExecuteCount,
                                 Long gseTaskId) {
        if (isUsingExecuteObject(stepInstance)) {
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
        if (isUsingExecuteObject(stepInstance)) {
            executeObjectTasks = fileExecuteObjectTaskDAO.listTasks(stepInstanceId, executeCount, batch, fileTaskMode);
        } else {
            // 兼容老版本数据
            executeObjectTasks = fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch, fileTaskMode);
        }
        return executeObjectTasks;
    }
}
