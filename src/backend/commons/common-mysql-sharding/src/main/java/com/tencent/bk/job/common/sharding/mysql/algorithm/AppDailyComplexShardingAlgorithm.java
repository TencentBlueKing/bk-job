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

import com.google.common.collect.Range;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.infra.exception.core.external.sql.type.generic.UnsupportedSQLOperationException;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务按天分片算法
 */
@Slf4j
public class AppDailyComplexShardingAlgorithm implements ComplexKeysShardingAlgorithm<Long> {

    private String SHARDING_COLUMN_NAME_APP_ID = "app_id";
    private String SHARDING_COLUMN_NAME_TIME = "create_time";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private int tableCount;

    private int dbCount;

    private int dbNodeTotalSlot;

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<Long> shardingValue) {
        Set<String> matchTargetNames = new HashSet<>();

        Collection<Long> appIds = shardingValue.getColumnNameAndShardingValuesMap().get(SHARDING_COLUMN_NAME_APP_ID);
        if (CollectionUtils.isEmpty(appIds)) {
            log.error("Shard key [appId] required");
            throw new UnsupportedSQLOperationException("Shard key [appId] required");
        }
        List<Long> dates = null;
        if (shardingValue.getColumnNameAndShardingValuesMap().containsKey(SHARDING_COLUMN_NAME_TIME)) {
            Collection<Long> times = shardingValue.getColumnNameAndShardingValuesMap().get(SHARDING_COLUMN_NAME_TIME);
            dates = times.stream().map(this::computeDate).distinct().collect(Collectors.toList());
        } else if (shardingValue.getColumnNameAndRangeValuesMap().containsKey(SHARDING_COLUMN_NAME_TIME)) {
            Range<Long> timeRange = shardingValue.getColumnNameAndRangeValuesMap().get(SHARDING_COLUMN_NAME_TIME);
            dates = generateDatesBetweenTimestamps(timeRange.lowerEndpoint(), timeRange.upperEndpoint());
        }
        if (CollectionUtils.isEmpty(dates)) {
            log.error("Shard key [time] required");
            throw new UnsupportedSQLOperationException("Shard key [time] required");
        }

        boolean isShardTable = availableTargetNames.stream()
            .anyMatch(targetName -> targetName.startsWith(shardingValue.getLogicTableName()));
        for (Long appId : appIds) {
            for (Long date : dates) {
                long shardValue = appId + date;
                long slot = shardValue % dbNodeTotalSlot;
                long suffix = isShardTable ? slot % tableCount : slot / tableCount;
                for (String targetName : availableTargetNames) {
                    if (targetName.endsWith("_" + suffix)) {
                        matchTargetNames.add(targetName);
                    }
                }
            }
        }
        return matchTargetNames;
    }

    public List<Long> generateDatesBetweenTimestamps(long startTimestamp, long endTimestamp) {
        List<Long> dateList = new ArrayList<>();

        // 将 Unix 时间戳（ms) 转换为 LocalDate
        LocalDate startDate = Instant.ofEpochMilli(startTimestamp).atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = Instant.ofEpochMilli(endTimestamp).atZone(ZoneId.of("UTC")).toLocalDate();

        // 遍历日期并添加到列表
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateList.add(Long.parseLong(date.format(dateTimeFormatter)));
        }

        return dateList;
    }

    private Long computeDate(long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC")).toLocalDate();
        return Long.parseLong(date.format(dateTimeFormatter));
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
                } else if (shardKey.equals("create_time")) {
                    SHARDING_COLUMN_NAME_TIME = columnName;
                } else {
                    log.warn("Invalid shardColumnNameMapping, shardKey: {}", shardKey);
                }
            }
        }
        if (props.containsKey("tableCount")) {
            tableCount = Integer.parseInt(props.getProperty("tableCount").trim());
        } else {
            log.error("Prop [tableCount] is required");
            throw new IllegalStateException("Init sharding algorithm error");
        }

        if (props.containsKey("dbCount")) {
            dbCount = Integer.parseInt(props.getProperty("dbCount").trim());
        } else {
            log.error("Prop [dbCount] is required");
            throw new IllegalStateException("Init sharding algorithm error");
        }

        dbNodeTotalSlot = dbCount * tableCount;
    }

    @Override
    public String getType() {
        return "APP_DAILY";
    }
}
