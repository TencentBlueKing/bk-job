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

package com.tencent.bk.job.crontab.model.dto;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class CronJobInfoDTOTest {

    private static final String VALID_CRON_EXPRESSION = "0/5 * * * *";

    /**
     * 构造一个校验可通过的定时任务：cron 表达式模式、指定执行方案、未开启提前通知
     */
    private CronJobInfoDTO buildValidCronJob() {
        CronJobInfoDTO cronJob = new CronJobInfoDTO();
        cronJob.setName("cron-job");
        cronJob.setTaskPlanId(1000L);
        cronJob.setCronExpression(VALID_CRON_EXPRESSION);
        return cronJob;
    }

    private UserRoleInfoDTO buildValidNotifyUser() {
        UserRoleInfoDTO notifyUser = new UserRoleInfoDTO();
        notifyUser.setUserList(Collections.singletonList("admin"));
        notifyUser.setRoleList(Collections.emptyList());
        return notifyUser;
    }

    private void assertErrorCode(CronJobInfoDTO cronJob, int expectedErrorCode) {
        assertThatExceptionOfType(InvalidParamException.class)
            .isThrownBy(cronJob::validate)
            .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    @Test
    @DisplayName("校验通过：cron表达式与执行方案有效时不抛异常")
    void validateSuccess() {
        CronJobInfoDTO cronJob = buildValidCronJob();

        assertThatCode(cronJob::validate).doesNotThrowAnyException();
        assertThat(cronJob.getEnable()).isFalse();
        assertThat(cronJob.getNotifyOffset()).isEqualTo(0L);
        assertThat(cronJob.getExecuteTime()).isNull();
    }

    @Test
    @DisplayName("校验通过：配置了提前通知且通知人、通知渠道齐备")
    void validateSuccessWithNotifyConfig() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setNotifyOffset(300L);
        cronJob.setNotifyUser(buildValidNotifyUser());
        cronJob.setNotifyChannel(Collections.singletonList("mail"));

        assertThatCode(cronJob::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("配置了提前通知但通知人为空：1245012")
    void validateNotifyUserEmpty() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setNotifyOffset(300L);
        cronJob.setNotifyChannel(Collections.singletonList("mail"));

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_NOTIFY_USER_EMPTY);
    }

    @Test
    @DisplayName("配置了提前通知但通知人为null：1245012（不抛NPE）")
    void validateNotifyUserNull() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setNotifyOffset(300L);
        cronJob.setNotifyUser(null);
        cronJob.setNotifyChannel(Collections.singletonList("mail"));

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_NOTIFY_USER_EMPTY);
    }

    @Test
    @DisplayName("配置了提前通知但通知渠道为空：1245013")
    void validateNotifyChannelEmpty() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setNotifyOffset(300L);
        cronJob.setNotifyUser(buildValidNotifyUser());
        cronJob.setNotifyChannel(Collections.emptyList());

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_NOTIFY_CHANNEL_EMPTY);
    }

    @Test
    @DisplayName("执行方案与脚本均未有效指定：1245014")
    void validatePlanAndScriptMissing() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setTaskPlanId(null);
        cronJob.setScriptId(null);
        cronJob.setScriptVersionId(null);

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_PLAN_OR_SCRIPT_INVALID);
    }

    @Test
    @DisplayName("cron表达式不合法：1245015")
    void validateCronExpressionInvalid() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setCronExpression("invalid cron expression");

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_CRON_EXPRESSION_INVALID);
    }

    @Test
    @DisplayName("既无cron表达式也无晚于当前时间的执行时间：1245016")
    void validateExecuteTimeConfigInvalid() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setCronExpression(null);
        cronJob.setExecuteTime(null);

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_EXECUTE_TIME_CONFIG_INVALID);
    }

    @Test
    @DisplayName("结束时间早于结束前通知时间：1245010")
    void validateEndNotifyTimeAlreadyPassed() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setNotifyOffset(600L);
        cronJob.setNotifyUser(buildValidNotifyUser());
        cronJob.setNotifyChannel(Collections.singletonList("mail"));
        cronJob.setEndTime(DateUtils.currentTimeSeconds() + 300L);

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_END_NOTIFY_TIME_ALREADY_PASSED);
    }

    @Test
    @DisplayName("单次执行时间早于执行前通知时间：1245011")
    void validateExecuteNotifyTimeAlreadyPassed() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setCronExpression(null);
        cronJob.setExecuteTime(DateUtils.currentTimeSeconds() + 300L);
        cronJob.setNotifyOffset(600L);
        cronJob.setNotifyUser(buildValidNotifyUser());
        cronJob.setNotifyChannel(Collections.singletonList("mail"));

        assertErrorCode(cronJob, ErrorCode.CRON_JOB_EXECUTE_NOTIFY_TIME_ALREADY_PASSED);
    }

    @Test
    @DisplayName("指定脚本(无执行方案)时校验通过")
    void validateSuccessWithScript() {
        CronJobInfoDTO cronJob = buildValidCronJob();
        cronJob.setTaskPlanId(null);
        cronJob.setScriptId("script-id");
        cronJob.setScriptVersionId(2000L);
        cronJob.setTaskTemplateId(3000L);

        assertThatCode(cronJob::validate).doesNotThrowAnyException();
        assertThat(cronJob.getTaskTemplateId()).isNull();
        List<String> notifyChannel = cronJob.getNotifyChannel();
        assertThat(notifyChannel).isEmpty();
    }
}
