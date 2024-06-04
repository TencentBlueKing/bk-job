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

package com.tencent.bk.job.common.sharding.mysql;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;

public class ConsistenceHashAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * 范围查找时需要用到改分片算法，这里暂不完善了
     * @param collection
     * @param rangeShardingValue
     * @return
     */
    @Override
    public Collection<String> doSharding(Collection collection, RangeShardingValue rangeShardingValue) {
        System.out.println(collection);
        System.out.println(rangeShardingValue);
        return collection;
    }

    /**
     * @param collection collection 配置文件中解析到的所有分片节点
     * @param preciseShardingValue 解析到的sql值
     * @return
     */
    @Override
    public String doSharding(Collection collection, PreciseShardingValue preciseShardingValue) {
//        System.out.println(collection);
//        InitTableNodesToHashLoop initTableNodesToHashLoop = SpringUtils.getBean(InitTableNodesToHashLoop.class);
//        if (CollectionUtils.isEmpty(collection)) {
//            return preciseShardingValue.getLogicTableName();
//        }
//
//        //这里主要为了兼容当联表查询时，如果两个表非关联表则
//        //当对副表分表时shardingValue这里传递进来的依然是主表的名称，
//        //但availableTargetNames中确是副表名称，所有这里要从availableTargetNames中匹配真实表
//        ArrayList<String> availableTargetNameList = new ArrayList<>(collection);
//        String logicTableName = availableTargetNameList.get(0).replaceAll("[^(a-zA-Z_)]", "");
//        SortedMap<Long, String> tableHashNode =
//            initTableNodesToHashLoop .getTableVirtualNodes().get(logicTableName);
//
//        ConsistenceHashUtil consistentHashAlgorithm = new ConsistenceHashUtil(tableHashNode,
//            collection);
//
//        return consistentHashAlgorithm.getTableNode(String.valueOf(preciseShardingValue.getValue()
        return null;
    }
}
