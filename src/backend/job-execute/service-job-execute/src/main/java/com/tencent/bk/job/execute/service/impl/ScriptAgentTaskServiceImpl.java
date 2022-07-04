package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
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
import org.apache.commons.lang3.StringUtils;
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
    private final GseTaskIpLogDAO gseTaskIpLogDAO;

    @Autowired
    public ScriptAgentTaskServiceImpl(ScriptAgentTaskDAO scriptAgentTaskDAO,
                                      StepInstanceService stepInstanceService,
                                      HostService hostService,
                                      GseTaskIpLogDAO gseTaskIpLogDAO) {
        super(stepInstanceService, hostService);
        this.scriptAgentTaskDAO = scriptAgentTaskDAO;
        this.gseTaskIpLogDAO = gseTaskIpLogDAO;
    }

    @Override
    public void batchSaveAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        long stepInstanceId = agentTasks.stream().findAny().map(AgentTaskDTO::getStepInstanceId)
            .orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR));
        if (usingNewTable(stepInstanceId)) {
            scriptAgentTaskDAO.batchSaveAgentTasks(agentTasks);
        } else {
            // 兼容实现，发布之后删除
            gseTaskIpLogDAO.batchSaveAgentTasks(agentTasks);
        }

    }

    private boolean usingNewTable(long stepInstanceId) {
        return false;
    }

    @Override
    public void batchUpdateAgentTasks(Collection<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return;
        }
        long stepInstanceId = agentTasks.stream().findAny().map(AgentTaskDTO::getStepInstanceId)
            .orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR));
        if (usingNewTable(stepInstanceId)) {
            scriptAgentTaskDAO.batchUpdateAgentTasks(agentTasks);
        } else {
            // 兼容实现，发布之后删除
            gseTaskIpLogDAO.batchUpdateAgentTasks(agentTasks);
        }
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        if (isStepInstanceRecordExist(stepInstanceId)) {
            return scriptAgentTaskDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        } else {
            return gseTaskIpLogDAO.getSuccessAgentTaskCount(stepInstanceId, executeCount);
        }
    }

    @Override
    public List<AgentTaskDTO> listAgentTasks(Long stepInstanceId, Integer executeCount, Integer batch) {
        List<AgentTaskDTO> agentTasks = scriptAgentTaskDAO.listAgentTasks(stepInstanceId, executeCount, batch);
        if (CollectionUtils.isEmpty(agentTasks)) {
            // 兼容历史数据
            agentTasks = gseTaskIpLogDAO.listAgentTasks(stepInstanceId, executeCount);
        }
        return agentTasks;
    }

    @Override
    public List<AgentTaskDTO> listAgentTasksByGseTaskId(Long gseTaskId) {
        return scriptAgentTaskDAO.listAgentTasksByGseTaskId(gseTaskId);
    }

    @Override
    public AgentTaskDTO getAgentTaskByHost(StepInstanceBaseDTO stepInstance,
                                           Integer executeCount,
                                           Integer batch,
                                           HostDTO host) {
        AgentTaskDTO agentTask = null;
        Long hostId = host.getHostId();
        if (hostId == null) {
            // 根据ip反查hostId
            String cloudIp = host.toCloudIp();
            hostId = stepInstance.getTargetServers().getIpList().stream()
                .filter(targetHost -> cloudIp.equals(targetHost.toCloudIp()))
                .map(HostDTO::getHostId)
                .findFirst()
                .orElse(null);
        }

        if (hostId != null) {
            agentTask = scriptAgentTaskDAO.getAgentTaskByHostId(stepInstance.getId(), executeCount, batch, hostId);
        } else if (StringUtils.isNotEmpty(host.getIp())) {
            // 兼容历史数据
            agentTask = gseTaskIpLogDAO.getAgentTaskByIp(stepInstance.getId(), executeCount, host.toCloudIp());
        }

        return agentTask;
    }

    @Override
    public int getActualSuccessExecuteCount(long stepInstanceId, Integer batch, HostDTO host) {
        if (isStepInstanceRecordExist(stepInstanceId)) {
            return scriptAgentTaskDAO.getActualSuccessExecuteCount(stepInstanceId, batch, host.getHostId());
        } else {
            // 兼容历史数据
            return gseTaskIpLogDAO.getActualSuccessExecuteCount(stepInstanceId, host.toCloudIp());
        }
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
        List<AgentTaskDTO> agentTasks = scriptAgentTaskDAO.listAgentTaskByResultGroup(stepInstance.getId(),
            executeCount, batch, status, tag, limit, orderField, order);
        if (CollectionUtils.isEmpty(agentTasks)) {
            // 兼容历史数据
            agentTasks = gseTaskIpLogDAO.listAgentTaskByResultGroup(stepInstance.getId(), executeCount, status, tag,
                limit, orderField, order);
        }
        return fillHostDetail(stepInstance, agentTasks);
    }

    @Override
    public List<AgentTaskDetailDTO> listAgentTaskDetail(StepInstanceBaseDTO stepInstance,
                                                        Integer executeCount,
                                                        Integer batch) {
        List<AgentTaskDTO> agentTasks = listAgentTasks(stepInstance.getId(), executeCount, batch);
        return fillHostDetail(stepInstance, agentTasks);
    }

    private boolean isStepInstanceRecordExist(long stepInstanceId) {
        return scriptAgentTaskDAO.isStepInstanceRecordExist(stepInstanceId);
    }
}
