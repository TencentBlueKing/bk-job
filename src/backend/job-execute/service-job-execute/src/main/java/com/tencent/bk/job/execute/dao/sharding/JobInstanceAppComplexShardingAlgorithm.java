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

package com.tencent.bk.job.execute.dao.sharding;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 按业务分片算法
 */
@Slf4j
public class JobInstanceAppComplexShardingAlgorithm<T extends Comparable<T>>
    implements ComplexKeysShardingAlgorithm<T> {

    private String SHARDING_COLUMN_NAME_APP_ID = "app_id";
    private String SHARDING_COLUMN_NAME_TASK_INSTANCE_ID = "task_instance_id";

    private int tbCount = 2;

    private int dbCount = 2;

    private int dbNodeTotalSlot;

    private final Map<Long, String> taskInstanceIdShardingDsCluster = new HashMap<>();

    private static final String DEFAULT_DB_PREFIX = "ds_job_instance_app_default";

    public JobInstanceAppComplexShardingAlgorithm() {
        taskInstanceIdShardingDsCluster.put(9999431L, "ds_job_instance_app_gcs");
    }


    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<T> shardingValue) {
        Long appId = getAppIdShardingValue(shardingValue);
        Long taskInstanceId = getTaskInstanceIdShardingValue(shardingValue);
        String logicTableName = shardingValue.getLogicTableName();

        boolean isShardTable = availableTargetNames.stream()
                .anyMatch(targetName -> targetName.startsWith(shardingValue.getLogicTableName()));

        boolean shardByAppId = !taskInstanceIdShardingDsCluster.containsKey(appId);

        String dbNodePrefix = shardByAppId ? DEFAULT_DB_PREFIX : taskInstanceIdShardingDsCluster.get(appId);
        log.info("isShardTable: {}, shardByAppId: {}, dbNodePrefix: {}", isShardTable, shardByAppId, dbNodePrefix);

        if (shardByAppId) {
            if (isShardTable) {
                return evalTableTargetsByAppId(appId, availableTargetNames, logicTableName);
            } else {
                return evalDbTargetsByAppId(appId, availableTargetNames, dbNodePrefix);
            }
        } else {
            if (isShardTable) {
                if (taskInstanceId == null) {
                    // 全分片路由
                    return availableTargetNames.stream().filter(
                            targetName -> targetName.startsWith(logicTableName)).collect(Collectors.toList());
                } else {
                    // 精准分片路由
                    return evalTableTargetsByTaskInstanceId(taskInstanceId, availableTargetNames,
                            shardingValue.getLogicTableName());
                }
            } else {
                if (taskInstanceId == null) {
                    // 全分片路由
                    return availableTargetNames.stream().filter(
                            targetName -> targetName.startsWith(dbNodePrefix)).collect(Collectors.toList());
                } else {
                    return evalDbTargetsByTaskInstanceId(taskInstanceId, availableTargetNames, dbNodePrefix);
                }
            }
        }
    }

    @Getter
    private enum ShardKey {
        APP_ID("app_id"),
        TASK_INSTANCE_ID("task_instance_id");

        ShardKey(String name) {
            this.name = name;
        }

        private final String name;
    }

    private Set<String> evalDbTargetsByAppId(long appId,
                                             Collection<String> availableTargetNames,
                                             String targetNamePrefix) {
        long suffix = evalDbSuffix(appId);
        return evalTargets(availableTargetNames, targetNamePrefix, suffix);
    }

    private Set<String> evalTableTargetsByAppId(long appId,
                                                Collection<String> availableTargetNames,
                                                String targetNamePrefix) {
        long suffix = evalTableSuffix(appId);
        return evalTargets(availableTargetNames, targetNamePrefix, suffix);
    }

    private long evalDbSuffix(long shardingValue) {
        long slot = shardingValue % dbNodeTotalSlot;
        return slot / tbCount;
    }

    private long evalTableSuffix(long shardingValue) {
        long slot = shardingValue % dbNodeTotalSlot;
        return slot % tbCount;
    }

    private Set<String> evalTargets(Collection<String> availableTargetNames,
                                    String targetNamePrefix,
                                    long suffix) {
        Set<String> matchTargetNames = new HashSet<>();
        String matchTargetName = targetNamePrefix + "_" + suffix;
        if (availableTargetNames.contains(matchTargetName)) {
            matchTargetNames.add(matchTargetName);
        }
        return matchTargetNames;
    }

    private Set<String> evalTableTargetsByTaskInstanceId(long taskInstanceId,
                                                         Collection<String> availableTargetNames,
                                                         String targetNamePrefix) {
        long suffix = evalTableSuffix(taskInstanceId);
        return evalTargets(availableTargetNames, targetNamePrefix, suffix);
    }

    private Set<String> evalDbTargetsByTaskInstanceId(long taskInstanceId,
                                                      Collection<String> availableTargetNames,
                                                      String targetNamePrefix) {
        long suffix = evalDbSuffix(taskInstanceId);
        return evalTargets(availableTargetNames, targetNamePrefix, suffix);
    }

    private Long getAppIdShardingValue(ComplexKeysShardingValue<T> shardingValue) {
        List<Long> values = castToLongList(
                shardingValue.getColumnNameAndShardingValuesMap().get(SHARDING_COLUMN_NAME_APP_ID));
        if (CollectionUtils.isEmpty(values)) {
            log.error("Shard key [" + SHARDING_COLUMN_NAME_APP_ID + "] required");
            throw new IllegalShardKeyException("Shard key [" + SHARDING_COLUMN_NAME_APP_ID + "] required");
        }
        if (values.size() > 1) {
            log.error("Shard key [" + SHARDING_COLUMN_NAME_APP_ID + "] does not support multi value");
            throw new IllegalShardKeyException(
                    "Shard key [" + SHARDING_COLUMN_NAME_APP_ID + "] does not support multi value");
        }
        return values.get(0);
    }

    private Long getTaskInstanceIdShardingValue(ComplexKeysShardingValue<T> shardingValue) {
        List<Long> values = castToLongList(
                shardingValue.getColumnNameAndShardingValuesMap().get(SHARDING_COLUMN_NAME_TASK_INSTANCE_ID));
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        if (values.size() > 1) {
            log.error("Shard key [" + SHARDING_COLUMN_NAME_TASK_INSTANCE_ID + "] does not support multi value");
            throw new IllegalShardKeyException(
                    "Shard key [" + SHARDING_COLUMN_NAME_TASK_INSTANCE_ID + "] does not support multi value");
        }
        return values.get(0);
    }

    private List<Long> castToLongList(Collection<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        List<Long> result = new ArrayList<>(list.size());
        list.forEach(num -> {
            if (num instanceof Integer) {
                result.add(((Integer) num).longValue());
            } else if (num instanceof Long) {
                result.add((Long) num);
            } else {
                throw new IllegalArgumentException("Invalid sharding value: " + num);
            }
        });
        return result;
    }

    @Override
    public void init(Properties props) {
        ComplexKeysShardingAlgorithm.super.init(props);
        if (props.containsKey("shardColumnNameMapping")) {
            String[] columnNameMappings = props.getProperty("shardColumnNameMapping").split(",");
            for (String columnNameMapping : columnNameMappings) {
                String[] shardKeyAndColumnName = columnNameMapping.split("=");
                String shardKey = shardKeyAndColumnName[0].trim();
                String columnName = shardKeyAndColumnName[1].trim();
                if (shardKey.equals("app_id")) {
                    SHARDING_COLUMN_NAME_APP_ID = columnName;
                } else if (shardKey.equals("task_instance_id")) {
                    SHARDING_COLUMN_NAME_TASK_INSTANCE_ID = columnName;
                } else {
                    log.warn("Invalid shardColumnNameMapping, shardKey: {}", shardKey);
                }
            }
        }
//        if (props.containsKey("tbCount")) {
//            tbCount = Integer.parseInt(props.getProperty("tbCount").trim());
//        } else {
//            log.error("Prop [tbCount] is required");
//            throw new IllegalStateException("Init sharding algorithm error");
//        }
//
//        if (props.containsKey("dbCount")) {
//            dbCount = Integer.parseInt(props.getProperty("dbCount").trim());
//        } else {
//            log.error("Prop [dbCount] is required");
//            throw new IllegalStateException("Init sharding algorithm error");
//        }

        dbNodeTotalSlot = dbCount * tbCount;
    }

    @Override
    public String getType() {
        return "JOB_INSTANCE_APP";
    }
}
