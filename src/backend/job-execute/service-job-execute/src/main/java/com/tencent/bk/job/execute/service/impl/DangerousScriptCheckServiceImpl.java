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

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.client.ScriptCheckResourceClient;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.model.DangerousRecordDTO;
import com.tencent.bk.job.execute.model.ScriptCheckItemDTO;
import com.tencent.bk.job.execute.model.ScriptCheckResultDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.DangerousRecordService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.manage.common.consts.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceCheckScriptRequest;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DangerousScriptCheckServiceImpl implements DangerousScriptCheckService {
    private final ScriptCheckResourceClient scriptCheckResourceClient;
    private final MessageI18nService messageI18nService;
    private final DangerousRecordService dangerousRecordService;
    private final ApplicationService applicationService;

    @Autowired
    public DangerousScriptCheckServiceImpl(ScriptCheckResourceClient scriptCheckResourceClient,
                                           MessageI18nService messageI18nService,
                                           DangerousRecordService dangerousRecordService,
                                           ApplicationService applicationService) {
        this.scriptCheckResourceClient = scriptCheckResourceClient;
        this.messageI18nService = messageI18nService;
        this.dangerousRecordService = dangerousRecordService;
        this.applicationService = applicationService;
    }

    @Override
    public List<ServiceScriptCheckResultItemDTO> check(ScriptTypeEnum scriptType, String content) {
        InternalResponse<List<ServiceScriptCheckResultItemDTO>> response =
            scriptCheckResourceClient.check(new ServiceCheckScriptRequest(content, scriptType.getValue()));
        return response.isSuccess() ? response.getData() : Collections.emptyList();
    }

    @Override
    public boolean shouldIntercept(List<ServiceScriptCheckResultItemDTO> checkResultItems) {
        return checkResultItems.stream().anyMatch(checkResultItem -> checkResultItem.getAction() != null
            && RuleMatchHandleActionEnum.INTERCEPT.getValue() == checkResultItem.getAction());
    }

    @Override
    public String summaryDangerousScriptCheckResult(String stepName,
                                                    List<ServiceScriptCheckResultItemDTO> checkResults) {
        if (CollectionUtils.isEmpty(checkResults)) {
            return "";
        }
        List<String> checkResultDescList =
            checkResults.stream().filter(checkResult
                -> ScriptCheckErrorLevelEnum.FATAL.getValue() == checkResult.getLevel())
            .map(checkResult -> buildScriptCheckResultDetail(stepName, checkResult))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(checkResultDescList)) {
            return "";
        }
        String errorTitle = messageI18nService.getI18n("script.check.result.detect.dangerous.script");
        StringJoiner stringJoiner = new StringJoiner("||");
        checkResultDescList.forEach(stringJoiner::add);
        return errorTitle + "!" + stringJoiner.toString();
    }

    private String buildScriptCheckResultDetail(String stepName, ServiceScriptCheckResultItemDTO checkResult) {
        StringBuilder builder = new StringBuilder("[step:").append(stepName).append("]");
        builder.append("[line:").append(checkResult.getLine()).append("]");
        builder.append("[content:").append(checkResult.getLineContent()).append("]");
        builder.append(" - ");
        builder.append(checkResult.getDescription());
        return builder.toString();
    }

    @Override
    public void saveDangerousRecord(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                                    List<ServiceScriptCheckResultItemDTO> checkResultItems) {
        DangerousRecordDTO record = buildDangerousRecord(taskInstance, stepInstance, checkResultItems);
        dangerousRecordService.saveDangerousRecord(record);
    }

    private DangerousRecordDTO buildDangerousRecord(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                                                    List<ServiceScriptCheckResultItemDTO> checkResultItems) {
        ServiceScriptCheckResultItemDTO checkResultItem = getPrimaryOne(checkResultItems);
        DangerousRecordDTO record = new DangerousRecordDTO();
        record.setRuleId(checkResultItem.getRuleId());
        record.setRuleExpression(checkResultItem.getRuleExpression());
        record.setAction(checkResultItem.getAction());
        record.setAppId(taskInstance.getAppId());
        ServiceApplicationDTO app = applicationService.getAppById(taskInstance.getAppId());
        if (app != null) {
            record.setAppName(app.getName());
        }
        record.setCreateTime(System.currentTimeMillis());
        record.setStartupMode(taskInstance.getStartupMode());
        if (TaskStartupModeEnum.getStartupMode(taskInstance.getStartupMode()) == TaskStartupModeEnum.API) {
            record.setClient(taskInstance.getAppCode());
        } else {
            record.setClient("bk_job");
        }
        record.setOperator(taskInstance.getOperator());
        record.setScriptLanguage(stepInstance.getScriptType());
        record.setScriptContent(stepInstance.getScriptContent());

        List<ScriptCheckItemDTO> checkItems = new ArrayList<>(1);
        ScriptCheckItemDTO checkItem = new ScriptCheckItemDTO(checkResultItem.getLine(),
            checkResultItem.getLineContent(),
            checkResultItem.getMatchContent(), checkResultItem.getLevel(), checkResultItem.getDescription());
        checkItems.add(checkItem);
        ScriptCheckResultDTO checkResult = new ScriptCheckResultDTO(checkItems);
        record.setCheckResult(checkResult);

        return record;
    }

    private ServiceScriptCheckResultItemDTO getPrimaryOne(List<ServiceScriptCheckResultItemDTO> checkResultItems) {
        if (checkResultItems.size() == 1) {
            return checkResultItems.get(0);
        }
        return checkResultItems.stream().filter(checkResultItem -> checkResultItem.getAction() != null
            && RuleMatchHandleActionEnum.INTERCEPT.getValue() == checkResultItem.getAction())
            .findFirst().orElseGet(() -> checkResultItems.get(0));

    }
}
