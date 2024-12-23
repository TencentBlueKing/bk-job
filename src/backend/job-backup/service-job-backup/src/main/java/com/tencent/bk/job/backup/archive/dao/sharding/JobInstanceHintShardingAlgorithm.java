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

package com.tencent.bk.job.backup.archive.dao.sharding;

import com.tencent.bk.job.common.sharding.mysql.algorithm.IllegalShardKeyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.hint.HintShardingValue;

import java.util.Collection;
import java.util.Collections;

/**
 * 作业实例强制路由分片算法
 */
@Slf4j
public class JobInstanceHintShardingAlgorithm implements HintShardingAlgorithm<String> {

    @Override
    public Collection<String> doSharding(Collection<String> availableTargets,
                                         HintShardingValue<String> hintShardingValue) {
        Collection<String> hintValues = hintShardingValue.getValues();
        if (CollectionUtils.isEmpty(hintValues)) {
            log.error("Sharding value are empty");
            throw new IllegalShardKeyException("Sharding value are empty");
        }
        if (hintValues.size() > 1) {
            log.error("Not support multi hint sharding value : {}", hintValues);
            throw new IllegalShardKeyException("Not support multi hint sharding value");

        }
        // 强制指定目标
        String hintTarget = hintValues.stream().findFirst().get();
        if (!availableTargets.contains(hintTarget)) {
            log.error("Target is not available, hintTarget: {}, availableTargets: {}", hintTarget, availableTargets);
            throw new IllegalShardKeyException("Target is not available");
        }
        return Collections.singletonList(hintTarget);
    }

    @Override
    public String getType() {
        return "JOB_INSTANCE_HINT";
    }
}
