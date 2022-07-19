/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.manage.metrics.MetricsConstants;
import com.tencent.bk.job.manage.service.PaaSService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WatchableSendMsgService {

    private final PaaSService paaSService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public WatchableSendMsgService(PaaSService paaSService,
                                   MeterRegistry meterRegistry) {
        this.paaSService = paaSService;
        this.meterRegistry = meterRegistry;
    }

    @EsbApiTimed
    public boolean sendMsg(
        Long appId,
        long createTimeMillis,
        String msgType,
        String sender,
        Set<String> receivers,
        String title,
        String content
    ) throws Exception {
        String sendStatus = MetricsConstants.TAG_VALUE_SEND_STATUS_FAILED;
        try {
            boolean result = paaSService.sendMsg(msgType, sender, receivers, title, content);
            if (result) {
                sendStatus = MetricsConstants.TAG_VALUE_SEND_STATUS_SUCCESS;
            }
            return result;
        } finally {
            long delayMillis = System.currentTimeMillis() - createTimeMillis;
            Tags tags = Tags.of(MetricsConstants.TAG_KEY_MSG_TYPE, msgType);
            tags = tags.and(MetricsConstants.TAG_KEY_SEND_STATUS, sendStatus);
            tags = tags.and(MetricsConstants.TAG_KEY_APP_ID, buildAppIdStr(appId));
            recordSendMsgDelay(delayMillis, tags);
        }
    }

    private String buildAppIdStr(Long appId) {
        return appId == null ? MetricsConstants.TAG_VALUE_APP_ID_NULL : appId.toString();
    }

    private void recordSendMsgDelay(long delayMillis, Iterable<Tag> tags) {
        Timer.builder(MetricsConstants.NAME_NOTIFY_DELAY)
            .description("Notify Message Send Delay")
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(100))
            .maximumExpectedValue(Duration.ofSeconds(60L))
            .register(meterRegistry)
            .record(delayMillis, TimeUnit.MILLISECONDS);
    }
}
