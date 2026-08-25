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

package com.tencent.bk.job.crontab.api.esb.v4;

import com.tencent.bk.audit.context.AuditContext;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.model.esb.v4.V4CronStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.service.CronGlobalVarSummaryBuilder;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.V4SaveCronRequestConverter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 保存定时任务 OpenAPI 的预检分支。
 * <p>
 * 审批链路的预检与放行走的是这同一个方法，只有 dryRun 取值不同，<b>这是"预检结果与实际执行不漂移"的
 * 结构保证</b>；预检必须只回带操作概要而不落定时任务，否则审批还没通过任务就已经建出来了。
 */
class OpenApiSaveCronV4ResourceImplTest {

    private static final String USERNAME = "admin";
    private static final String APP_CODE = "bk_ai";

    /**
     * 用户提交的 UNIX 形态定时规则，也是概要里该展示的形态
     */
    private static final String CRON_EXPRESSION_UNIX = "30 10 8 * *";

    /**
     * 转换后落库的 Quartz 形态，由 V4SaveCronRequestConverter 产出（此处被 mock，直接给结果）
     */
    private static final String CRON_EXPRESSION_QUARTZ = "0 30 10 8 * ? *";

    private CronJobService cronJobService;

    private V4SaveCronRequestConverter requestConverter;

    private CronGlobalVarSummaryBuilder globalVarSummaryBuilder;

