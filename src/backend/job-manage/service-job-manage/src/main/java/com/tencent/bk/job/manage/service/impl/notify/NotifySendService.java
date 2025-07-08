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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.paas.user.UserLocalCache;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.impl.WatchableSendMsgService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class NotifySendService {

    //发通知专用线程池
    private final ThreadPoolExecutor notifySendExecutor;
    private final WatchableSendMsgService watchableSendMsgService;
    private final UserLocalCache userLocalCache;
    private final TenantService tenantService;

    @Autowired
    public NotifySendService(WatchableSendMsgService watchableSendMsgService,
                             UserLocalCache userLocalCache,
                             @Qualifier("notifySendExecutor") ThreadPoolExecutor notifySendExecutor,
                             MeterRegistry meterRegistry,
                             TenantService tenantService) {
        this.watchableSendMsgService = watchableSendMsgService;
        this.userLocalCache = userLocalCache;
        this.notifySendExecutor = notifySendExecutor;
        this.tenantService = tenantService;
        measureNotifySendExecutor(meterRegistry);
    }

    private void measureNotifySendExecutor(MeterRegistry meterRegistry) {
        meterRegistry.gauge(
            MetricsConstants.NAME_NOTIFY_POOL_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE,
                MetricsConstants.TAG_VALUE_MODULE_NOTIFY)),
            notifySendExecutor,
            ThreadPoolExecutor::getPoolSize
        );
        meterRegistry.gauge(
            MetricsConstants.NAME_NOTIFY_QUEUE_SIZE,
            Collections.singletonList(Tag.of(MetricsConstants.TAG_KEY_MODULE,
                MetricsConstants.TAG_VALUE_MODULE_NOTIFY)),
            notifySendExecutor,
            threadPoolExecutor -> threadPoolExecutor.getQueue().size()
        );
    }

    private SendNotifyTask buildSendTask(Long appId,
                                         Set<String> receivers,
                                         String channel,
                                         String title,
                                         String content) {
        SendNotifyTask task = SendNotifyTask.builder()
            .appId(appId)
            .createTimeMillis(System.currentTimeMillis())
            .msgType(channel)
            .receivers(receivers)
            .title(title)
            .content(content)
            .tenantId(tenantService.getTenantIdByAppId(appId))
            .build();
        task.bindService(watchableSendMsgService, userLocalCache);
        return task;
    }

    public void asyncSendUserChannelNotify(Long appId, Set<String> receivers, String channel, String title,
                                           String content) {
        if (CollectionUtils.isEmpty(receivers)) {
            log.warn("receivers is empty of channel {}, do not send notification", channel);
            return;
        }
        log.debug(
            "Begin to send {} notify to {}, title:{}, content:{}",
            channel,
            String.join(",", receivers),
            title,
            content
        );
        notifySendExecutor.submit(buildSendTask(appId, receivers, channel, title, content));
    }

    public void sendUserChannelNotify(Set<String> receivers,
                                      String channel,
                                      String title,
                                      String content) {
        if (CollectionUtils.isEmpty(receivers)) {
            log.warn("receivers is empty of channel {}, do not send notification", channel);
            return;
        }
        watchableSendMsgService.sendMsgWithCurrentTenant(
            JobContextUtil.getTenantId(),
            System.currentTimeMillis(),
            channel,
            null,
            receivers,
            title,
            content
        );
    }

    public void asyncSendNotifyMessages(Long appId, Map<String, Set<String>> channelUsersMap, String title,
                                        String content) {
        channelUsersMap.forEach((channel, userSet) ->
            asyncSendUserChannelNotify(appId, userSet, channel, title, content)
        );
    }
}
