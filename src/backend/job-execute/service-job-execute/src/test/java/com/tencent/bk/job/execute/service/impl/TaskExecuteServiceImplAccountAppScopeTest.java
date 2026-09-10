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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.FileSourceReferenceService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖 TaskExecuteServiceImpl 中账号相关的两处逻辑：
 * <ul>
 *   <li>{@code checkAccountAppScope}：步骤引用的账号必须归属当前业务。按别名解析账号一直带 appId 过滤、
 *   按 ID 解析不带，而账号 ID 是 API 调用方可任意指定的，这里是「跨业务引用在执行前被拒」的回归网</li>
 *   <li>{@code checkAndSetOsAccountInfo}：解析文件源账号时判断用的是源账号 ID、查的却是步骤账号 ID
 *   （存量 bug），后两个用例是该修复的回归网</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskExecuteServiceImpl: 账号归属业务校验与文件源账号解析")
class TaskExecuteServiceImplAccountAppScopeTest {

    private static final long APP_ID = 2L;
    private static final long OTHER_APP_ID = 3L;

    @Mock private AccountService accountService;
    @Mock private TaskInstanceService taskInstanceService;
    @Mock private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    @Mock private TaskPlanService taskPlanService;
    @Mock private TaskInstanceVariableService taskInstanceVariableService;
    @Mock private TaskOperationLogService taskOperationLogService;
    @Mock private ScriptService scriptService;
    @Mock private StepInstanceService stepInstanceService;
    @Mock private ServiceUserResource userResource;
    @Mock private ExecuteAuthService executeAuthService;
    @Mock private DangerousScriptCheckService dangerousScriptCheckService;
    @Mock private JobExecuteConfig jobExecuteConfig;
    @Mock private TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    @Mock private RollingConfigService rollingConfigService;
    @Mock private ServiceTaskTemplateResource taskTemplateResource;
    @Mock private TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor;
    @Mock private RunningJobResourceQuotaManager runningJobResourceQuotaManager;
    @Mock private HostService hostService;
    @Mock private CustomPasswordCache customPasswordCache;
    @Mock private TenantService tenantService;
    @Mock private FileSourceReferenceService fileSourceReferenceService;

    private TaskExecuteServiceImpl service;
    private Method checkMethod;
    private Method osAccountMethod;

    @BeforeEach
    void setUp() throws Exception {
        service = new TaskExecuteServiceImpl(
            accountService,
            taskInstanceService,
            taskExecuteMQEventDispatcher,
            taskPlanService,
            taskInstanceVariableService,
            taskOperationLogService,
            scriptService,
            stepInstanceService,
            userResource,
            executeAuthService,
            dangerousScriptCheckService,
            jobExecuteConfig,
            taskEvictPolicyExecutor,
            rollingConfigService,
            taskTemplateResource,
            taskInstanceExecuteObjectProcessor,
            runningJobResourceQuotaManager,
            hostService,
            customPasswordCache,
            tenantService,
            fileSourceReferenceService
        );

        checkMethod = TaskExecuteServiceImpl.class
            .getDeclaredMethod("checkAccountAppScope", TaskInstanceDTO.class, List.class);
        checkMethod.setAccessible(true);

        osAccountMethod = TaskExecuteServiceImpl.class
            .getDeclaredMethod("checkAndSetOsAccountInfo", StepInstanceDTO.class, Long.class);
        osAccountMethod.setAccessible(true);
    }