    private OpenApiSaveCronV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        cronJobService = mock(CronJobService.class);
        requestConverter = mock(V4SaveCronRequestConverter.class);
        when(requestConverter.convert(any(), any())).thenAnswer(invocation ->
            buildCronJobInfo(invocation.getArgument(0, V4SaveCronRequest.class)));
        globalVarSummaryBuilder = mock(CronGlobalVarSummaryBuilder.class);
        resource = new OpenApiSaveCronV4ResourceImpl(cronJobService, requestConverter, globalVarSummaryBuilder);
        // 操作人由网关鉴权后经拦截器写入上下文，实现类只从上下文取
        JobContextUtil.setUser(new User("tenant_a", USERNAME, USERNAME));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("新增场景预检时只回带操作概要，不落定时任务")
    void givenDryRunCreateThenReturnSummaryOnly() {
        when(cronJobService.dryRunCreateCronJobInfo(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        EsbV4Response<V4CronJobDTO> response = callSaveCron(baseCreateRequest(), true);

        assertThat(response.getData()).isNull();
        ResolvedSummary summary = response.getDryRunSummary();
        assertThat(summary.getName()).isEqualTo("test_cron");
        assertThat(summaryFields(summary)).containsEntry("operation", "CREATE");
        // 定时规则按用户提交的 UNIX 形态展示，而不是转换后落库的 Quartz 形态
        assertThat(summaryFields(summary)).containsEntry("cron_expression", CRON_EXPRESSION_UNIX);
        // 新增时还没有定时任务 ID，概要里不该出现
        assertThat(summaryFields(summary)).doesNotContainKey("cron_id");
        verify(cronJobService, never()).createCronJobInfo(any(), any());
    }

    @Test
    @DisplayName("预检概要带上全局变量：定时任务到点就拿这套变量去跑")
    void givenDryRunThenFillGlobalVars() {
        when(cronJobService.dryRunCreateCronJobInfo(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        doAnswer(invocation -> {
            ResolvedSummary summary = invocation.getArgument(0, ResolvedSummary.class);
            ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
            globalVar.setName("version");
            summary.addGlobalVar(globalVar);
            return null;
        }).when(globalVarSummaryBuilder).fillGlobalVars(any(), any(), any());

        EsbV4Response<V4CronJobDTO> response = callSaveCron(baseCreateRequest(), true);

        verify(globalVarSummaryBuilder).fillGlobalVars(any(), any(), any());
        assertThat(response.getDryRunSummary().getGlobalVars()).hasSize(1);
    }

    @Test
    @DisplayName("定时规则转换失败时退回原表达式，不让预检整个失败")
    void givenUnconvertibleCronExpressionThenFallbackToRawValue() {
        doAnswer(invocation -> buildCronJobInfo(invocation.getArgument(0, V4SaveCronRequest.class), "not-a-cron"))
            .when(requestConverter).convert(any(), any());
        when(cronJobService.dryRunCreateCronJobInfo(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));

        EsbV4Response<V4CronJobDTO> response = callSaveCron(baseCreateRequest(), true);

        assertThat(summaryFields(response.getDryRunSummary()))
            .containsEntry("cron_expression", "not-a-cron");
    }

    @Test
    @DisplayName("更新场景预检时概要里带上被改的定时任务 ID")
    void givenDryRunUpdateThenSummaryContainsCronId() {
        when(cronJobService.dryRunUpdateCronJobInfo(any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        V4SaveCronRequest request = baseCreateRequest();
        request.setId(88L);

        EsbV4Response<V4CronJobDTO> response = callSaveCron(request, true);

        assertThat(summaryFields(response.getDryRunSummary()))
            .containsEntry("operation", "UPDATE")
            .containsEntry("cron_id", "88");
        verify(cronJobService, never()).updateCronJobInfo(any(), any());
    }

    @Test
    @DisplayName("dryRun 未传时按正式执行处理，新增出来的定时任务处于暂停状态")
    void givenNullDryRunThenCreateAndReturnStopping() {
        when(cronJobService.createCronJobInfo(any(), any())).thenAnswer(invocation -> {
            CronJobInfoDTO created = invocation.getArgument(1, CronJobInfoDTO.class);
            created.setId(66L);
            // 新增出来的定时任务默认不启动，需要调启停接口才会真正开始调度
            created.setEnable(false);
            return created;
        });

        EsbV4Response<V4CronJobDTO> response = callSaveCron(baseCreateRequest(), null);

        assertThat(response.getDryRunSummary()).isNull();
        assertThat(response.getData().getId()).isEqualTo(66L);
        assertThat(response.getData().getStatus()).isEqualTo(V4CronStatusEnum.DISABLED.getStatus());
    }

    @Test
    @DisplayName("传了 ID 时走更新而非新增")
    void givenIdThenUpdateInsteadOfCreate() {
        when(cronJobService.updateCronJobInfo(any(), any())).thenAnswer(invocation -> {
            CronJobInfoDTO updated = invocation.getArgument(1, CronJobInfoDTO.class);
            updated.setEnable(true);
            return updated;
        });
        V4SaveCronRequest request = baseCreateRequest();
        request.setId(88L);

        EsbV4Response<V4CronJobDTO> response = callSaveCron(request, null);

        assertThat(response.getData().getStatus()).isEqualTo(V4CronStatusEnum.ENABLED.getStatus());
        verify(cronJobService, never()).createCronJobInfo(any(), any());
    }

    /**
     * 审计动作在方法内按新增/更新动态改写，脱离 Spring 容器时审计上下文未初始化，需要接管
     */
    private EsbV4Response<V4CronJobDTO> callSaveCron(V4SaveCronRequest request, Boolean dryRun) {
        AuditContext auditContext = mock(AuditContext.class);
        try (var mocked = mockStatic(AuditContext.class)) {
            mocked.when(AuditContext::current).thenReturn(auditContext);
            return resource.saveCron(USERNAME, APP_CODE, dryRun, request);
        }
    }

    private Map<String, String> summaryFields(ResolvedSummary summary) {
        return summary.getFields().stream()
            .filter(field -> field.getValue() != null)
            .collect(Collectors.toMap(ResolvedSummary.ResolvedField::getLabel,
                ResolvedSummary.ResolvedField::getValue));
    }

    private V4SaveCronRequest baseCreateRequest() {
        V4SaveCronRequest request = new V4SaveCronRequest();
        request.setAppId(2L);
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("test_cron");
        request.setPlanId(100L);
        request.setCronExpression(CRON_EXPRESSION_UNIX);
        return request;
    }

    private CronJobInfoDTO buildCronJobInfo(V4SaveCronRequest request) {
        return buildCronJobInfo(request, CRON_EXPRESSION_QUARTZ);
    }

    private CronJobInfoDTO buildCronJobInfo(V4SaveCronRequest request, String cronExpression) {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(request.getId());
        cronJobInfo.setAppId(request.getAppId());
        cronJobInfo.setName(request.getName());
        cronJobInfo.setTaskPlanId(request.getPlanId());
        cronJobInfo.setCronExpression(cronExpression);
        return cronJobInfo;
    }
}
