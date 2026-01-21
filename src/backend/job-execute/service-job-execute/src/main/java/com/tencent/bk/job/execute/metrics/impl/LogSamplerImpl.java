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

package com.tencent.bk.job.execute.metrics.impl;

import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.metrics.LogSampler;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 记录任务日志大小指标
 */
@Slf4j
@Service
public class LogSamplerImpl implements LogSampler {

    private final MeterRegistry meterRegistry;

    @Autowired
    public LogSamplerImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void tryToRecordLogSizeMetrics(ServiceBatchSaveLogRequest request) {
        try {
            Iterable<Tag> tags = buildTags(request);
            long logBytes = calcLogsTotalBytes(request.getLogs());
            if (log.isDebugEnabled()) {
                log.debug(
                    "tryToRecordLogSizeMetrics: appId={}, cronTaskId={}, appCode={}, logBytes={}",
                    request.getAppId(),
                    request.getCronTaskId(),
                    request.getAppCode(),
                    logBytes
                );
            }
            Counter.builder(ExecuteMetricsConstants.NAME_JOB_LOG_BYTES)
                .tags(tags)
                .register(meterRegistry)
                .increment(logBytes);
        } catch (Exception e) {
            log.warn("tryToRecordLogSizeMetrics error", e);
        }
    }

    /**
     * 构造指标的标签
     *
     * @param request 保存日志请求
     * @return 指标标签列表
     */
    private Iterable<Tag> buildTags(ServiceBatchSaveLogRequest request) {
        Long appId = request.getAppId();
        Long cronTaskId = request.getCronTaskId();
        String appCode = request.getAppCode();
        return Tags.of(
            Tag.of(ExecuteMetricsConstants.TAG_KEY_APP_ID, String.valueOf(appId)),
            Tag.of(ExecuteMetricsConstants.TAG_KEY_CRON_TASK_ID, String.valueOf(cronTaskId)),
            Tag.of(ExecuteMetricsConstants.TAG_KEY_APP_CODE, String.valueOf(appCode))
        );
    }

    /**
     * 计算批量日志中所有日志的字节大小总和
     *
     * @param logs 日志列表
     * @return 日志总字节大小
     */
    private long calcLogsTotalBytes(List<ServiceExecuteObjectLogDTO> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return 0;
        }
        return logs.stream()
            .mapToLong(this::calcLogBytes)
            .sum();
    }

    /**
     * 计算单个日志的字节数
     *
     * @param log 日志内容
     * @return 单个日志的字节数
     */
    private long calcLogBytes(ServiceExecuteObjectLogDTO log) {
        if (log == null) {
            return 0L;
        }
        ServiceExecuteObjectScriptLogDTO scriptLog = log.getScriptLog();
        // 脚本类型的日志，直接获取contentSizeBytes
        if (scriptLog != null) {
            return scriptLog.getContentSizeBytes();
        }
        // 文件类型的日志，计算content字段的字节数并汇总
        List<ServiceFileTaskLogDTO> fileTaskLogs = log.getFileTaskLogs();
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return 0L;
        }
        long totalBytes = 0L;
        for (ServiceFileTaskLogDTO fileTaskLog : fileTaskLogs) {
            String content = fileTaskLog.getContent();
            if (content != null) {
                totalBytes += content.getBytes(StandardCharsets.UTF_8).length;
            }
        }
        return totalBytes;
    }
}
