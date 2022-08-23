package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileAgentTaskServiceImpl
    extends AbstractAgentTaskServiceImpl
    implements FileAgentTaskService {

    private final FileAgentTaskDAO fileAgentTaskDAO;
    private final GseTaskIpLogDAO gseTaskIpLogDAO;

    @Autowired
    public FileAgentTaskServiceImpl(FileAgentTaskDAO fileAgentTaskDAO,
                                    StepInstanceService stepInstanceService,
                                    HostService hostService,
                                    GseTaskIpLogDAO gseTaskIpLogDAO) {
        super(stepInstanceService, hostService);
        this.fileAgentTaskDAO = fileAgentTaskDAO;
        this.gseTaskIpLogDAO = gseTaskIpLogDAO;
    }

    @Override
    public void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        fileAgentTaskDAO.batchSaveAgentTasks(agentTasks);
    }

    @Override
    public void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        fileAgentTaskDAO.batchUpdateAgentTasks(agentTasks);
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        if (isStepInstanceRecordExist(stepInstanceId)) {
            return fileAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        } else {
            return gseTaskIpLogDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        }
    }

    @Override
    public List<AgentTaskResultGroupDTO> listAndGroupAgentTasks(StepInstanceBaseDTO stepInstance,
                                                                int executeCount,
                                                                Integer batch) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();

        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch,
            FileTaskModeEnum.DOWNLOAD);
        if (CollectionUtils.isEmpty(agentTasks)) {
            return resultGroups;
        }

        List<AgentTaskDetailDTO> agentTaskDetailList = fillHostDetail(stepInstance, agentTasks);

        resultGroups = groupAgentTasks(agentTaskDetailList);

        return resultGroups.stream().sorted().collect(Collectors.toList());
    }

    @Override
    public List<AgentTaskResultGroupBaseDTO> listResultGroups(long stepInstanceId,
                                                              int executeCount,
                                                              Integer batch) {
        List<AgentTaskResultGroupBaseDTO> resultGroups;
        resultGroups = fileAgentTaskDAO.listResultGroups(stepInstanceId, executeCount, batch);
        if (CollectionUtils.isEmpty(resultGroups)) {
            // 兼容历史数据
            resultGroups = gseTaskIpLogDAO.listResultGroups(stepInstanceId, executeCount);
        }
        return resultGroups;
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer status,
                                                                     String tag) {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status);
        if (CollectionUtils.isEmpty(agentTasks)) {
            // 兼容历史数据
            agentTasks = gseTaskIpLogDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount, status, tag);
        }
        return fillHostDetail(stepInstance, agentTasks);
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer status,
                                                                     String tag,
                                                                     Integer limit,
                                                                     String orderField,
                                                                     Order order) {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status, limit, orderField, order);
        if (CollectionUtils.isEmpty(agentTasks)) {
            // 兼容历史数据
            agentTasks = gseTaskIpLogDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount, status, tag,
                limit, orderField, order);
        }
        return fillHostDetail(stepInstance, agentTasks);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount, Integer batch,
                                             FileTaskModeEnum fileTaskMode) {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch,
            fileTaskMode);
        // 兼容历史数据
        if (CollectionUtils.isEmpty(agentTasks)) {
            agentTasks = gseTaskIpLogDAO.listAgentTasks(stepInstanceId, executeCount);
            if (CollectionUtils.isNotEmpty(agentTasks) && fileTaskMode != null) {
                agentTasks = agentTasks.stream().filter(agentTask -> agentTask.getFileTaskMode() == fileTaskMode)
                    .collect(Collectors.toList());
            }
        }

        return agentTasks;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        return fileAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
    }

    @Override
    public AgentTaskDTO getAgentTaskByHost(StepInstanceDTO stepInstance, Integer executeCount, Integer batch,
                                           FileTaskModeEnum fileTaskMode, HostDTO host) {
        AgentTaskDTO agentTask = null;
        Long hostId = host.getHostId();
        if (hostId != null) {
            // 根据hostId查询
            agentTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstance.getId(), executeCount, batch,
                fileTaskMode, hostId);
        } else if (StringUtils.isNotEmpty(host.toCloudIp())) {
            // 根据ip查询的模式，有两种情况，数据可能在gse_file_agent_task/gse_task_ip_log表中，优先查询gse_file_agent_task
            HostDTO queryHost = getStepHostByIp(stepInstance, host.toCloudIp());
            if (queryHost != null) {
                agentTask = fileAgentTaskDAO.getAgentTaskByHostId(stepInstance.getId(), executeCount, batch,
                    fileTaskMode, queryHost.getHostId());
            } else {
                // 根据ip查询gse_task_ip_log表中的数据
                agentTask = gseTaskIpLogDAO.getAgentTaskByIp(stepInstance.getId(), executeCount, host.toCloudIp());
            }
        }
        return agentTask;
    }

    private HostDTO getStepHostByIp(StepInstanceDTO stepInstance, String cloudIp) {
        HostDTO queryHost = stepInstance.getTargetServers().getIpList().stream()
            .filter(targetHost -> cloudIp.equals(targetHost.toCloudIp()))
            .findFirst()
            .orElse(null);
        if (queryHost == null) {
            if (CollectionUtils.isNotEmpty(stepInstance.getResolvedFileSourceList())) {
                for (FileSourceDTO fileSource : stepInstance.getResolvedFileSourceList()) {
                    if (fileSource.getServers() != null
                        && CollectionUtils.isNotEmpty(fileSource.getServers().getIpList())) {
                        queryHost = fileSource.getServers().getIpList().stream()
                            .filter(sourceHost -> cloudIp.equals(sourceHost.toCloudIp()))
                            .findFirst()
                            .orElse(null);
                        if (queryHost != null) {
                            break;
                        }
                    }
                }
            }
        }

        return queryHost;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount, Integer batch) {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch, null);
        if (CollectionUtils.isEmpty(agentTasks)) {
            // 兼容历史数据
            agentTasks = gseTaskIpLogDAO.listAgentTasks(stepInstanceId, executeCount);
        }
        return agentTasks;
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, String cloudIp) {
        // 兼容历史数据
        return gseTaskIpLogDAO.getActualSuccessExecuteCount(stepInstanceId, cloudIp);
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch);
        return fillHostDetail(stepInstance, agentTasks);
    }

    private boolean isStepInstanceRecordExist(long stepInstanceId) {
        return fileAgentTaskDAO.isStepInstanceRecordExist(stepInstanceId);
    }

    @Override
    public void updateActualExecuteCount(long stepInstanceId, Integer batch, int actualExecuteCount) {
        fileAgentTaskDAO.updateActualExecuteCount(stepInstanceId, batch, actualExecuteCount);
    }
}
