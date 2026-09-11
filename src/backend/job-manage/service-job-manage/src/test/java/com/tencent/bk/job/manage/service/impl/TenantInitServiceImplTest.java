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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskDaemon;
import com.tencent.bk.job.manage.background.sync.BizSetSyncService;
import com.tencent.bk.job.manage.background.sync.BizSyncService;
import com.tencent.bk.job.manage.background.sync.TenantHostSyncService;
import com.tencent.bk.job.manage.background.sync.tenantset.ITenantSetSyncService;
import com.tencent.bk.job.manage.model.dto.TenantInitDoneMark;
import com.tencent.bk.job.manage.service.TenantInitResult;
import com.tencent.bk.job.manage.service.impl.notify.NotifyChannelInitService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TenantInitServiceImpl 测试")
class TenantInitServiceImplTest {

    private static final String TENANT_ID = "tenant-001";
    private static final String INIT_DONE_KEY_PREFIX = "job:manage:tenant:init:done:";
    private static final String EXISTED_MARK =
        "{\"machineIp\":\"127.0.0.1\",\"startTime\":\"2026-08-17 17:38:09\",\"endTime\":\"2026-08-17 17:38:11\"}";
    private static final String DATETIME_REGEX = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private BizSyncService bizSyncService;
    private BizSetSyncService bizSetSyncService;
    private ITenantSetSyncService tenantSetSyncService;
    private TenantHostSyncService tenantHostSyncService;
    private NotifyChannelInitService notifyChannelInitService;
    private BackGroundTaskDaemon backGroundTaskDaemon;
    private TenantInitServiceImpl tenantInitService;

    /**
     * DistributedUniqueTask 经 HeartBeatRedisLock 调用静态的 LockUtils 加锁，需先注入可用的 RedisTemplate，
     * 否则加锁必然失败，走不到初始化步骤
     */
    @BeforeAll
    static void initLockUtils() {
        StringRedisTemplate lockRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> lockValueOperations = mock(ValueOperations.class);
        when(lockRedisTemplate.opsForValue()).thenReturn(lockValueOperations);
        when(lockValueOperations.setIfAbsent(anyString(), anyString(), anyLong(), eq(TimeUnit.MILLISECONDS)))
            .thenReturn(true);
        LockUtils.init(lockRedisTemplate, RedisScript.of("return 1", Long.class));
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        bizSyncService = mock(BizSyncService.class);
        bizSetSyncService = mock(BizSetSyncService.class);
        tenantSetSyncService = mock(ITenantSetSyncService.class);
        tenantHostSyncService = mock(TenantHostSyncService.class);
        notifyChannelInitService = mock(NotifyChannelInitService.class);
        backGroundTaskDaemon = mock(BackGroundTaskDaemon.class);
        tenantInitService = new TenantInitServiceImpl(
            redisTemplate,
            bizSyncService,
            bizSetSyncService,
            tenantSetSyncService,
            tenantHostSyncService,
            notifyChannelInitService,
            backGroundTaskDaemon
        );
    }

