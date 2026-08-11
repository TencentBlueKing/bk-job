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
import com.tencent.bk.job.common.exception.AbortedException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.quota.ResourceQuotaCheckResultEnum;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceExecuteObjects;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * dryRun（预检）返回点单测。
 * <p>
 * <b>这个测试类存在的唯一目的：锁住"预检不写数据"这条性质。</b>
 * 带审批的接口在创建审批任务时用 dryRun 触发完整业务校验，如果日后有人在返回点之上插入写操作
 * （落作业实例、缓存密码、写操作日志、发 MQ 启动事件），预检就会静默地变成一次真实执行 ——
 * 用户还没审批，作业已经跑了。该失效模式没有任何外部症状，只能靠单测拦住，所以这里断言的是
 * "这些 mock 一次都没被调用"，而不是某个返回值。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TaskExecuteServiceDryRunTest {

    private static final long APP_ID = 100L;
    private static final long ACCOUNT_ID = 9001L;
    private static final long HOST_ID = 1001L;
    private static final String DANGEROUS_SUMMARY = "dry-run-step 命中高危规则：rm -rf";

    @Mock
    private AccountService accountService;
    @Mock
    private TaskInstanceService taskInstanceService;
    @Mock
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    @Mock
    private TaskPlanService taskPlanService;
    @Mock
    private TaskInstanceVariableService taskInstanceVariableService;
    @Mock
    private TaskOperationLogService taskOperationLogService;
    @Mock
    private ScriptService scriptService;
    @Mock
    private StepInstanceService stepInstanceService;
    @Mock
    private ServiceUserResource userResource;
    @Mock
    private ExecuteAuthService executeAuthService;
    @Mock
    private DangerousScriptCheckService dangerousScriptCheckService;
    @Mock
    private JobExecuteConfig jobExecuteConfig;
    @Mock
    private TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    @Mock
    private RollingConfigService rollingConfigService;
    @Mock
    private ServiceTaskTemplateResource taskTemplateResource;
    @Mock
    private TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor;
    @Mock
    private RunningJobResourceQuotaManager runningJobResourceQuotaManager;
    @Mock
    private HostService hostService;
    @Mock
    private CustomPasswordCache customPasswordCache;
    @Mock
    private TenantService tenantService;

    private TaskExecuteServiceImpl service;
    private User operator;

    /**
     * checkRunningJobQuotaLimit 通过静态单例拿 AppScopeMappingService，单测里必须先注册一个。
     * register 是"首次生效、后续忽略"的语义，多个测试类并存也不会互相覆盖。
     */
    @BeforeAll
    static void registerGlobalAppScopeMappingService() {
        AppScopeMappingService mappingService = mock(AppScopeMappingService.class);
        when(mappingService.getScopeByAppId(anyLong())).thenReturn(new ResourceScope("biz", "2"));
        GlobalAppScopeMappingService.register(mappingService);
    }

    @BeforeEach
    void setUp() {
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
            tenantService
        );
        operator = new User("tenant-1", "admin", "admin");
        stubPassThroughChecks();
    }

    // ========================================================================
    // 快速执行脚本链路
    // ========================================================================

    @Test
    @DisplayName("dryRun=true：走完全部校验后在 saveTaskInstance 之前返回，不产生任何写操作")
    void fastTaskDryRun_noWriteOperations() {
        FastTaskDTO fastTask = scriptFastTask(true);

        TaskInstanceDTO result = service.executeFastTask(fastTask);

        assertThat(result).isNotNull();
        // 作业实例未落库 —— 没有 id 就说明没走 addTaskInstance
        assertThat(result.getId()).isNull();

        // 逐项对应方案 §6.3 的副作用清单
        verifyNoInteractions(taskInstanceService);
        verifyNoInteractions(taskOperationLogService);
        verifyNoInteractions(stepInstanceService);
        verifyNoInteractions(taskExecuteMQEventDispatcher);
        verifyNoInteractions(customPasswordCache);
        verifyNoInteractions(taskInstanceVariableService);
        verify(runningJobResourceQuotaManager, never()).addJob(anyString(), any(), anyLong());
    }

    @Test
    @DisplayName("dryRun=true：校验与鉴权照常真实执行（预检的全部价值所在）")
    void fastTaskDryRun_stillRunsValidationAndAuth() {
        FastTaskDTO fastTask = scriptFastTask(true);

        service.executeFastTask(fastTask);

        // 配额、驱逐、账号、执行对象解析、滚动批次、IAM 鉴权都必须真实走一遍，
        // 否则用户会在审批通过后才拿到失败
        verify(runningJobResourceQuotaManager).checkResourceQuotaLimit(any(), any());
        verify(taskEvictPolicyExecutor).shouldEvictTask(any());
        verify(accountService).getAccountPreferCache(anyLong(), any(), any(), any());
        verify(taskInstanceExecuteObjectProcessor).processExecuteObjects(any(), any(), any());
        verify(rollingConfigService).validateRollingBatchCountForFastJob(any());
        verify(executeAuthService).authAccountExecutable(any(), any(AppResourceScope.class), anyLong());
        verify(executeAuthService).authFastExecuteScript(any(), any(AppResourceScope.class), any());
    }

    @Test
    @DisplayName("dryRun=true：鉴权不通过时抛权限异常，预检不得放过越权请求")
    void fastTaskDryRun_authFailStillThrows() {
        when(executeAuthService.authAccountExecutable(any(), any(AppResourceScope.class), anyLong()))
            .thenReturn(AuthResult.fail(operator));
        FastTaskDTO fastTask = scriptFastTask(true);

        assertThatThrownBy(() -> service.executeFastTask(fastTask))
            .isInstanceOf(PermissionDeniedException.class);

        verifyNoInteractions(taskInstanceService);
    }

    @Test
    @DisplayName("dryRun 未显式设置时默认为 false：不得因为漏传参数就把真实执行变成空跑")
    void fastTaskDryRunDefaultsToFalse() {
        assertThat(FastTaskDTO.builder().build().getDryRun()).isFalse();
    }

    // ========================================================================
    // 高危脚本检查：预检期不写 dangerous_record，但拦截照常生效
    // ========================================================================

    @Test
    @DisplayName("dryRun=true 命中高危规则（非拦截级）：不写 dangerous_record，命中概要随返回结果带回")
    void fastTaskDryRun_dangerousRuleHit_noDangerousRecordWritten() {
        stubDangerousRuleHit(false);
        FastTaskDTO fastTask = scriptFastTask(true);

        TaskInstanceDTO result = service.executeFastTask(fastTask);

        // 同一次操作会先预检、审批通过后再执行，预检也写库会让高危统计翻倍
        verify(dangerousScriptCheckService, never()).saveDangerousRecord(any(), any(), any());
        // 命中结果不能因为不写库就丢掉，它要进审批单据的概要
        assertThat(result.getStepInstances()).hasSize(1);
        assertThat(result.getStepInstances().get(0).getDangerousCheckSummary())
            .isEqualTo(DANGEROUS_SUMMARY);
    }

    @Test
    @DisplayName("dryRun=true 命中拦截级高危规则：照常拦下，不因为不写库而弱化门禁")
    void fastTaskDryRun_fatalDangerousRule_stillIntercepts() {
        stubDangerousRuleHit(true);
        FastTaskDTO fastTask = scriptFastTask(true);

        assertThatThrownBy(() -> service.executeFastTask(fastTask))
            .isInstanceOf(AbortedException.class)
            .satisfies(e -> {
                AbortedException aborted = (AbortedException) e;
                assertThat(aborted.getErrorCode())
                    .isEqualTo(ErrorCode.DANGEROUS_SCRIPT_FORBIDDEN_EXECUTION);
                // 高危信息必须回传给用户，否则他不知道被拦在哪条规则上
                assertThat(aborted.getErrorParams()).containsExactly(DANGEROUS_SUMMARY);
            });

        verify(dangerousScriptCheckService, never()).saveDangerousRecord(any(), any(), any());
        verifyNoInteractions(taskInstanceService);
    }

    @Test
    @DisplayName("dryRun=false 命中高危规则：dangerous_record 照常落库（正式执行行为不变）")
    void fastTaskRealRun_dangerousRuleHit_stillSavesDangerousRecord() {
        stubDangerousRuleHit(true);
        FastTaskDTO fastTask = scriptFastTask(false);

        assertThatThrownBy(() -> service.executeFastTask(fastTask))
            .isInstanceOf(AbortedException.class);

        verify(dangerousScriptCheckService).saveDangerousRecord(any(), any(), any());
    }

    // ========================================================================
    // dryRun 与 skipAuth 互斥
    // ========================================================================

    @Test
    @DisplayName("dryRun + skipAuth 同时为 true → 抛 IllegalStateException（预检必须真实鉴权）")
    void dryRunWithSkipAuth_throws() {
        TaskExecuteParam param = TaskExecuteParam.builder()
            .planId(1L)
            .operator(operator)
            .dryRun(true)
            .skipAuth(true)
            .build();

        assertThatThrownBy(param::assertDryRunNotSkipAuth)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("现有 skipAuth=true 的调用方（dryRun=false）不被断言误伤")
    void skipAuthWithoutDryRun_passes() {
        TaskExecuteParam param = TaskExecuteParam.builder()
            .planId(1L)
            .operator(operator)
            .skipAuth(true)
            .build();

        param.assertDryRunNotSkipAuth();
    }

    // ========================================================================
    // 测试辅助方法
    // ========================================================================

    /**
     * 把预检路径上的只读校验全部 stub 成"通过"，让测试聚焦于返回点行为。
     * 任何一项没 stub 都会让校验提前抛异常，从而掩盖真正要断言的写操作缺失。
     */
    private void stubPassThroughChecks() {
        when(runningJobResourceQuotaManager.checkResourceQuotaLimit(any(), any()))
            .thenReturn(ResourceQuotaCheckResultEnum.NO_LIMIT);
        when(taskEvictPolicyExecutor.shouldEvictTask(any())).thenReturn(false);

        AccountDTO account = new AccountDTO();
        account.setId(ACCOUNT_ID);
        account.setAccount("root");
        account.setAlias("root");
        when(accountService.getAccountPreferCache(anyLong(), any(), any(), any()))
            .thenReturn(account);

        TaskInstanceExecuteObjects executeObjects = new TaskInstanceExecuteObjects();
        executeObjects.setWhiteHostAllowActions(Collections.emptyMap());
        when(taskInstanceExecuteObjectProcessor.processExecuteObjects(any(), any(), any()))
            .thenReturn(executeObjects);

        // 步骤上限校验取的是 int 原始值，mock 默认 0 会把 1 台目标机也判成超限
        when(jobExecuteConfig.getScriptTaskMaxTargetServer()).thenReturn(10000);

        when(executeAuthService.authAccountExecutable(any(), any(AppResourceScope.class), anyLong()))
            .thenReturn(AuthResult.pass(operator));
        when(executeAuthService.authFastExecuteScript(any(), any(AppResourceScope.class), any()))
            .thenReturn(AuthResult.pass(operator));
    }

    /**
     * 让高危脚本检查命中一条规则。
     *
     * @param intercept 命中的规则是否为拦截级（FATAL）
     */
    private void stubDangerousRuleHit(boolean intercept) {
        ServiceScriptCheckResultItemDTO item = new ServiceScriptCheckResultItemDTO();
        item.setRuleId(1L);
        item.setRuleExpression("rm -rf");
        when(dangerousScriptCheckService.check(anyString(), any(), any()))
            .thenReturn(Collections.singletonList(item));
        when(dangerousScriptCheckService.summaryDangerousScriptCheckResult(any(), any()))
            .thenReturn(DANGEROUS_SUMMARY);
        when(dangerousScriptCheckService.shouldIntercept(anyString(), any())).thenReturn(intercept);
    }

    private FastTaskDTO scriptFastTask(boolean dryRun) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(APP_ID);
        taskInstance.setName("dry-run-task");
        taskInstance.setOperator(operator.getUsername());
        taskInstance.setAppCode("test-app-code");

        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setAppId(APP_ID);
        stepInstance.setName("dry-run-step");
        stepInstance.setOperator(operator.getUsername());
        stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        stepInstance.setAccountId(ACCOUNT_ID);
        // 自定义脚本：不带 scriptId/scriptVersionId，checkAndSetScript 走 CUSTOM 分支
        stepInstance.setScriptContent("ZWNobyAx");
        stepInstance.setTargetExecuteObjects(staticHost(HOST_ID));

        return FastTaskDTO.builder()
            .taskInstance(taskInstance)
            .stepInstance(stepInstance)
            .operator(operator)
            .startTask(true)
            .dryRun(dryRun)
            .build();
    }

    /**
     * 真实链路里 executeObjects 由 processExecuteObjects 填充，这里 processor 被 mock 掉，
     * 需手工把两个字段都设上，还原"执行对象已解析完"的状态，否则 checkStepInstance 会先以
     * 目标为空失败，掩盖住真正要断言的返回点行为。
     */
    private static ExecuteTargetDTO staticHost(long hostId) {
        ExecuteTargetDTO target = new ExecuteTargetDTO();
        HostDTO host = new HostDTO(hostId);
        target.setStaticIpList(Collections.singletonList(host));
        target.setExecuteObjects(Collections.singletonList(new ExecuteObject(host)));
        return target;
    }
}
