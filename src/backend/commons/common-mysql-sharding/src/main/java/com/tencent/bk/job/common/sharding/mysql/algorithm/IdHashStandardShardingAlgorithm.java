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

import com.tencent.bk.job.common.sharding.mysql.DataSourceGroupShardingNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.infra.exception.core.external.sql.type.generic.UnsupportedSQLOperationException;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * 按 ID Hash 标准分片算法
 */
@Slf4j
public class IdHashStandardShardingAlgorithm extends ShardingAlgorithmBase implements StandardShardingAlgorithm<Long> {

    /**
     * 分库分表 - db 节点数量
     */
    private int dbNodeCount;

    /**
     * 分库分表 - 每个 db 上的表节点数量
     */
    private int tbNodeCount;

    /**
     * 所有数据节点数量
     */
    private int sumSlot;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> preciseShardingValue) {
        boolean isShardTable = availableTargetNames.stream()
            .anyMatch(targetName -> targetName.startsWith(preciseShardingValue.getLogicTableName()));

        if (isShardTable) {
            long slot = preciseShardingValue.getValue() % sumSlot;
            long tbIndex = slot % tbNodeCount;
            return evalTargets(availableTargetNames, tbIndex);
        } else {
            long slot = preciseShardingValue.getValue() % sumSlot;
            long dbIndex = slot / tbNodeCount;
            return evalTargets(availableTargetNames, dbIndex);
        }
    }

    private String evalTargets(Collection<String> availableTargetNames,
                               long index) {
        String suffix = "_" + index;
        String targetName = availableTargetNames.stream().filter(target -> target.endsWith(suffix))
            .findAny().orElse(null);
        if (targetName == null) {
            log.warn("Can not find target, availableTargetNames: {}, index: {}", availableTargetNames, index);
        }
        return targetName;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Long> rangeShardingValue) {
        throw new UnsupportedSQLOperationException("Range query is not allowed");
    }

    @Override
    public void init(Properties props) {
        List<DataSourceGroupShardingNode> dataSourceGroupShardingNodes = parseDataNodes(props);
        if (dataSourceGroupShardingNodes.size() > 1) {
            log.error("Multi data source group not support");
            throw new IllegalArgumentException("Invalid data nodes");
        }
        DataSourceGroupShardingNode dataSourceGroupShardingNode = dataSourceGroupShardingNodes.get(0);
        dbNodeCount = dataSourceGroupShardingNode.getDbNodeCount();
        tbNodeCount = dataSourceGroupShardingNode.getTbNodeCount();
        sumSlot = dbNodeCount * tbNodeCount;
        log.info("Init sharding algorithm, dbNodeSize: {}, tbNodeSize: {}", dbNodeCount, tbNodeCount);
    }


    @Override
    public String getType() {
        return "JOB_ID_HASH_STANDARD";
    }
}
