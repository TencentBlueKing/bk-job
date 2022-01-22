/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.dao.AgentTaskDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentTaskServiceImpl implements AgentTaskService {

    private final StepInstanceDAO stepInstanceDAO;
    private final AgentTaskDAO agentTaskDAO;

    private final LogService logService;

    @Autowired
    public AgentTaskServiceImpl(StepInstanceDAO stepInstanceDAO,
                                AgentTaskDAO agentTaskDAO,
                                LogService logService) {
        this.stepInstanceDAO = stepInstanceDAO;
        this.agentTaskDAO = agentTaskDAO;
        this.logService = logService;
    }

    @Override
    public void batchSaveAgentTasks(List<AgentTaskDTO> agentTasks) {
        if (agentTasks.size() == 0) {
            return;
        }
        if (agentTasks.size() <= 1000) {
            agentTaskDAO.batchSaveAgentTasks(agentTasks);
        } else {
            List<List<AgentTaskDTO>> batches = BatchUtil.buildBatchList(agentTasks, 1000);
            batches.parallelStream().forEach(agentTaskDAO::batchSaveAgentTasks);
        }
    }

    @Override
    public void batchUpdateAgentTasks(long stepInstanceId, int executeCount, Collection<String> cloudAreaIdAndIps,
                                      Long startTime, Long endTime, IpStatus status) {
        if (cloudAreaIdAndIps == null || cloudAreaIdAndIps.size() == 0) {
            return;
        }
        agentTaskDAO.batchUpdateAgentTasks(stepInstanceId, executeCount, cloudAreaIdAndIps, startTime, endTime,
            status);
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        return agentTaskDAO.getSuccessIpCount(stepInstanceId, executeCount);
    }

    @Override
    public List<AgentTaskDTO> listSuccessAgentTasks(long stepInstanceId, int executeCount) {
        return agentTaskDAO.getSuccessAgentTasks(stepInstanceId, executeCount);
    }

    @Override
    public List<AgentTaskResultGroupDTO> getAgentTaskStatInfo(long stepInstanceId, int executeCount) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();

        List<ResultGroupBaseDTO> baseResultGroups = agentTaskDAO.listResultGroups(stepInstanceId, executeCount);
        for (ResultGroupBaseDTO baseResultGroup : baseResultGroups) {
            AgentTaskResultGroupDTO resultGroup = new AgentTaskResultGroupDTO();
            resultGroup.setResultType(IpStatus.valueOf(baseResultGroup.getResultType()));
            resultGroup.setTag(baseResultGroup.getTag());
            resultGroup.setCount(baseResultGroup.getAgentTaskCount());
            resultGroups.add(resultGroup);
        }
        return resultGroups;
    }

    @Override
    public List<AgentTaskResultGroupDTO> getLogStatInfoWithIp(long stepInstanceId, int executeCount) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();

        List<ResultGroupBaseDTO> baseResultGroups = agentTaskDAO.listResultGroups(stepInstanceId, executeCount);
        for (ResultGroupBaseDTO baseResultGroup : baseResultGroups) {
            AgentTaskResultGroupDTO resultGroup = new AgentTaskResultGroupDTO();
            resultGroup.setResultType(IpStatus.valueOf(baseResultGroup.getResultType()));
            resultGroup.setTag(baseResultGroup.getTag());
            resultGroup.setCount(baseResultGroup.getAgentTaskCount());
            List<AgentTaskDTO> agentTaskList = listAgentTasksByResultType(stepInstanceId, executeCount,
                baseResultGroup.getResultType(), baseResultGroup.getTag());
            List<IpDTO> ipList = new ArrayList<>();
            for (AgentTaskDTO agentTask : agentTaskList) {
                ipList.add(IpDTO.fromCloudAreaIdAndIpStr(agentTask.getCloudAreaAndIp()));
            }
            resultGroup.setIpList(ipList);
            resultGroups.add(resultGroup);
        }
        return resultGroups;
    }


    @Override
    public List<AgentTaskDTO> listAgentTasksByResultType(Long stepInstanceId, Integer executeCount,
                                                         Integer resultType,
                                                         String tag) {
        return agentTaskDAO.listAgentTaskByResultType(stepInstanceId, executeCount, resultType, tag);
    }

    @Override
    public List<AgentTaskDTO> getAgentTaskContentByResultType(Long stepInstanceId, Integer executeCount,
                                                              Integer resultType, String tag) {
        List<AgentTaskDTO> agentTaskByResultType = listAgentTasksByResultType(stepInstanceId, executeCount,
            resultType, tag);
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        if (stepInstance == null) {
            return Collections.emptyList();
        }
        for (AgentTaskDTO agentTask : agentTaskByResultType) {
            long startTime = System.currentTimeMillis();
            if (stepInstance.isScriptStep()) {
                ScriptIpLogContent scriptIpLogContent = logService.getScriptIpLogContent(stepInstanceId, executeCount
                    , IpDTO.fromCloudAreaIdAndIpStr(agentTask.getCloudAreaAndIp()));
                agentTask.setScriptLogContent(scriptIpLogContent == null ? "" : scriptIpLogContent.getContent());
            } else if (stepInstance.isFileStep()) {
                FileIpLogContent fileIpLogContent = logService.getFileIpLogContent(stepInstanceId, executeCount,
                    IpDTO.fromCloudAreaIdAndIpStr(agentTask.getCloudAreaAndIp()),
                    FileDistModeEnum.DOWNLOAD.getValue());
                agentTask.setScriptLogContent(fileIpLogContent == null ? "" : fileIpLogContent.getContent());
            }
            log.debug("stepInstanceId={}|ip={}|time={} ms", stepInstanceId, agentTask.getCloudAreaAndIp(),
                (System.currentTimeMillis() - startTime));

        }

        return agentTaskByResultType;
    }

    @Override
    public List<AgentTaskDTO> getAgentTask(Long stepInstanceId, Integer executeCount, boolean onlyTargetIp) {
        return agentTaskDAO.listAgentTasks(stepInstanceId, executeCount, onlyTargetIp);
    }

    @Override
    public AgentTaskDTO getAgentTask(Long stepInstanceId, Integer executeCount, String cloudAreaIdAndIp) {
        return agentTaskDAO.getAgentTaskByIp(stepInstanceId, executeCount, cloudAreaIdAndIp);
    }

    @Override
    public List<IpDTO> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount) {
        return agentTaskDAO.getTaskFileSourceIps(stepInstanceId, executeCount)
            .stream().map(IpDTO::fromCloudAreaIdAndIpStr).collect(Collectors.toList());
    }
}
