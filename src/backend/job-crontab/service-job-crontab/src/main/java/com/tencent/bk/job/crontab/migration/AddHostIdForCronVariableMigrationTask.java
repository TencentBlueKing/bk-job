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

package com.tencent.bk.job.crontab.migration;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobWithVarsDTO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.crontab.service.HostService;
import com.tencent.bk.job.manage.model.migration.AddHostIdResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

/**
 * 对定时任务变量数据中不存在hostId的数据使用CMDB中的完整数据补全hostId
 */
@Slf4j
@Service
public class AddHostIdForCronVariableMigrationTask {

    private final CronJobDAO cronJobDAO;
    private final HostService hostService;

    @Autowired
    public AddHostIdForCronVariableMigrationTask(CronJobDAO cronJobDAO,
                                                 HostService hostService) {
        this.cronJobDAO = cronJobDAO;
        this.hostService = hostService;
    }

    private String getTaskName() {
        return "migrate_cron_job";
    }

    private AddHostIdResult getInitAddHostIdResult() {
        AddHostIdResult result = new AddHostIdResult(getTaskName());
        result.setTotalRecords(0);
        result.setSuccessRecords(0);
        result.setSuccess(true);
        return result;
    }

    public AddHostIdResult execute(boolean isDryRun) {
        StopWatch watch = new StopWatch(getTaskName());
        try {
            return updateHostIdForCronVariables(isDryRun, watch);
        } catch (Throwable t) {
            log.warn("Fail to updateHostIdForCronVariables", t);
            AddHostIdResult result = getInitAddHostIdResult();
            result.setSuccess(false);
            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("AddHostIdForCronVariableMigrationTask finished, time consuming:{}", watch.prettyPrint());
        }
    }

    /**
     * 判断定时任务的变量中是否存在hostId
     *
     * @param cronJobDTO 定时任务信息
     * @return 是否存在hostId
     */
    private boolean existsHostId(CronJobWithVarsDTO cronJobDTO) {
        if (cronJobDTO == null) {
            return false;
        }
        List<CronJobVariableDTO> variableValue = cronJobDTO.getVariableValue();
        if (CollectionUtils.isEmpty(variableValue)) {
            return false;
        }
        for (CronJobVariableDTO cronJobVariableDTO : variableValue) {
            if (TaskVariableTypeEnum.HOST_LIST != cronJobVariableDTO.getType()) {
                continue;
            }
            ServerDTO server = cronJobVariableDTO.getServer();
            if (server == null || CollectionUtils.isEmpty(server.getIps())) {
                continue;
            }
            List<HostDTO> hosts = server.getIps();
            for (HostDTO host : hosts) {
                if (host.getHostId() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private AddHostIdResult updateHostIdForCronVariables(boolean isDryRun, StopWatch watch) {
        AddHostIdResult result = getInitAddHostIdResult();
        int start = 0;
        int batchSize = 100;
        List<CronJobWithVarsDTO> cronJobList;
        int totalNullHostIdRecordsCount = 0;
        int totalUpdatedCount = 0;
        do {
            // 1.查询含有主机变量的定时任务
            watch.start("listBasicCronJobWithHostVars_" + start + "_" + (start + batchSize));
            cronJobList = cronJobDAO.listBasicCronJobWithHostVars(start, batchSize);
            watch.stop();
            if (CollectionUtils.isEmpty(cronJobList)) {
                continue;
            }
            totalNullHostIdRecordsCount += cronJobList.size();
            int count = 0;

            // 2.查出变量中主机的hostId并填充
            watch.start("addHostIdForCronVariables_" + start + "_" + (start + batchSize));
            int filledHostCount = tryToAddHostIdForCronVarHosts(cronJobList);
            watch.stop();

            // 3.更新已填充hostId的变量到DB
            watch.start("updateVariableById_" + start + "_" + (start + batchSize));
            log.debug("filled {} hosts with hostId for {} cronJobs", filledHostCount, cronJobList.size());
            for (CronJobWithVarsDTO cronJobWithVarsDTO : cronJobList) {
                if (!existsHostId(cronJobWithVarsDTO)) {
                    continue;
                }
                String variableValueStr = JsonUtils.toJson(cronJobWithVarsDTO.getVariableValue());
                if (isDryRun) {
                    log.info(
                        "[DryRun]set variable_value={} for cronJob(id={})",
                        variableValueStr,
                        cronJobWithVarsDTO.getId()
                    );
                    count += 1;
                } else {
                    count += cronJobDAO.updateVariableById(cronJobWithVarsDTO.getId(), variableValueStr);
                }
            }
            watch.stop();

            // 4.统计信息记录、打印
            start += batchSize;
            totalUpdatedCount += count;
            log.info((isDryRun ? "[DryRun]" : "") + "{}/{} cronJobs have been added hostId", count, cronJobList.size());
        } while (!CollectionUtils.isEmpty(cronJobList));
        log.info((isDryRun ? "[DryRun]" : "") + "{} cronJobs have been added hostId in total", totalUpdatedCount);
        result.setTotalRecords(totalNullHostIdRecordsCount);
        result.setSuccessRecords(totalUpdatedCount);
        result.setSuccess(true);
        return result;
    }

    /**
     * 尝试为定时任务变量中的主机填充hostId，忽略找不到hostId的主机（可能已在CMDB中被移除）
     *
     * @param cronJobList 定时任务列表
     * @return 实际填充了hostId的主机数量
     */
    private int tryToAddHostIdForCronVarHosts(List<CronJobWithVarsDTO> cronJobList) {
        List<HostDTO> hostList = new ArrayList<>();
        // 1.收集所有主机信息
        for (CronJobWithVarsDTO cronJobDTO : cronJobList) {
            List<CronJobVariableDTO> variableValue = cronJobDTO.getVariableValue();
            if (CollectionUtils.isEmpty(variableValue)) {
                continue;
            }
            for (CronJobVariableDTO cronJobVariableDTO : variableValue) {
                if (TaskVariableTypeEnum.HOST_LIST != cronJobVariableDTO.getType()) {
                    continue;
                }
                ServerDTO server = cronJobVariableDTO.getServer();
                if (server == null || CollectionUtils.isEmpty(server.getIps())) {
                    continue;
                }
                for (HostDTO host : server.getIps()) {
                    // 已存在hostId的主机不再填充
                    if (host.getHostId() == null) {
                        hostList.add(host);
                    }
                }
            }
        }
        // 2.批量查询hostId并填充
        if (CollectionUtils.isNotEmpty(hostList)) {
            return hostService.fillHosts(hostList);
        } else {
            return 0;
        }
    }
}
