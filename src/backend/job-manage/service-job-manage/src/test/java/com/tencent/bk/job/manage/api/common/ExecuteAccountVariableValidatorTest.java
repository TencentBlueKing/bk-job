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
import com.tencent.bk.job.common.exception.NotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExecuteAccountVariableValidatorTest {

    private static final Long APP_ID = 1L;
    private static final Long ACCOUNT_ID = 100L;

    private AccountService accountService;
    private ExecuteAccountVariableValidator validator;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        validator = new ExecuteAccountVariableValidator(accountService);
    }

    @Test
    void sqlStep_requiresDbAccount() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.SYSTEM));

        assertThatThrownBy(() -> validator.validate(APP_ID,
            Collections.singletonList(scriptStep("SQL step", ScriptTypeEnum.SQL)),
            Collections.singletonList(accountVariable())))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> assertThat(e.getErrorParams()).containsExactly(
                "variables", "step[SQL step] uses variable[account] requiring a db account"));
    }

    @Test
    void sqlStep_acceptsDbAccount() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.DB));

        assertThatCode(() -> validator.validate(APP_ID,
            Collections.singletonList(scriptStep("SQL step", ScriptTypeEnum.SQL)),
            Collections.singletonList(accountVariable())))
            .doesNotThrowAnyException();
    }

    @Test
    void serverFile_requiresSystemAccount() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.DB));

        assertThatThrownBy(() -> validator.validate(APP_ID,
            Collections.singletonList(serverFileStep()), Collections.singletonList(accountVariable())))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> assertThat(e.getErrorParams()).containsExactly(
                "variables", "step[File step] uses variable[account] requiring a system account"));
    }

    @Test
    void fileTarget_requiresSystemAccount() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.DB));

        TaskStepDTO step = fileStep(TaskFileTypeEnum.LOCAL);
        step.getFileStepInfo().setExecuteAccountVar("account");

        assertThatThrownBy(() -> validator.validate(APP_ID, Collections.singletonList(step),
            Collections.singletonList(accountVariable())))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> assertThat(e.getErrorParams()).containsExactly(
                "variables", "step[File step] uses variable[account] requiring a system account"));
    }

    @Test
    void localFile_ignoresSourceAccount() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.DB));

        assertThatCode(() -> validator.validate(APP_ID,
            Collections.singletonList(fileStep(TaskFileTypeEnum.LOCAL)),
            Collections.singletonList(accountVariable())))
            .doesNotThrowAnyException();
    }

    @Test
    void accountNotExist_throws() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID))
            .thenThrow(new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST));

        assertThatThrownBy(() -> validator.validate(APP_ID,
            Collections.singletonList(scriptStep("Shell step", ScriptTypeEnum.SHELL)),
            Collections.singletonList(accountVariable())))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_EXIST));
    }

    @Test
    void accountValue_isTrimmed() {
        TaskVariableDTO variable = accountVariable();
        variable.setDefaultValue("  " + ACCOUNT_ID + "  ");
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.SYSTEM));

        validator.validate(APP_ID, Collections.singletonList(scriptStep("Shell step", ScriptTypeEnum.SHELL)),
            Collections.singletonList(variable));

        assertThat(variable.getDefaultValue()).isEqualTo(String.valueOf(ACCOUNT_ID));
    }

    @Test
    void invalidAccountId_throws() {
        TaskVariableDTO variable = accountVariable();
        variable.setDefaultValue("invalid");

        assertThatThrownBy(() -> validator.validate(APP_ID,
            Collections.singletonList(scriptStep("Shell step", ScriptTypeEnum.SHELL)),
            Collections.singletonList(variable)))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> assertThat(e.getErrorParams()).containsExactly(
                "variables", "value of variable[account] must be a valid account ID"));
    }

    @Test
    void deletedStep_isIgnored() {
        when(accountService.getAccount(APP_ID, ACCOUNT_ID)).thenReturn(account(AccountCategoryEnum.SYSTEM));
        TaskStepDTO step = scriptStep("SQL step", ScriptTypeEnum.SQL);
        step.setDelete(1);

        assertThatCode(() -> validator.validate(APP_ID, Collections.singletonList(step),
            Collections.singletonList(accountVariable())))
            .doesNotThrowAnyException();
    }

    private TaskStepDTO scriptStep(String name, ScriptTypeEnum language) {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setLanguage(language);
        scriptStep.setAccountVar("account");
        TaskStepDTO step = new TaskStepDTO();
        step.setName(name);
        step.setType(TaskStepTypeEnum.SCRIPT);
        step.setScriptStepInfo(scriptStep);
        return step;
    }

    private TaskStepDTO serverFileStep() {
        return fileStep(TaskFileTypeEnum.SERVER);
    }

    private TaskStepDTO fileStep(TaskFileTypeEnum fileType) {
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
        fileInfo.setFileType(fileType);
        fileInfo.setHostAccountVar("account");
        TaskFileStepDTO fileStep = new TaskFileStepDTO();
        fileStep.setOriginFileList(Collections.singletonList(fileInfo));
        TaskStepDTO step = new TaskStepDTO();
        step.setName("File step");
        step.setType(TaskStepTypeEnum.FILE);
        step.setFileStepInfo(fileStep);
        return step;
    }

    private TaskVariableDTO accountVariable() {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setName("account");
        variable.setType(TaskVariableTypeEnum.EXECUTE_ACCOUNT);
        variable.setDefaultValue(String.valueOf(ACCOUNT_ID));
        return variable;
    }

    private AccountDTO account(AccountCategoryEnum category) {
        AccountDTO account = new AccountDTO();
        account.setId(ACCOUNT_ID);
        account.setAppId(APP_ID);
        account.setCategory(category);
        return account;
    }
}
