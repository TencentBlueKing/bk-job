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
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.dao.GseTaskIpLogDAO;
import com.tencent.bk.job.execute.dao.GseTaskLogDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.TaskInstanceDAO;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GseTaskLogServiceImpl implements GseTaskLogService {
    private final TaskInstanceDAO taskInstanceDao;
    private final StepInstanceDAO stepInstanceDao;
    private final GseTaskLogDAO gseTaskLogDao;
    private final GseTaskIpLogDAO gseTaskIpLogDao;

    private final LogService logService;

    @Autowired
    public GseTaskLogServiceImpl(TaskInstanceDAO taskInstanceDao, StepInstanceDAO stepInstanceDao,
                                 GseTaskLogDAO gseTaskLogDao, GseTaskIpLogDAO gseTaskIpLogDao, LogService logService) {
        this.taskInstanceDao = taskInstanceDao;
        this.stepInstanceDao = stepInstanceDao;
        this.gseTaskLogDao = gseTaskLogDao;
        this.gseTaskIpLogDao = gseTaskIpLogDao;
        this.logService = logService;
    }

    @Override
    public GseTaskLogDTO getTaskLastRetryLog(long taskInstanceId) {
        long currentStepId = taskInstanceDao.getTaskInstance(taskInstanceId).getCurrentStepId();
        return gseTaskLogDao.getStepLastExecuteLog(currentStepId);
    }

    @Override
    public void saveGseTaskLog(GseTaskLogDTO gseTaskLog) {
        gseTaskLogDao.saveGseTaskLog(gseTaskLog);
    }

    @Override
    public GseTaskLogDTO getGseTaskLog(long stepInstanceId, int executeCount) {
        return gseTaskLogDao.getGseTaskLog(stepInstanceId, executeCount);
    }

    @Override
    public void deleteGseTaskLog(long stepInstanceId, int executeCount) {
        gseTaskLogDao.deleteGseTaskLog(stepInstanceId, executeCount);
    }

    @Override
    public void clearAllIpLog(long stepInstanceId, int executeCount) {
        gseTaskIpLogDao.deleteAllIpLog(stepInstanceId, executeCount);

        // 删除日志文件
        StepInstanceBaseDTO stepInstance = stepInstanceDao.getStepInstanceBase(stepInstanceId);
        if (null == stepInstance) {
            return;
        }

        TaskInstanceDTO taskInstance = taskInstanceDao.getTaskInstance(stepInstance.getTaskInstanceId());
        if (null == taskInstance) {
            return;
        }
        logService.deleteStepLog(DateUtils.formatUnixTimestamp(taskInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC")), stepInstanceId, executeCount);
    }

    @Override
    public void batchSaveIpLog(List<GseTaskIpLogDTO> ipLogList) {
        if (ipLogList.size() == 0) {
            return;
        }
        if (ipLogList.size() <= 1000) {
            gseTaskIpLogDao.batchSaveIpLog(ipLogList);
        } else {
            List<List<GseTaskIpLogDTO>> batches = BatchUtil.buildBatchList(ipLogList, 1000);
            batches.parallelStream().forEach(gseTaskIpLogDao::batchSaveIpLog);
        }
    }

    @Override
    public void batchUpdateIpLog(long stepInstanceId, int executeCount, Collection<String> cloudAreaIdAndIps,
                                 Long startTime, Long endTime, IpStatus status) {
        if (cloudAreaIdAndIps == null || cloudAreaIdAndIps.size() == 0) {
            return;
        }
        gseTaskIpLogDao.batchUpdateIpLog(stepInstanceId, executeCount, cloudAreaIdAndIps, startTime, endTime, status);
    }

    @Override
    public int getSuccessIpCount(long stepInstanceId, int executeCount) {
        return gseTaskIpLogDao.getSuccessIpCount(stepInstanceId, executeCount);
    }

    @Override
    public List<GseTaskIpLogDTO> getSuccessGseTaskIp(long stepInstanceId, int executeCount) {
        return gseTaskIpLogDao.getSuccessGseTaskIp(stepInstanceId, executeCount);
    }

    @Override
    public List<AgentTaskResultGroupDTO> getIpLogStatInfo(long stepInstanceId, int executeCount) {
        List<AgentTaskResultGroupDTO> resultGroups = new ArrayList<>();

        List<ResultGroupBaseDTO> baseResultGroups = gseTaskIpLogDao.getResultGroups(stepInstanceId, executeCount);
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

        List<ResultGroupBaseDTO> baseResultGroups = gseTaskIpLogDao.getResultGroups(stepInstanceId, executeCount);
        for (ResultGroupBaseDTO baseResultGroup : baseResultGroups) {
            AgentTaskResultGroupDTO resultGroup = new AgentTaskResultGroupDTO();
            resultGroup.setResultType(IpStatus.valueOf(baseResultGroup.getResultType()));
            resultGroup.setTag(baseResultGroup.getTag());
            resultGroup.setCount(baseResultGroup.getAgentTaskCount());
            List<GseTaskIpLogDTO> gseTaskIpLogList = getIpLogByResultType(stepInstanceId, executeCount,
                baseResultGroup.getResultType(), baseResultGroup.getTag());
            List<IpDTO> ipList = new ArrayList<>();
            for (GseTaskIpLogDTO gseTaskIpLog : gseTaskIpLogList) {
                ipList.add(IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()));
            }
            resultGroup.setIpList(ipList);
            resultGroups.add(resultGroup);
        }
        return resultGroups;
    }


    @Override
    public List<GseTaskIpLogDTO> getIpLogByResultType(Long stepInstanceId, Integer executeCount, Integer resultType,
                                                      String tag) {
        return gseTaskIpLogDao.getIpLogByResultType(stepInstanceId, executeCount, resultType, tag);
    }

    @Override
    public List<GseTaskIpLogDTO> getIpLogContentByResultType(Long stepInstanceId, Integer executeCount,
                                                             Integer resultType, String tag) {
        List<GseTaskIpLogDTO> ipLogByResultType = getIpLogByResultType(stepInstanceId, executeCount, resultType, tag);
        StepInstanceBaseDTO stepInstance = stepInstanceDao.getStepInstanceBase(stepInstanceId);
        if (stepInstance == null) {
            return Collections.emptyList();
        }
        for (GseTaskIpLogDTO gseTaskIpLog : ipLogByResultType) {
            long startTime = System.currentTimeMillis();
            if (stepInstance.isScriptStep()) {
                ScriptIpLogContent scriptIpLogContent = logService.getScriptIpLogContent(stepInstanceId, executeCount
                    , IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()));
                gseTaskIpLog.setLogContent(scriptIpLogContent == null ? "" : scriptIpLogContent.getContent());
            } else if (stepInstance.isFileStep()) {
                FileIpLogContent fileIpLogContent = logService.getFileIpLogContent(stepInstanceId, executeCount,
                    IpDTO.fromCloudAreaIdAndIpStr(gseTaskIpLog.getCloudAreaAndIp()),
                    FileDistModeEnum.DOWNLOAD.getValue());
                gseTaskIpLog.setLogContent(fileIpLogContent == null ? "" : fileIpLogContent.getContent());
            }
            log.debug("stepInstanceId={}|ip={}|time={} ms", stepInstanceId, gseTaskIpLog.getCloudAreaAndIp(),
                (System.currentTimeMillis() - startTime));

        }

        return ipLogByResultType;
    }

    @Override
    public List<GseTaskIpLogDTO> getIpLog(Long stepInstanceId, Integer executeCount, boolean onlyTargetIp) {
        return gseTaskIpLogDao.getIpLog(stepInstanceId, executeCount, onlyTargetIp);
    }

    @Override
    public GseTaskIpLogDTO getIpLog(Long stepInstanceId, Integer executeCount, String cloudAreaIdAndIp) {
        return gseTaskIpLogDao.getIpLogByIp(stepInstanceId, executeCount, cloudAreaIdAndIp);
    }

    @Override
    public List<IpDTO> getTaskFileSourceIps(Long stepInstanceId, Integer executeCount) {
        return gseTaskIpLogDao.getTaskFileSourceIps(stepInstanceId, executeCount)
            .stream().map(IpDTO::fromCloudAreaIdAndIpStr).collect(Collectors.toList());
    }

    @Override
    public int getSuccessExecuteCount(long stepInstanceId, String cloudAreaAndIp) {
        return gseTaskIpLogDao.getSuccessRetryCount(stepInstanceId, cloudAreaAndIp);
    }
}
