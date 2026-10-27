/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.manage.api.common;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.check.ParamCheckUtil;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行账号全局变量保存校验器。
 */
@Slf4j
@Service
public class ExecuteAccountVariableValidator {

    private final AccountService accountService;

    public ExecuteAccountVariableValidator(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 校验步骤与待保存的执行账号变量，并规范化变量默认值。
     *
     * @param appId     业务 ID
     * @param steps     作业步骤列表
     * @param variables 待保存的变量列表
     */
    public void validate(Long appId, List<TaskStepDTO> steps, List<TaskVariableDTO> variables) {
        if (Boolean.TRUE.equals(JobContextUtil.isAllowMigration()) || CollectionUtils.isEmpty(steps)) {
            return;
        }
        Map<String, AccountDTO> variableAccountMap = buildAccountMap(appId, variables);
        for (TaskStepDTO step : steps) {
            if (step.isDeleted()) {
                continue;
            }
            validateStep(step, variableAccountMap);
        }
    }

    /**
     * 校验执行账号变量默认值并构建账号映射。
     */
    private Map<String, AccountDTO> buildAccountMap(Long appId, List<TaskVariableDTO> variables) {
        Map<String, AccountDTO> variableAccountMap = new HashMap<>();
        for (TaskVariableDTO variable : CollectionUtils.emptyIfNull(variables)) {
            if (Boolean.TRUE.equals(variable.getDelete())) {
                continue;
            }
            if (variable.getType() != TaskVariableTypeEnum.EXECUTE_ACCOUNT) {
                continue;
            }
            String accountValue = StringUtils.trim(variable.getDefaultValue());
            variable.setDefaultValue(accountValue);
            if (StringUtils.isBlank(accountValue)) {
                continue;
            }
            Long accountId = ParamCheckUtil.parseExecuteAccountId(accountValue, variable.getName());
            variableAccountMap.put(variable.getName(), accountService.getAccount(appId, accountId));
        }
        return variableAccountMap;
    }

    /**
     * 校验步骤引用的执行账号变量。
     */
    private void validateStep(TaskStepDTO step, Map<String, AccountDTO> variableAccountMap) {
        if (step.getType() == TaskStepTypeEnum.SCRIPT) {
            TaskScriptStepDTO scriptStep = step.getScriptStepInfo();
            AccountCategoryEnum expectedCategory = scriptStep.getLanguage() == ScriptTypeEnum.SQL
                ? AccountCategoryEnum.DB : AccountCategoryEnum.SYSTEM;
            validateAccountVariable(step, scriptStep.getAccountVar(), expectedCategory, variableAccountMap);
        } else if (step.getType() == TaskStepTypeEnum.FILE) {
            TaskFileStepDTO fileStep = step.getFileStepInfo();
            validateAccountVariable(step, fileStep.getExecuteAccountVar(), AccountCategoryEnum.SYSTEM,
                variableAccountMap);
            fileStep.getOriginFileList().stream()
                .filter(file -> file.getFileType() == TaskFileTypeEnum.SERVER)
                .map(TaskFileInfoDTO::getHostAccountVar)
                .forEach(accountVar -> validateAccountVariable(
                    step, accountVar, AccountCategoryEnum.SYSTEM, variableAccountMap));
        }
    }

    /**
     * 校验执行账号变量及其账号类型。
     */
    private void validateAccountVariable(TaskStepDTO step,
                                         String accountVar,
                                         AccountCategoryEnum expectedCategory,
                                         Map<String, AccountDTO> variableAccountMap) {
        if (StringUtils.isBlank(accountVar)) {
            return;
        }
        AccountDTO account = variableAccountMap.get(accountVar);
        if (account != null && account.getCategory() != expectedCategory) {
            log.warn("Execute account variable category mismatch, stepId={}, stepName={}, variable={}, accountId={}, "
                + "expected={}, actual={}", step.getId(), step.getName(), accountVar, account.getId(),
                expectedCategory, account.getCategory());
            throw invalidVariable(String.format(
                "step[%s] uses variable[%s] requiring a %s account", step.getName(), accountVar,
                expectedCategory.getName()));
        }
    }

    /**
     * 构造执行账号变量参数异常。
     */
    private InvalidParamException invalidVariable(String reason) {
        return new InvalidParamException(
            ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
            new Object[]{"variables", reason}
        );
    }
}
