package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.ScriptAgentTaskDAO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
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
public class ScriptAgentTaskServiceImpl
    extends AbstractAgentTaskServiceImpl
    implements ScriptAgentTaskService {

    private final ScriptAgentTaskDAO scriptAgentTaskDAO;

    @Autowired
    public ScriptAgentTaskServiceImpl(ScriptAgentTaskDAO scriptAgentTaskDAO,
                                      StepInstanceService stepInstanceService,
                                      HostService hostService) {
        super(stepInstanceService, hostService);
        this.scriptAgentTaskDAO = scriptAgentTaskDAO;
    }

    @Override
    public void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        scriptAgentTaskDAO.batchSaveAgentTasks(agentTasks);
    }

    @Override
    public void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        scriptAgentTaskDAO.batchUpdateAgentTasks(agentTasks);
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        return scriptAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount, Integer batch) {
        return scriptAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch);
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        return scriptAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
    }

    @Override
    public AgentTaskDTO getAgentTaskByHost(Long stepInstanceId, Integer executeCount, Integer batch, HostDTO host) {
        return scriptAgentTaskDAO.getAgentTaskByHostId(stepInstanceId, executeCount, batch, host.getHostId());
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, HostDTO host) {
        return scriptAgentTaskDAO.getActualSuccessExecuteCount(stepInstanceId, batch, host.getHostId());
    }

    @Override
    public List<AgentTaskResultGroupDTO> listAndGroupAgentTasks(StepInstanceBaseDTO stepInstance,
                                                                int executeCount,
                                                                Integer batch) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();
        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch);
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
        List<AgentTaskDTO> agentTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status, tag);
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
        List<AgentTaskDTO> agentTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status, tag, limit, orderField, order);
        return fillHostDetail(stepInstance, agentTasks);
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch);
        return fillHostDetail(stepInstance, agentTasks);
    }
}
