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

import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.v4.resp.V4CronJobDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 启停定时任务 OpenAPI 的预检分支。
 * <p>
 * 审批链路的预检与放行走的是这同一个方法，只有 dryRun 取值不同，<b>这是"预检结果与实际执行不漂移"的
 * 结构保证</b>；预检必须只回带操作概要而不改状态，否则审批还没通过定时任务就已经跑起来了。
 */
class OpenApiUpdateCronStatusV4ResourceImplTest {

    private static final String USERNAME = "admin";
    private static final String APP_CODE = "bk_ai";
    private static final Long APP_ID = 2L;
    private static final Long CRON_ID = 88L;

    private CronJobService cronJobService;

    private OpenApiUpdateCronStatusV4ResourceImpl resource;

    @BeforeEach
    void setUp() {
        cronJobService = mock(CronJobService.class);
        resource = new OpenApiUpdateCronStatusV4ResourceImpl(cronJobService);
        // 操作人由网关鉴权后经拦截器写入上下文，实现类只从上下文取
        JobContextUtil.setUser(new User("tenant_a", USERNAME, USERNAME));
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    @Test
    @DisplayName("预检时只回带操作概要，不改定时任务状态")
    void givenDryRunThenReturnSummaryOnly() {
        when(cronJobService.getCronJobInfoById(APP_ID, CRON_ID)).thenReturn(buildCronJobInfo());

        EsbV4Response<V4CronJobDTO> response =
            resource.updateCronStatus(USERNAME, APP_CODE, true, baseRequest(CronStatusEnum.RUNNING.getStatus()));

        assertThat(response.getData()).isNull();
        verify(cronJobService, never()).changeCronJobEnableStatus(any(), anyLong(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("入参只有 ID 与目标状态，概要必须补出任务名与执行方案供审批人判断影响面")
    void givenDryRunThenSummaryContainsTaskContext() {
        when(cronJobService.getCronJobInfoById(APP_ID, CRON_ID)).thenReturn(buildCronJobInfo());

        EsbV4Response<V4CronJobDTO> response =
            resource.updateCronStatus(USERNAME, APP_CODE, true, baseRequest(CronStatusEnum.RUNNING.getStatus()));

        ResolvedSummary summary = response.getDryRunSummary();
        assertThat(summary.getName()).isEqualTo("test_cron");
        assertThat(summaryFields(summary))
            .containsEntry("cron_id", String.valueOf(CRON_ID))
            .containsEntry("target_status", CronStatusEnum.RUNNING.name())
            .containsEntry("job_plan_id", "100")
            .containsEntry("cron_expression", "0 0 12 * *");
    }

    @Test
    @DisplayName("定时任务不存在时预检直接拒绝，不产出只剩 ID 的概要")
    void givenCronNotExistThenReject() {
        when(cronJobService.getCronJobInfoById(APP_ID, CRON_ID)).thenReturn(null);

        assertThatThrownBy(() ->
            resource.updateCronStatus(USERNAME, APP_CODE, true, baseRequest(CronStatusEnum.RUNNING.getStatus())))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("dryRun 未传时按正式执行处理，返回改后的状态")
    void givenNullDryRunThenChangeStatus() {
        EsbV4Response<V4CronJobDTO> response =
            resource.updateCronStatus(USERNAME, APP_CODE, null, baseRequest(CronStatusEnum.STOPPING.getStatus()));

        assertThat(response.getDryRunSummary()).isNull();
        assertThat(response.getData().getId()).isEqualTo(CRON_ID);
        assertThat(response.getData().getStatus()).isEqualTo(CronStatusEnum.STOPPING.getStatus());
        verify(cronJobService).changeCronJobEnableStatus(any(), eq(APP_ID), eq(CRON_ID), eq(false));
    }

    @Test
    @DisplayName("状态值不在启动与暂停之外时拒绝")
    void givenIllegalStatusThenReject() {
        assertThatThrownBy(() -> resource.updateCronStatus(USERNAME, APP_CODE, true, baseRequest(99)))
            .isInstanceOf(InvalidParamException.class);

        verify(cronJobService, never()).dryRunChangeCronJobEnableStatus(any(), anyLong(), anyLong(), anyBoolean());
    }

    private Map<String, String> summaryFields(ResolvedSummary summary) {
        return summary.getFields().stream()
            .filter(field -> field.getValue() != null)
            .collect(Collectors.toMap(ResolvedSummary.ResolvedField::getLabel,
                ResolvedSummary.ResolvedField::getValue));
    }

    private V4UpdateCronStatusRequest baseRequest(Integer status) {
        V4UpdateCronStatusRequest request = new V4UpdateCronStatusRequest();
        request.setAppId(APP_ID);
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setId(CRON_ID);
        request.setStatus(status);
        return request;
    }

    private CronJobInfoDTO buildCronJobInfo() {
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(CRON_ID);
        cronJobInfo.setAppId(APP_ID);
        cronJobInfo.setName("test_cron");
        cronJobInfo.setTaskPlanId(100L);
        cronJobInfo.setCronExpression("0 0 12 * *");
        return cronJobInfo;
    }
}
