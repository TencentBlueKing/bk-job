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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.dao.CronJobDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.mq.CrontabMQEventDispatcher;
import com.tencent.bk.job.crontab.service.CustomNotifyPolicyService;
import com.tencent.bk.job.crontab.service.ExecuteTaskService;
import com.tencent.bk.job.crontab.service.HostService;
import com.tencent.bk.job.crontab.service.QuartzService;
import com.tencent.bk.job.crontab.service.TaskPlanService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzTaskHandler;
import com.tencent.bk.job.manage.api.inner.ServiceTenantResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 定时任务 dryRun（预检）返回点单测。
 * <p>
 * <b>这个测试类存在的唯一目的：锁住"预检不写数据"这条性质。</b>
 * 带审批的接口在创建审批任务时用 dryRun 触发完整业务校验，如果日后有人在返回点之上插入写操作
 * （落定时任务、改状态、注册调度），预检就会静默地变成一次真实操作 —— 用户还没审批，定时任务已经建好并跑起来了。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CronJobServiceDryRunTest {

    private static final long APP_ID = 2L;
    private static final long CRON_JOB_ID = 3001L;
    private static final long PLAN_ID = 4001L;
    private static final String CRON_NAME = "dry-run-cron";

    @Mock
    private CronJobDAO cronJobDAO;
    @Mock
    private AbstractQuartzTaskHandler quartzTaskHandler;
    @Mock
    private QuartzService quartzService;
    @Mock
    private TaskPlanService taskPlanService;
    @Mock
    private CronAuthService cronAuthService;
    @Mock
    private ExecuteTaskService executeTaskService;
    @Mock
    private HostService hostService;
    @Mock
    private CrontabMQEventDispatcher crontabMQEventDispatcher;
    @Mock
    private BatchCronJobServiceImpl batchCronJobService;
    @Mock
    private ServiceTenantResource tenantResource;
    @Mock
    private CustomNotifyPolicyService customNotifyPolicyService;

    private CronJobServiceImpl service;
    private User operator;

    @BeforeEach
    void setUp() {
        service = new CronJobServiceImpl(
            cronJobDAO,
            quartzTaskHandler,
            quartzService,
            taskPlanService,
            cronAuthService,
            executeTaskService,
            hostService,
            crontabMQEventDispatcher,
            batchCronJobService,
            tenantResource,
            customNotifyPolicyService
        );
        operator = new User("tenant-1", "admin", "admin");

        when(cronJobDAO.checkCronJobName(anyLong(), anyLong(), anyString())).thenReturn(true);
        when(cronAuthService.authCreateCron(any(User.class), any(AppResourceScope.class)))
            .thenReturn(AuthResult.pass(operator));
        when(cronAuthService.authManageCron(any(User.class), any(AppResourceScope.class), anyLong(), any()))
            .thenReturn(AuthResult.pass(operator));
        ServiceTaskPlanDTO plan = new ServiceTaskPlanDTO();
        plan.setId(PLAN_ID);
        plan.setTaskTemplateId(5001L);
        when(taskPlanService.getPlanBasicInfoById(APP_ID, PLAN_ID)).thenReturn(plan);
    }

    // ========================================================================
    // 保存定时任务（创建 / 更新）
    // ========================================================================

    @Test
    @DisplayName("预检创建：走完全部校验后在落库之前返回，不写库、不注册调度、不注册权限中心资源")
    void dryRunCreate_noWriteOperations() {
        CronJobInfoDTO result = service.dryRunCreateCronJobInfo(operator, buildCronJobInfo(null));

        verify(cronJobDAO, never()).insertCronJob(any());
        verify(cronAuthService, never()).registerCron(any(User.class), anyLong(), anyString());
        verifyNoInteractions(customNotifyPolicyService);
        verifyNoInteractions(crontabMQEventDispatcher);
        // 未落库 —— 没有 id 就说明没走 insertCronJob
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
    }

    @Test
    @DisplayName("预检创建：创建权限与执行方案的执行权限照常真实校验")
    void dryRunCreate_stillRunsAuth() {
        service.dryRunCreateCronJobInfo(operator, buildCronJobInfo(null));

        verify(cronAuthService).authCreateCron(any(User.class), any(AppResourceScope.class));
        verify(executeTaskService).authExecuteTask(eq(APP_ID), eq(PLAN_ID), any(), eq(CRON_NAME), any(), anyString());
    }

    @Test
    @DisplayName("预检创建：鉴权不通过时抛权限异常，预检不得放过越权请求")
    void dryRunCreate_authFailStillThrows() {
        when(cronAuthService.authCreateCron(any(User.class), any(AppResourceScope.class)))
            .thenReturn(AuthResult.fail(operator));

        assertThatThrownBy(() -> service.dryRunCreateCronJobInfo(operator, buildCronJobInfo(null)))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("预检创建：任务名已存在时照常失败，让用户在发起阶段就拿到错误")
    void dryRunCreate_duplicatedNameStillThrows() {
        when(cronJobDAO.checkCronJobName(anyLong(), anyLong(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.dryRunCreateCronJobInfo(operator, buildCronJobInfo(null)))
            .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("预检创建：执行方案不存在时照常失败")
    void dryRunCreate_planNotExistStillThrows() {
        when(taskPlanService.getPlanBasicInfoById(APP_ID, PLAN_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.dryRunCreateCronJobInfo(operator, buildCronJobInfo(null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("预检更新：不更新定时任务、不变更调度")
    void dryRunUpdate_noWriteOperations() {
        when(cronJobDAO.getCronJobById(CRON_JOB_ID)).thenReturn(buildCronJobInfo(CRON_JOB_ID));

        CronJobInfoDTO result = service.dryRunUpdateCronJobInfo(operator, buildCronJobInfo(CRON_JOB_ID));

        verify(cronJobDAO, never()).updateCronJobById(any());
        verify(customNotifyPolicyService, never())
            .createOrUpdateCronJobCustomNotifyPolicy(anyLong(), any(CronJobInfoDTO.class));
        verifyNoInteractions(crontabMQEventDispatcher);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("预检更新：本次没传的执行方案与变量取值按原值补齐，单据上不能打出 null")
    void dryRunUpdate_fillsUnchangedFieldsForApproval() {
        CronJobInfoDTO originCron = buildCronJobInfo(CRON_JOB_ID);
        originCron.setVariableValue(Collections.singletonList(buildVariable("version", "v1.2.3")));
        when(cronJobDAO.getCronJobById(CRON_JOB_ID)).thenReturn(originCron);
        // 更新接口允许只传要改的字段，这里只改了定时规则
        CronJobInfoDTO updateReq = new CronJobInfoDTO();
        updateReq.setId(CRON_JOB_ID);
        updateReq.setAppId(APP_ID);
        updateReq.setName(CRON_NAME);
        updateReq.setCronExpression("0 0 3 * * ?");

        CronJobInfoDTO result = service.dryRunUpdateCronJobInfo(operator, updateReq);

        assertThat(result.getTaskPlanId())
            .as("单据展示的是更新之后实际生效的样子，沿用原值的执行方案不能变成 null")
            .isEqualTo(PLAN_ID);
        assertThat(result.getVariableValue()).hasSize(1);
        assertThat(result.getVariableValue().get(0).getValue()).isEqualTo("v1.2.3");
    }

    // ========================================================================
    // 启停定时任务
    // ========================================================================

    @Test
    @DisplayName("预检启用：执行方案的执行权限照常校验，但不改状态、不注册调度")
    void dryRunEnable_noWriteOperationsButAuthChecked() {
        when(cronJobDAO.getCronJobById(APP_ID, CRON_JOB_ID)).thenReturn(buildCronJobInfo(CRON_JOB_ID));

        Boolean result = service.dryRunChangeCronJobEnableStatus(operator, APP_ID, CRON_JOB_ID, true);

        assertThat(result).isTrue();
        verify(executeTaskService).authExecuteTask(eq(APP_ID), eq(PLAN_ID), eq(CRON_JOB_ID), eq(CRON_NAME),
            any(), anyString());
        verify(cronJobDAO, never()).updateCronJobById(any());
        verifyNoInteractions(crontabMQEventDispatcher);
    }

    @Test
    @DisplayName("预检停用：不改状态、不变更调度")
    void dryRunDisable_noWriteOperations() {
        when(cronJobDAO.getCronJobById(APP_ID, CRON_JOB_ID)).thenReturn(buildCronJobInfo(CRON_JOB_ID));

        Boolean result = service.dryRunChangeCronJobEnableStatus(operator, APP_ID, CRON_JOB_ID, false);

        assertThat(result).isTrue();
        verify(cronJobDAO, never()).updateCronJobById(any());
        verifyNoInteractions(crontabMQEventDispatcher);
    }

    @Test
    @DisplayName("预检启停：管理权限不通过时抛权限异常")
    void dryRunChangeStatus_authFailStillThrows() {
        when(cronAuthService.authManageCron(any(User.class), any(AppResourceScope.class), anyLong(), any()))
            .thenReturn(AuthResult.fail(operator));

        assertThatThrownBy(() -> service.dryRunChangeCronJobEnableStatus(operator, APP_ID, CRON_JOB_ID, true))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("预检启停：定时任务不存在时照常失败")
    void dryRunChangeStatus_cronNotExistStillThrows() {
        when(cronJobDAO.getCronJobById(APP_ID, CRON_JOB_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.dryRunChangeCronJobEnableStatus(operator, APP_ID, CRON_JOB_ID, true))
            .isInstanceOf(NotFoundException.class);
    }

    private CronJobVariableDTO buildVariable(String name, String value) {
        CronJobVariableDTO variable = new CronJobVariableDTO();
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING);
        variable.setValue(value);
        return variable;
    }

    private CronJobInfoDTO buildCronJobInfo(Long id) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(id);
        cronJobInfo.setAppId(APP_ID);
        cronJobInfo.setName(CRON_NAME);
        cronJobInfo.setTaskPlanId(PLAN_ID);
        cronJobInfo.setCronExpression("0 0 12 * * ?");
        cronJobInfo.setEnable(false);
        cronJobInfo.setLastModifyUser(operator.getUsername());
        return cronJobInfo;
    }
}
