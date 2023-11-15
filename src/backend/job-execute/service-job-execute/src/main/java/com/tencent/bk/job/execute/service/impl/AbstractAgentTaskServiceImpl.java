package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.cc.sdk.BkNetClient;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 任务Service的公共实现
 */
public abstract class AbstractAgentTaskServiceImpl implements AgentTaskService {
    private final StepInstanceService stepInstanceService;

    public AbstractAgentTaskServiceImpl(StepInstanceService stepInstanceService) {
        this.stepInstanceService = stepInstanceService;
    }

    protected final List<AgentTaskDetailDTO> fillHostDetail(StepInstanceBaseDTO stepInstance,
                                                            List<AgentTaskDTO> agentTasks) {
        if (CollectionUtils.isEmpty(agentTasks)) {
            return Collections.emptyList();
        }

        List<AgentTaskDetailDTO> agentTaskDetailList;
        boolean hasIpInfo = agentTasks.stream().anyMatch(agentTask -> StringUtils.isNotEmpty(agentTask.getCloudIp()));
        if (!hasIpInfo) {
            // 从当前版本开始AgentTask不会包含主机详细信息（ip\云区域等），需要从StepInstance反查
            Map<Long, HostDTO> hosts = stepInstanceService.computeStepHosts(stepInstance, HostDTO::getHostId);
            agentTaskDetailList = agentTasks
                .stream()
                .map(agentTask -> {
                    AgentTaskDetailDTO agentTaskDetail = new AgentTaskDetailDTO(agentTask);
                    HostDTO host = hosts.get(agentTask.getHostId());
                    agentTaskDetail.setCloudIp(host.toCloudIp());
                    agentTaskDetail.setBkCloudId(host.getBkCloudId());
                    agentTaskDetail.setIp(host.getIp());
                    agentTaskDetail.setIpv6(host.getIpv6());
                    agentTaskDetail.setBkCloudName(BkNetClient.getCloudAreaNameFromCache(host.getBkCloudId()));
                    return agentTaskDetail;
                }).collect(Collectors.toList());
        } else {
            // 历史版本AgentTask会包含ipv4信息
            agentTaskDetailList = agentTasks
                .stream()
                .map(AgentTaskDetailDTO::new)
                .collect(Collectors.toList());
            agentTaskDetailList.forEach(
                agentTaskDetail -> agentTaskDetail.setBkCloudName(
                    BkNetClient.getCloudAreaNameFromCache(agentTaskDetail.getBkCloudId())));
        }
        return agentTaskDetailList;
    }

    protected final List<AgentTaskResultGroupDTO> groupAgentTasks(List<AgentTaskDetailDTO> agentTasks) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();
        agentTasks.stream()
            .collect(Collectors.groupingBy(
                agentTask -> new AgentTaskResultGroupDTO(agentTask.getStatus().getValue(), agentTask.getTag())))
            .forEach((resultGroup, groupedAgentTasks) -> {
                resultGroup.setTotalAgentTasks(groupedAgentTasks.size());
                resultGroup.setAgentTasks(groupedAgentTasks);
                resultGroups.add(resultGroup);
            });
        return resultGroups;
    }


}
