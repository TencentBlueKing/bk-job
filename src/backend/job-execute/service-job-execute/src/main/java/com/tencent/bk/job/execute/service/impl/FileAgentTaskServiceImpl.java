package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.FileAgentTaskDAO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
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
    public AgentTaskDTO getAgentTask(Long stepInstanceId, Integer executeCount, Integer batch,
                                     FileTaskModeEnum fileTaskMode, HostDTO host) {
        return fileAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount, batch, fileTaskMode,
            host.getHostId());
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