    @Test
    @DisplayName("账号属于本业务：放行")
    void accountInCurrentAppPasses() {
        mockAccount(100L, APP_ID, null);

        StepInstanceDTO step = new StepInstanceDTO();
        step.setAccountId(100L);

        assertThatCode(() -> invoke(step)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("步骤账号属于其他业务：拒绝并报账号不存在")
    void stepAccountOfOtherAppRejected() {
        mockAccount(100L, OTHER_APP_ID, null);

        StepInstanceDTO step = new StepInstanceDTO();
        step.setAccountId(100L);

        assertThatThrownBy(() -> invoke(step))
            .isInstanceOf(NotFoundException.class)
            .satisfies(e -> assertThat(((NotFoundException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_EXIST));
    }

    @Test
    @DisplayName("文件源的源账号属于其他业务：拒绝")
    void fileSourceAccountOfOtherAppRejected() {
        mockAccount(100L, APP_ID, null);
        mockAccount(200L, OTHER_APP_ID, null);

        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountId(200L);
        StepInstanceDTO step = new StepInstanceDTO();
        step.setAccountId(100L);
        step.setFileSourceList(Collections.singletonList(fileSource));

        assertThatThrownBy(() -> invoke(step)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("DB 账号本身在本业务、但其依赖的系统账号跨业务：同样拒绝")
    void dbAccountDependentSystemAccountOfOtherAppRejected() {
        mockAccount(300L, APP_ID, 301L);
        mockAccount(301L, OTHER_APP_ID, null);

        StepInstanceDTO step = new StepInstanceDTO();
        step.setDbAccountId(300L);

        assertThatThrownBy(() -> invoke(step)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("账号不存在与账号跨业务返回同一错误码，不可据此探测其他业务的账号")
    void notExistAndCrossAppAreIndistinguishable() {
        mockAccount(100L, OTHER_APP_ID, null);
        when(accountService.getAccountPreferCache(eq(999L), isNull(), isNull(), isNull())).thenReturn(null);

        StepInstanceDTO crossAppStep = new StepInstanceDTO();
        crossAppStep.setAccountId(100L);
        StepInstanceDTO notExistStep = new StepInstanceDTO();
        notExistStep.setAccountId(999L);

        NotFoundException crossApp = catchNotFound(crossAppStep);
        NotFoundException notExist = catchNotFound(notExistStep);

        assertThat(crossApp.getErrorCode()).isEqualTo(notExist.getErrorCode());
        assertThat(crossApp.getErrorParams()).containsExactly("ID=100");
        assertThat(notExist.getErrorParams()).containsExactly("ID=999");
    }

    @Test
    @DisplayName("多个步骤引用同一账号时只查一次")
    void duplicatedAccountIsQueriedOnce() {
        mockAccount(100L, APP_ID, null);

        StepInstanceDTO first = new StepInstanceDTO();
        first.setAccountId(100L);
        StepInstanceDTO second = new StepInstanceDTO();
        second.setAccountId(100L);

        assertThatCode(() -> invokeAll(first, second)).doesNotThrowAnyException();
        verify(accountService, times(1)).getAccountPreferCache(eq(100L), isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("步骤以别名指定账号时，经解析回填 ID 后仍进入归属校验（锁住两者的先后顺序）")
    void aliasSpecifiedAccountIsCheckedAfterResolved() throws Exception {
        AccountDTO stepAccount = new AccountDTO();
        stepAccount.setId(100L);
        stepAccount.setAppId(APP_ID);
        stepAccount.setAccount("root");
        stepAccount.setAlias("root-alias");
        when(accountService.getSystemAccountByAlias("root-alias", APP_ID)).thenReturn(stepAccount);
        mockAccount(100L, APP_ID, null);

        StepInstanceDTO step = new StepInstanceDTO();
        step.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        step.setAccountAlias("root-alias");

        invokeCheckAndSetOsAccountInfo(step);
        assertThat(step.getAccountId()).isEqualTo(100L);

        invoke(step);
        verify(accountService).getAccountPreferCache(eq(100L), isNull(), isNull(), isNull());
    }

    // ------------------------------------------------ 文件源账号解析（回归：判断用源账号 ID、查的却是步骤账号 ID）

    @Test
    @DisplayName("源账号与目标账号不同时，源文件回填的是源账号而非步骤的目标账号")
    void fileSourceKeepsItsOwnAccount() throws Exception {
        mockNamedAccount(100L, APP_ID, "root");
        mockNamedAccount(200L, APP_ID, "user00");

        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountId(200L);
        StepInstanceDTO step = fileStep();
        step.setAccountId(100L);
        step.setFileSourceList(Collections.singletonList(fileSource));

        invokeCheckAndSetOsAccountInfo(step);

        assertThat(step.getAccount()).isEqualTo("root");
        assertThat(fileSource.getAccount()).isEqualTo("user00");
        assertThat(fileSource.getAccountAlias()).isEqualTo("user00-alias");
    }

    @Test
    @DisplayName("步骤用别名指定账号、源文件用 ID 指定时，不因步骤账号 ID 缺省而误报账号不存在")
    void fileSourceResolvedWhenStepAccountGivenByAlias() {
        AccountDTO stepAccount = new AccountDTO();
        stepAccount.setId(100L);
        stepAccount.setAppId(APP_ID);
        stepAccount.setAccount("root");
        stepAccount.setAlias("root-alias");
        when(accountService.getSystemAccountByAlias("root-alias", APP_ID)).thenReturn(stepAccount);
        mockNamedAccount(200L, APP_ID, "user00");

        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccountId(200L);
        StepInstanceDTO step = fileStep();
        step.setAccountAlias("root-alias");
        step.setFileSourceList(Collections.singletonList(fileSource));

        assertThatCode(() -> invokeCheckAndSetOsAccountInfo(step)).doesNotThrowAnyException();
        assertThat(fileSource.getAccount()).isEqualTo("user00");
    }

    private StepInstanceDTO fileStep() {
        StepInstanceDTO step = new StepInstanceDTO();
        step.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        return step;
    }

    private void invokeCheckAndSetOsAccountInfo(StepInstanceDTO step) throws Exception {
        try {
            osAccountMethod.invoke(service, step, APP_ID);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    private void mockNamedAccount(Long accountId, Long appId, String name) {
        AccountDTO account = new AccountDTO();
        account.setId(accountId);
        account.setAppId(appId);
        account.setAccount(name);
        account.setAlias(name + "-alias");
        when(accountService.getAccountPreferCache(eq(accountId), isNull(), isNull(), isNull())).thenReturn(account);
    }

    private void mockAccount(Long accountId, Long appId, Long dbSystemAccountId) {
        AccountDTO account = new AccountDTO();
        account.setId(accountId);
        account.setAppId(appId);
        account.setDbSystemAccountId(dbSystemAccountId);
        when(accountService.getAccountPreferCache(eq(accountId), isNull(), isNull(), isNull())).thenReturn(account);
    }

    private NotFoundException catchNotFound(StepInstanceDTO step) {
        try {
            invoke(step);
        } catch (NotFoundException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("expected NotFoundException but got " + e, e);
        }
        throw new AssertionError("expected NotFoundException");
    }

    private void invoke(StepInstanceDTO step) throws Exception {
        invokeAll(step);
    }

    private void invokeAll(StepInstanceDTO... steps) throws Exception {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(APP_ID);
        try {
            checkMethod.invoke(service, taskInstance, Arrays.asList(steps));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }
}
