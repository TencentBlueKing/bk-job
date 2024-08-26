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

package com.tencent.bk.job.common.sharding.mysql.algorithm;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 根据资源空间手动分片算法
 */
@Slf4j
public class ResourceScopeManualShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private final AppScopeMappingService appScopeMappingService;

    private final Map<Long, Integer> appIdAndTargetIndex = new HashMap<>();

    public ResourceScopeManualShardingAlgorithm() {
        this.appScopeMappingService = ApplicationContextRegister.getBean(AppScopeMappingService.class);
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        Long appId = shardingValue.getValue();
        Integer targetIndex = appIdAndTargetIndex.get(appId);
        if (targetIndex == null) {
            targetIndex = (int) (appId % availableTargetNames.size());
        }
        String dataNodePrefix = shardingValue.getDataNodeInfo().getPrefix();
        String targetName = dataNodePrefix + targetIndex;
        if (!availableTargetNames.contains(targetName)) {
            log.warn("Target sharding data node not found");
            throw new RuntimeException("Target sharding data node not found");
        }
        if (log.isDebugEnabled()) {
            log.debug("Sharding target -> {}", targetName);
        }
        return targetName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Long> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        StandardShardingAlgorithm.super.init(props);
        if (props.containsKey("manual")) {
            String[] resourceScopeShardingExpressions = props.getProperty("manual").split(",");
            for (String resourceScopeShardingExpression : resourceScopeShardingExpressions) {
                String[] resourceScopeAndTargetIndex = resourceScopeShardingExpression.split("=");
                String resourceScope = resourceScopeAndTargetIndex[0].trim();
                String[] resourceScopeTypeAndId = resourceScope.split(":");
                Long appId = appScopeMappingService.getAppIdByScope(
                    new ResourceScope(resourceScopeTypeAndId[0], resourceScopeTypeAndId[1]));
                if (appId == null) {
                    log.info("Can not find resource scope : {}, ignore", resourceScope);
                    continue;
                }
                appIdAndTargetIndex.put(appId, Integer.parseInt(resourceScopeAndTargetIndex[1].trim()));
            }
            log.info("Init resource scope and target data node index, appIdAndTargetIndex: {}", appIdAndTargetIndex);
        }
    }

    @Override
    public String getType() {
        return "RESOURCE_SCOPE";
    }
}
