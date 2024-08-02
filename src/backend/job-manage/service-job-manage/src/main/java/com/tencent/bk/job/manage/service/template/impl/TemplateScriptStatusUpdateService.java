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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.util.TemplateScriptStatusFlagsUtil;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.dao.template.TaskTemplateScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.TemplateStepScriptStatusInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TemplateScriptStatusUpdateService {
    private final TaskTemplateDAO taskTemplateDAO;
    private final TaskTemplateScriptStepDAO taskTemplateScriptStepDAO;

    private final ScriptDAO scriptDAO;

    @Autowired
    public TemplateScriptStatusUpdateService(TaskTemplateDAO taskTemplateDAO,
                                             TaskTemplateScriptStepDAO taskTemplateScriptStepDAO,
                                             ScriptDAO scriptDAO) {
        this.taskTemplateDAO = taskTemplateDAO;
        this.taskTemplateScriptStepDAO = taskTemplateScriptStepDAO;
        this.scriptDAO = scriptDAO;
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void refreshTemplateScriptStatusByTemplate(long templateId) {
        log.info("Refresh template script status flags by templateId : {}", templateId);

        StopWatch watch = new StopWatch("refreshTemplateScriptStatusByTemplate");
        try {
            watch.start("getTemplateStepScriptStatusInfos");
            List<TemplateStepScriptStatusInfo> steps = getTemplateStepScriptStatusInfos(templateId);
            if (log.isDebugEnabled()) {
                log.debug("Affected steps : {}", steps);
            }
            watch.stop();
            updateScriptStatusFlags(watch, steps);
            log.info("Refresh template script status flags by templateId success");
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000L) {
                log.info("RefreshTemplateScriptStatusByTemplate is slow, cost: {}", watch.prettyPrint());
            }
        }

    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void refreshTemplateScriptStatusByScript(String scriptId, long scriptVersionId) {
        log.info("Refresh template script status flags by script, scriptId: {}, scriptVersionId: {}",
            scriptId, scriptVersionId);

        StopWatch watch = new StopWatch("refreshTemplateScriptStatusByScript");
        try {
            watch.start("getTemplateStepScriptStatusInfos");
            List<TemplateStepScriptStatusInfo> steps = getTemplateStepScriptStatusInfos(scriptId, scriptVersionId);
            if (log.isDebugEnabled()) {
                log.debug("Affected steps : {}", steps);
            }
            watch.stop();
            updateScriptStatusFlags(watch, steps);
            log.info("Refresh template script status flags by script success");
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000L) {
                log.info("RefreshTemplateScriptStatusByScript is slow, cost: {}", watch.prettyPrint());
            }

        }
    }

    private void updateScriptStatusFlags(StopWatch watch, List<TemplateStepScriptStatusInfo> steps) {
        if (CollectionUtils.isEmpty(steps)) {
            log.info("None template and step script status need update!");
            return;
        }
        // 查询脚本状态实时数据并重置
        watch.start("resetStepsScriptStatusFlags");
        resetStepsScriptStatusFlags(steps);
        watch.stop();

        // 更新步骤脚本状态
        watch.start("updateStepsScriptStatusFlags");
        updateStepsScriptStatusFlags(steps);
        watch.stop();

        watch.start("updateTemplateScriptStatusFlags");
        // 更新模版整体脚本状态
        updateTemplateScriptStatusFlags(steps);
        watch.stop();
    }

    private List<TemplateStepScriptStatusInfo> getTemplateStepScriptStatusInfos(String scriptId,
                                                                                long scriptVersionId) {
        return taskTemplateScriptStepDAO.listAllRelatedTemplateStepsScriptStatusInfo(scriptId, scriptVersionId);
    }

    private List<TemplateStepScriptStatusInfo> getTemplateStepScriptStatusInfos(long templateId) {
        return taskTemplateScriptStepDAO.listStepsScriptStatusInfoByTemplateId(templateId);
    }

    /*
     * 更新步骤上的脚本状态flags
     */
    private void updateStepsScriptStatusFlags(List<TemplateStepScriptStatusInfo> templateStepScriptStatusInfos) {
        // 先按照状态组织数据，方便后续进行批量更新
        Map<Integer, List<TemplateStepScriptStatusInfo>> scriptStatusFlagsGroups =
            templateStepScriptStatusInfos.stream()
                .collect(Collectors.groupingBy(TemplateStepScriptStatusInfo::getScriptStatusFlags));
        scriptStatusFlagsGroups.forEach((scriptStatusFlags, steps) -> {
            List<Long> stepIds = steps.stream()
                .map(TemplateStepScriptStatusInfo::getStepId).distinct().collect(Collectors.toList());
            log.info("Update template steps script status flags, stepIds: {}, scriptStatusFlags: {}",
                stepIds, scriptStatusFlags);
            taskTemplateScriptStepDAO.batchUpdateScriptStatusFlags(stepIds, scriptStatusFlags);
        });
    }

    /*
     * 更新模版整体脚本状态flags
     */
    private void updateTemplateScriptStatusFlags(List<TemplateStepScriptStatusInfo> templateStepScriptStatusInfos) {
        Map<Long, List<TemplateStepScriptStatusInfo>> templateSteps =
            templateStepScriptStatusInfos.stream()
                .collect(Collectors.groupingBy(TemplateStepScriptStatusInfo::getTemplateId));
        Map<Long, Integer> templateScriptStatusFlagsMap = new HashMap<>();
        templateSteps.forEach((templateId, steps) ->
            templateScriptStatusFlagsMap.put(templateId, computeTemplateScriptStatusFlags(steps)));

        log.info("Update template script status flags, templateScriptStatusFlagsMap: {}",
            templateScriptStatusFlagsMap);

        // 先按照状态组织数据，方便后续进行批量更新
        Map<Integer, List<Long>> scriptStatusFlagsGroups =
            templateScriptStatusFlagsMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                    Map.Entry::getValue,
                    Collectors.mapping(Map.Entry::getKey, Collectors.toList()))
                );
        scriptStatusFlagsGroups.forEach((scriptStatusFlags, templateIds) ->
            taskTemplateDAO.batchUpdateTemplateScriptStatus(templateIds, scriptStatusFlags));
    }

    private void resetStepsScriptStatusFlags(List<TemplateStepScriptStatusInfo> templateStepScriptStatusInfos) {
        Map<Long, JobResourceStatusEnum> scriptVersionStatusMap =
            batchGetScriptVersionStatus(templateStepScriptStatusInfos);
        templateStepScriptStatusInfos.forEach(
            templateStepScriptStatusInfo ->
                templateStepScriptStatusInfo.setScriptStatusFlags(
                    toScriptStatusBinaryFlags(
                        scriptVersionStatusMap.get(templateStepScriptStatusInfo.getScriptVersionId()))
                )
        );
    }

    private int toScriptStatusBinaryFlags(JobResourceStatusEnum scriptStatus) {
        switch (scriptStatus) {
            case ONLINE:
                return TemplateScriptStatusFlagsUtil.computeScriptStatusFlags(false, false);
            case OFFLINE:
                return TemplateScriptStatusFlagsUtil.computeScriptStatusFlags(true, false);
            case DISABLED:
                return TemplateScriptStatusFlagsUtil.computeScriptStatusFlags(false, true);
            default:
                log.error("Unexpected script status");
                throw new IllegalStateException("Unexpected script status [" + scriptStatus + "]");
        }
    }

    private Map<Long, JobResourceStatusEnum> batchGetScriptVersionStatus(
        List<TemplateStepScriptStatusInfo> templateStepScriptStatusInfos) {
        List<Long> scriptVersionIds = templateStepScriptStatusInfos.stream()
            .map(TemplateStepScriptStatusInfo::getScriptVersionId)
            .distinct()
            .collect(Collectors.toList());
        return scriptDAO.batchGetScriptVersionStatus(scriptVersionIds);
    }

    private int computeTemplateScriptStatusFlags(
        List<TemplateStepScriptStatusInfo> templateStepScriptStatusInfos) {
        boolean existOfflineScript = templateStepScriptStatusInfos.stream()
            .anyMatch(steps -> TemplateScriptStatusFlagsUtil.readOfflineFlag(steps.getScriptStatusFlags()));
        boolean existDisabledScript = templateStepScriptStatusInfos.stream()
            .anyMatch(steps -> TemplateScriptStatusFlagsUtil.readDisableFlag(steps.getScriptStatusFlags()));
        return TemplateScriptStatusFlagsUtil.computeScriptStatusFlags(existOfflineScript, existDisabledScript);
    }
}
