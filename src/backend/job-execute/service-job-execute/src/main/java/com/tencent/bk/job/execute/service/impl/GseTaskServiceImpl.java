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
import com.tencent.bk.job.execute.dao.GseAgentTaskDAO;
import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.GseAgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.ResultGroupBaseDTO;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.GseTaskService;
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
public class GseTaskServiceImpl implements GseTaskService {
    private final TaskInstanceDAO taskInstanceDao;
    private final StepInstanceDAO stepInstanceDao;
    private final GseTaskDAO gseTaskDao;
    private final GseAgentTaskDAO gseAgentTaskDao;

    private final LogService logService;

    @Autowired
    public GseTaskServiceImpl(TaskInstanceDAO taskInstanceDao, StepInstanceDAO stepInstanceDao,
                              GseTaskDAO gseTaskDao, GseAgentTaskDAO gseAgentTaskDao, LogService logService) {
        this.taskInstanceDao = taskInstanceDao;
        this.stepInstanceDao = stepInstanceDao;
        this.gseTaskDao = gseTaskDao;
        this.gseAgentTaskDao = gseAgentTaskDao;
        this.logService = logService;
    }

    @Override
    public void saveGseTask(GseTaskDTO gseTask) {
        gseTaskDao.saveGseTask(gseTask);
    }

    @Override
    public GseTaskDTO getGseTask(long stepInstanceId, int executeCount, int batch) {
        return gseTaskDao.getGseTask(stepInstanceId, executeCount, batch);
    }

    @Override
    public void batchSaveGseAgentTasks(List<GseAgentTaskDTO> gseAgentTasks) {
        if (gseAgentTasks.size() == 0) {
            return;
        }
        if (gseAgentTasks.size() <= 1000) {
            gseAgentTaskDao.batchSaveGseAgentTasks(gseAgentTasks);
        } else {
            List<List<GseAgentTaskDTO>> batches = BatchUtil.buildBatchList(gseAgentTasks, 1000);
            batches.parallelStream().forEach(gseAgentTaskDao::batchSaveGseAgentTasks);
        }
    }

    @Override
    public void batchUpdateGseAgentTasks(long stepInstanceId, int executeCount, Collection<String> cloudAreaIdAndIps,
                                         Long startTime, Long endTime, IpStatus status) {
        if (cloudAreaIdAndIps == null || cloudAreaIdAndIps.size() == 0) {
            return;
        }
        gseAgentTaskDao.batchUpdateGseAgentTasks(stepInstanceId, executeCount, cloudAreaIdAndIps, startTime, endTime, status);
    }

    @Override
    public int getSuccessAgentTaskCount(long stepInstanceId, int executeCount) {
        return gseAgentTaskDao.getSuccessIpCount(stepInstanceId, executeCount);
    }

    @Override
    public List<GseAgentTaskDTO> listSuccessAgentGseTask(long stepInstanceId, int executeCount) {
        return gseAgentTaskDao.getSuccessGseTaskIp(stepInstanceId, executeCount);
    }

    @Override
    public List<AgentTaskResultGroupDTO> getGseAgentTaskStatInfo(long stepInstanceId, int executeCount) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();

        List<ResultGroupBaseDTO> baseResultGroups = gseAgentTaskDao.listResultGroups(stepInstanceId, executeCount);
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

        List<ResultGroupBaseDTO> baseResultGroups = gseAgentTaskDao.listResultGroups(stepInstanceId, executeCount);
        for (ResultGroupBaseDTO baseResultGroup : baseResultGroups) {
            AgentTaskResultGroupDTO resultGroup = new AgentTaskResultGroupDTO();
            resultGroup.setResultType(IpStatus.valueOf(baseResultGroup.getResultType()));
            resultGroup.setTag(baseResultGroup.getTag());
            resultGroup.setCount(baseResultGroup.getAgentTaskCount());
            List<GseAgentTaskDTO> gseTaskIpLogList = listGseAgentTasksByResultType(stepInstanceId, executeCount,
                baseResultGroup.getResultType(), baseResultGroup.getTag());
            List<IpDTO> ipList = new ArrayList<>();
            for (GseAgentTaskDTO gseTaskIpLog : gseTaskIpLogList) {
                ipList.add(IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()));
            }
            resultGroup.setIpList(ipList);
            resultGroups.add(resultGroup);
        }
        return resultGroups;
    }


    @Override
    public List<GseAgentTaskDTO> listGseAgentTasksByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                               String tag) {
        return gseAgentTaskDao.listAgentTaskByResultType(stepInstanceId, executeCount, resultType, tag);
    }

    @Override
    public List<GseAgentTaskDTO> getGseAgentTaskContentByResultType(Long stepInstanceId, Integer executeCount,
                                                                    Integer resultType, String tag) {
        List<GseAgentTaskDTO> ipLogByResultType = listGseAgentTasksByResultType(stepInstanceId, executeCount, resultType, tag);
        StepInstanceBaseDTO stepInstance = stepInstanceDao.getStepInstanceBase(stepInstanceId);
        if (stepInstance == null) {
            return Collections.emptyList();
        }
        for (GseAgentTaskDTO gseTaskIpLog : ipLogByResultType) {
            long startTime = System.currentTimeMillis();
            if (stepInstance.isScriptStep()) {
                ScriptIpLogContent scriptIpLogContent = logService.getScriptIpLogContent(stepInstanceId, executeCount
                    , IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()));
                gseTaskIpLog.setScriptLogContent(scriptIpLogContent == null ? "" : scriptIpLogContent.getContent());
            } else if (stepInstance.isFileStep()) {
                FileIpLogContent fileIpLogContent = logService.getFileIpLogContent(stepInstanceId, executeCount,
                    IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()),
                    FileDistModeEnum.DOWNLOAD.getValue());
                gseTaskIpLog.setScriptLogContent(fileIpLogContent == null ? "" : fileIpLogContent.getContent());
            }
            log.debug("stepInstanceId={}|ip={}|time={} ms", stepInstanceId, gseTaskIpLog.getCloudAreaAndIp(),
                (System.currentTimeMillis() - startTime));

        }

        return ipLogByResultType;
    }

    @Override
    public List<GseAgentTaskDTO> getGseAgentTask(Long stepInstanceId, Integer executeCount, boolean onlyTargetIp) {
        return gseAgentTaskDao.listGseAgentTasks(stepInstanceId, executeCount, onlyTargetIp);
    }

    @Override
    public GseAgentTaskDTO getGseAgentTask(Long stepInstanceId, Integer executeCount, String cloudAreaIdAndIp) {
        return gseAgentTaskDao.getGseAgentTaskByIp(stepInstanceId, executeCount, cloudAreaIdAndIp);
    }

    @Override
    public List<IpDTO> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount) {
        return gseAgentTaskDao.getTaskFileSourceIps(stepInstanceId, executeCount)
            .stream().map(IpDTO::fromCloudAreaIdAndIpStr).collect(Collectors.toList());
    }
}
