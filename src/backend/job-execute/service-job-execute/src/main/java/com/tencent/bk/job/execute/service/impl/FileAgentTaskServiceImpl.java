package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
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

    @Autowired
    public FileAgentTaskServiceImpl(FileAgentTaskDAO fileAgentTaskDAO,
                                    StepInstanceService stepInstanceService,
                                    HostService hostService) {
        super(stepInstanceService, hostService);
        this.fileAgentTaskDAO = fileAgentTaskDAO;
    }

    @Override
    public void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        fileAgentTaskDAO.batchSaveAgentTasks(agentTasks);
    }

    @Override
    public void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        fileAgentTaskDAO.batchUpdateAgentTasks(agentTasks);
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        return fileAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
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
    public List<AgentTaskDetailDTO> listAgentTaskDetailByResultGroup(StepInstanceBaseDTO stepInstance,
                                                                     Integer executeCount,
                                                                     Integer batch,
                                                                     Integer status,
                                                                     String tag) {
        List<AgentTaskDTO> agentTasks = fileAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status);
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
        return fillHostDetail(stepInstance, agentTasks);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount, Integer batch,
                                             FileTaskModeEnum fileTaskMode) {
        return fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch, fileTaskMode);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        return fileAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
    }

    @Override
    public AgentTaskDTO getAgentTaskByHost(StepInstanceDTO stepInstance, Integer executeCount, Integer batch,
                                           FileTaskModeEnum fileTaskMode, HostDTO host) {
        Long hostId = host.getHostId();
        if (hostId == null) {
            // 根据ip反查hostId
            HostDTO queryHost = getStepHostByIp(stepInstance, host.toCloudIp());
            if (queryHost != null) {
                hostId = queryHost.getHostId();
            }
        }
        if (hostId == null) {
            return null;
        }
        return fileAgentTaskDAO.getAgentTaskByHostId(stepInstance.getId(), executeCount, batch,
            fileTaskMode, hostId);
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
        return fileAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch, null);
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, FileTaskModeEnum mode, HostDTO host) {
        return fileAgentTaskDAO.getActualSuccessExecuteCount(stepInstanceId, batch, mode, host.getHostId());
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch);
        return fillHostDetail(stepInstance, agentTasks);
    }
}
