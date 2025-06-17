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

package com.tencent.bk.job.execute.metrics.impl;

import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.metrics.ExecuteObjectSampler;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceExecuteObjects;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 记录任务执行对象相关指标
 */
@Slf4j
@Service
public class ExecuteObjectSamplerImpl implements ExecuteObjectSampler {

    private final MeterRegistry meterRegistry;

    @Autowired
    public ExecuteObjectSamplerImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void tryToRecordExecuteObjectMetrics(TaskInstanceDTO taskInstance,
                                                TaskInstanceExecuteObjects executeObjects) {
        try {
            recordExecuteObjectMetrics(taskInstance, executeObjects);
        } catch (Exception e) {
            log.warn("tryToRecordExecuteObjectMetrics error", e);
        }
    }

    /**
     * 记录任务执行对象指标
     *
     * @param taskInstance   任务实例
     * @param executeObjects 任务执行对象
     */
    public void recordExecuteObjectMetrics(TaskInstanceDTO taskInstance,
                                           TaskInstanceExecuteObjects executeObjects) {
        int executeObjectNum = executeObjects.getExecuteObjectNum();
        Iterable<Tag> tags = buildTags(taskInstance, executeObjects);
        if (log.isDebugEnabled()) {
            log.debug(
                "tryToRecordExecuteObjectMetrics: appId={}, cronTaskId={}, appCode={}, executeObjectNum={}",
                taskInstance.getAppId(),
                taskInstance.getCronTaskId(),
                taskInstance.getAppCode(),
                executeObjectNum
            );
        }
        DistributionSummary.builder(ExecuteMetricsConstants.NAME_JOB_TASK_EXECUTE_OBJECT_NUM)
            .tags(tags)
            .register(meterRegistry)
            .record(executeObjectNum);
    }

    /**
     * 构造资源范围标签值
     *
     * @param resourceScope 资源范围
     * @return 资源范围标签值
     */
    private String buildResourceScopeTagValue(ResourceScope resourceScope) {
        if (resourceScope == null) {
            return "None";
        }
        return resourceScope.getType() + ":" + resourceScope.getId();
    }

    /**
     * 构造指标的标签
     *
     * @param taskInstance   任务实例
     * @param executeObjects 任务执行对象
     * @return 指标标签列表
     */
    private Iterable<Tag> buildTags(TaskInstanceDTO taskInstance,
                                    TaskInstanceExecuteObjects executeObjects) {
        ResourceScope resourceScope = GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId());
        Tag resourceScopeTag = Tag.of(CommonMetricTags.KEY_RESOURCE_SCOPE, buildResourceScopeTagValue(resourceScope));
        Tag cronTaskIdTag = Tag.of(
            ExecuteMetricsConstants.TAG_KEY_CRON_TASK_ID,
            String.valueOf(taskInstance.getCronTaskId())
        );
        Tag appCodeTag = Tag.of(ExecuteMetricsConstants.TAG_KEY_APP_CODE, String.valueOf(taskInstance.getAppCode()));
        Tag compositionTag = buildExecuteObjectCompositionTag(executeObjects);
        return Tags.of(resourceScopeTag, cronTaskIdTag, appCodeTag, compositionTag);
    }

    /**
     * 构造执行对象组成标签
     *
     * @param executeObjects 任务执行对象
     * @return 执行对象组成标签
     */
    private Tag buildExecuteObjectCompositionTag(TaskInstanceExecuteObjects executeObjects) {
        String executeObjectCompositionTagValue;
        boolean hasHost = executeObjects.isContainsAnyHost();
        boolean hasContainer = executeObjects.isContainsAnyContainer();
        if (hasHost && hasContainer) {
            executeObjectCompositionTagValue = ExecuteMetricsConstants.TAG_VALUE_EXECUTE_OBJECT_COMPOSITION_MIXED;
        } else if (hasHost) {
            executeObjectCompositionTagValue = ExecuteMetricsConstants.TAG_VALUE_EXECUTE_OBJECT_COMPOSITION_HOST;
        } else if (hasContainer) {
            executeObjectCompositionTagValue = ExecuteMetricsConstants.TAG_VALUE_EXECUTE_OBJECT_COMPOSITION_CONTAINER;
        } else {
            executeObjectCompositionTagValue = ExecuteMetricsConstants.TAG_VALUE_EXECUTE_OBJECT_COMPOSITION_NONE;
        }
        return Tag.of(
            ExecuteMetricsConstants.TAG_KEY_EXECUTE_OBJECT_COMPOSITION,
            executeObjectCompositionTagValue
        );
    }
}