    @Test
    @DisplayName("完成标记已存在时跳过初始化，不调用任何下游服务")
    void initTenantOnceShouldSkipWhenMarkExists() {
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + TENANT_ID)).thenReturn(EXISTED_MARK);

        TenantInitResult result = tenantInitService.initTenantOnce(TENANT_ID);

        assertThat(result).isEqualTo(TenantInitResult.ALREADY_DONE);
        verifyNoInteractions(bizSyncService, bizSetSyncService, tenantSetSyncService, tenantHostSyncService,
            notifyChannelInitService, backGroundTaskDaemon);
    }

    @Test
    @DisplayName("完成标记不存在且各步骤均成功时按序执行并写入无过期时间的完成标记")
    void initTenantOnceShouldWriteMarkAfterAllStepsSucceed() {
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + TENANT_ID)).thenReturn(null);

        TenantInitResult result = tenantInitService.initTenantOnce(TENANT_ID);

        assertThat(result).isEqualTo(TenantInitResult.SUCCESS);
        InOrder inOrder = inOrder(bizSyncService, bizSetSyncService, tenantSetSyncService, tenantHostSyncService,
            backGroundTaskDaemon, notifyChannelInitService);
        inOrder.verify(bizSyncService).syncBizFromCMDB(TENANT_ID);
        inOrder.verify(bizSetSyncService).syncBizSetFromCMDB(TENANT_ID);
        inOrder.verify(tenantSetSyncService).syncTenantSetFromCMDB();
        inOrder.verify(tenantHostSyncService).syncAllBizHostsAtOnce(TENANT_ID);
        inOrder.verify(backGroundTaskDaemon).checkAndResumeTaskForTenant(TENANT_ID);
        inOrder.verify(notifyChannelInitService).tryToInitDefaultNotifyChannelsWithSingleTenant(TENANT_ID);
        verify(valueOperations).set(eq(INIT_DONE_KEY_PREFIX + TENANT_ID), anyString());
    }

    @Test
    @DisplayName("完成标记的value为含machineIp、startTime、endTime的JSON串，时间为可读格式且开始不晚于结束")
    void initTenantOnceShouldWriteMarkAsJson() {
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + TENANT_ID)).thenReturn(null);

        tenantInitService.initTenantOnce(TENANT_ID);

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(INIT_DONE_KEY_PREFIX + TENANT_ID), valueCaptor.capture());
        TenantInitDoneMark mark = JsonUtils.fromJson(valueCaptor.getValue(), TenantInitDoneMark.class);
        assertThat(mark.getMachineIp()).isNotBlank();
        assertThat(mark.getStartTime()).matches(DATETIME_REGEX);
        assertThat(mark.getEndTime()).matches(DATETIME_REGEX);
        assertThat(mark.getStartTime()).isLessThanOrEqualTo(mark.getEndTime());
    }

    @Test
    @DisplayName("任一步骤抛异常时返回 FAILED、不写完成标记且不向外抛异常")
    void initTenantOnceShouldNotWriteMarkWhenStepFailed() {
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + TENANT_ID)).thenReturn(null);
        doThrow(new RuntimeException("cmdb unreachable")).when(bizSyncService).syncBizFromCMDB(TENANT_ID);

        TenantInitResult result = tenantInitService.initTenantOnce(TENANT_ID);

        assertThat(result).isEqualTo(TenantInitResult.FAILED);
        verify(valueOperations, never()).set(anyString(), anyString());
        verifyNoInteractions(bizSetSyncService, tenantSetSyncService, tenantHostSyncService, backGroundTaskDaemon,
            notifyChannelInitService);
    }

    @Test
    @DisplayName("系统租户跳过默认消息渠道初始化，前 5 个步骤正常执行")
    void initTenantOnceShouldSkipNotifyChannelForSystemTenant() {
        String systemTenantId = TenantIdConstants.SYSTEM_TENANT_ID;
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + systemTenantId)).thenReturn(null);

        TenantInitResult result = tenantInitService.initTenantOnce(systemTenantId);

        assertThat(result).isEqualTo(TenantInitResult.SUCCESS);
        verify(bizSyncService).syncBizFromCMDB(systemTenantId);
        verify(bizSetSyncService).syncBizSetFromCMDB(systemTenantId);
        verify(tenantSetSyncService).syncTenantSetFromCMDB();
        verify(tenantHostSyncService).syncAllBizHostsAtOnce(systemTenantId);
        verify(backGroundTaskDaemon).checkAndResumeTaskForTenant(systemTenantId);
        verifyNoInteractions(notifyChannelInitService);
    }

    @Test
    @DisplayName("OP 接口入口无条件执行 6 个步骤且不写完成标记")
    void initTenantShouldExecuteUnconditionallyWithoutWritingMark() throws Exception {
        when(valueOperations.get(INIT_DONE_KEY_PREFIX + TENANT_ID)).thenReturn(EXISTED_MARK);

        Boolean result = tenantInitService.initTenant(TENANT_ID);

        assertThat(result).isTrue();
        verify(bizSyncService).syncBizFromCMDB(TENANT_ID);
        verify(notifyChannelInitService).tryToInitDefaultNotifyChannelsWithSingleTenant(TENANT_ID);
        verify(valueOperations, never()).set(anyString(), anyString());
    }
}
