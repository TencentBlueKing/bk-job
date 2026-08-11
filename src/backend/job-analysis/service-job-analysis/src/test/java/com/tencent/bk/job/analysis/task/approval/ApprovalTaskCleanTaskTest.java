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

package com.tencent.bk.job.analysis.task.approval;

import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.dao.ApprovalTaskDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 清理任务的删除边界与保留期取值单测。
 * <p>
 * 只测被分布式锁包住的那段清理逻辑：锁本身由 DistributedUniqueTask 负责，
 * 而"保留期配错时删到哪一天"才是这里唯一可能误删审批凭据的地方。
 */
class ApprovalTaskCleanTaskTest {

    private ApprovalProperties approvalProperties;
    private ApprovalTaskDAO approvalTaskDAO;
    private ApprovalTaskCleanTask cleanTask;

    @BeforeEach
    void setUp() {
        approvalProperties = new ApprovalProperties();
        approvalTaskDAO = mock(ApprovalTaskDAO.class);
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        cleanTask = new ApprovalTaskCleanTask(redisTemplate, approvalProperties, approvalTaskDAO);
    }

    @Test
    @DisplayName("保留期配成 0 或负数时回落到默认 30 天，避免把当天的审批凭据一起删掉")
    void givenIllegalMaxKeepDaysThenFallbackToDefault() {
        approvalProperties.setMaxKeepDays(0);
        assertThat(cleanTask.resolveMaxKeepDays()).isEqualTo(30);

        approvalProperties.setMaxKeepDays(-1);
        assertThat(cleanTask.resolveMaxKeepDays()).isEqualTo(30);

        approvalProperties.setMaxKeepDays(null);
        assertThat(cleanTask.resolveMaxKeepDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("保留期独立配置生效，不受 AI 会话保留期影响")
    void givenConfiguredMaxKeepDaysThenUseIt() {
        approvalProperties.setMaxKeepDays(7);

        assertThat(cleanTask.resolveMaxKeepDays()).isEqualTo(7);
    }

    @Test
    @DisplayName("删除时间上界落在保留期首日的零点之前，保留期内的记录一条都不删")
    void givenMaxKeepDaysThenDeleteBeforeDayStart() {
        when(approvalTaskDAO.deleteByCreateTimeBefore(anyLong(), anyInt())).thenReturn(0);

        cleanTask.cleanByMaxKeepDays(30);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(approvalTaskDAO).deleteByCreateTimeBefore(captor.capture(), anyInt());
        long now = System.currentTimeMillis();
        long thirtyDays = TimeUnit.DAYS.toMillis(30);
        // 上界是"今天往前推 30 天那一天的零点"，因此必落在 [now-31d, now-30d] 之间
        assertThat(captor.getValue()).isLessThanOrEqualTo(now - thirtyDays);
        assertThat(captor.getValue()).isGreaterThan(now - thirtyDays - TimeUnit.DAYS.toMillis(1));
    }

    @Test
    @DisplayName("分批删除直到删不动为止，不会因为单批上限而漏删")
    void givenMoreThanOneBatchThenDeleteUntilEmpty() {
        when(approvalTaskDAO.deleteByCreateTimeBefore(anyLong(), anyInt()))
            .thenReturn(1000)
            .thenReturn(0);

        cleanTask.cleanByMaxKeepDays(30);

        verify(approvalTaskDAO, times(2)).deleteByCreateTimeBefore(anyLong(), anyInt());
    }
}
