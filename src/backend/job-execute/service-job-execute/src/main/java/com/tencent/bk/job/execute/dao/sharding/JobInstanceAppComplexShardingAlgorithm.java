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

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.sharding.mysql.DataSourceGroupShardingNode;
import com.tencent.bk.job.common.sharding.mysql.algorithm.IllegalShardKeyException;
import com.tencent.bk.job.common.sharding.mysql.algorithm.ShardingAlgorithmBase;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按业务分片算法
 */
@Slf4j
public class JobInstanceAppComplexShardingAlgorithm<T extends Comparable<T>> extends ShardingAlgorithmBase
    implements ComplexKeysShardingAlgorithm<T> {

    private String shardingKeyAppId = "app_id";

    private String shardingKeyTaskInstanceId = "task_instance_id";

    /**
     * 属性名称 - 默认数据源分组
     */
    private static final String PROP_DEFAULT_DS_GROUP = "defaultDsGroup";

    /**
     * 属性名称 - 包含大量数据的资源管理空间，特别指定的数据源分组
     */
    private static final String PROP_LARGE_DATA_DS_GROUP = "largeDataDsGroup";

    /**
     * 属性名称 - 分片键与列名称的映射关系
     */
    private static final String PROP_SHARDING_COLUMN_NAME_MAPPING = "shardingColumnNameMapping";

    private DataSourceGroupShardingNode defaultDsGroup;

    /**
     * 是否包含海量数据业务的数据源自定义配置
     */
    private volatile boolean isLargeDataResourceScopeDsGroupsConfigured;

    /**
     * 是否已经初始化海量数据业务的数据源自定义配置
     */
    private volatile boolean isLargeDataResourceScopeDsGroupsInitial;

    private final Map<String, DataSourceGroupShardingNode> largeDataResourceScopeDsGroups = new HashMap<>();

    private final Map<Long, DataSourceGroupShardingNode> largeDataAppDsGroups = new HashMap<>();


    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         ComplexKeysShardingValue<T> shardingValue) {
        // 初始化海量数据业务与数据源的映射关系
        lazyInitLargeDataResourceScopeDsGroupsIfNotAvailable();

        List<Long> appIdShardingValues = getAppIdShardingValues(shardingValue);
        List<Long> taskInstanceIdShardingValues = getTaskInstanceIdShardingValues(shardingValue);
        if (appIdShardingValues.size() > 1 ||
            (taskInstanceIdShardingValues != null && taskInstanceIdShardingValues.size() > 1)) {
            // 全分片查询
            return availableTargetNames;
        } else {
            // 精准分片查询
            return standardSharding(availableTargetNames, shardingValue, appIdShardingValues,
                taskInstanceIdShardingValues);
        }
    }


    private void lazyInitLargeDataResourceScopeDsGroupsIfNotAvailable() {
        if (!isLargeDataResourceScopeDsGroupsConfigured) {
            // 如果未配置，无需处理
            return;
        }
        if (isLargeDataResourceScopeDsGroupsInitial) {
            // 如果已初始化，无需处理
            return;
        }

        largeDataResourceScopeDsGroups.forEach((resourceScope, dataSourceGroupShardingNode) -> {
            Long appId = GlobalAppScopeMappingService.get().getAppIdByScope(new ResourceScope(resourceScope));
            if (appId == null) {
                log.error("AppId not found for resource scope : {}", resourceScope);
                throw new IllegalStateException("AppId not found for resource scope : " + resourceScope);
            }
            largeDataAppDsGroups.put(appId, dataSourceGroupShardingNode);
        });
        log.info("Init large data app ds groups, largeDataAppDsGroups: {}", largeDataAppDsGroups);
        isLargeDataResourceScopeDsGroupsInitial = true;
    }

    private Collection<String> standardSharding(Collection<String> availableTargetNames,
                                                ComplexKeysShardingValue<T> shardingValue,
                                                List<Long> appIdShardingValues,
                                                List<Long> taskInstanceIdShardingValues) {
        Long appId = appIdShardingValues.get(0);
        Long taskInstanceId = CollectionUtils.isEmpty(taskInstanceIdShardingValues) ? null :
            taskInstanceIdShardingValues.get(0);

        String logicTableName = shardingValue.getLogicTableName();
        // 判断是否使用分表算法（如果不是，那么使用分库算法)
        boolean isShardingTable = availableTargetNames.stream()
            .anyMatch(targetName -> targetName.startsWith(shardingValue.getLogicTableName()));
        // 是否海量数据业务；针对海量数据类型的业务，为了避免数据热点打到单个 db 实例的物理上限（比如存储、 mem 等），会使用 task_instance_id 进行二次分片
        boolean isLargeResourceScopeDsGroup = largeDataAppDsGroups.containsKey(appId);

        if (!isLargeResourceScopeDsGroup) {
            // 非海量数据业务，使用默认数据源分组，按照 app_id(业务 ID）进行分片
            if (isShardingTable) {
                return evalTableTargets(
                    appId,
                    defaultDsGroup,
                    availableTargetNames,
                    logicTableName
                );
            } else {
                return evalDbTargets(appId, defaultDsGroup, availableTargetNames);
            }
        } else {
            // 海量数据业务，使用专用的数据源分组，并使用 task_instance_id 进行二次分片
            DataSourceGroupShardingNode dsGroup = largeDataAppDsGroups.get(appId);
            if (isShardingTable) {
                if (taskInstanceId == null) {
                    // 全分片路由
                    return availableTargetNames;
                } else {
                    // 精准分片路由
                    return evalTableTargets(
                        taskInstanceId,
                        dsGroup,
                        availableTargetNames,
                        shardingValue.getLogicTableName()
                    );
                }
            } else {
                if (taskInstanceId == null) {
                    // 指定数据源分组范围内，全分片路由
                    return dsGroup.getAvailableTargetDbs()
                        .stream()
                        .filter(db -> {
                            boolean isMatch = availableTargetNames.contains(db);
                            if (!isMatch) {
                                log.warn("Can not find match db target, db: {}", db);
                            }
                            return isMatch;
                        })
                        .collect(Collectors.toList());
                } else {
                    return evalDbTargets(taskInstanceId, dsGroup, availableTargetNames);
                }
            }
        }
    }

    private Set<String> evalDbTargets(long shardingValue,
                                      DataSourceGroupShardingNode dataSourceGroupShardingNode,
                                      Collection<String> availableTargetNames) {
        long suffix = evalDbSuffix(shardingValue, dataSourceGroupShardingNode);
        return evalTargets(availableTargetNames, dataSourceGroupShardingNode.getDataSourceGroupName(), suffix);
    }

    private Set<String> evalTableTargets(long shardingValue,
                                         DataSourceGroupShardingNode dataSourceGroupShardingNode,
                                         Collection<String> availableTargetNames,
                                         String targetNamePrefix) {
        long suffix = evalTableSuffix(shardingValue, dataSourceGroupShardingNode);
        return evalTargets(availableTargetNames, targetNamePrefix, suffix);
    }

    private long evalDbSuffix(long shardingValue, DataSourceGroupShardingNode dataSourceGroupShardingNode) {
        long slot = shardingValue % dataSourceGroupShardingNode.getSumSlot();
        return slot / dataSourceGroupShardingNode.getTbNodeCount();
    }

    private long evalTableSuffix(long shardingValue, DataSourceGroupShardingNode dataSourceGroupShardingNode) {
        long slot = shardingValue % dataSourceGroupShardingNode.getSumSlot();
        return slot % dataSourceGroupShardingNode.getTbNodeCount();
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

    private List<Long> getAppIdShardingValues(ComplexKeysShardingValue<T> shardingValue) {
        List<Long> values = castToLongList(
            shardingValue.getColumnNameAndShardingValuesMap().get(shardingKeyAppId));
        if (CollectionUtils.isEmpty(values)) {
            log.error("Shard key [" + shardingKeyAppId + "] required");
            throw new IllegalShardKeyException("Shard key [" + shardingKeyAppId + "] required");
        }

        return values;
    }

    private List<Long> getTaskInstanceIdShardingValues(ComplexKeysShardingValue<T> shardingValue) {
        return castToLongList(
            shardingValue.getColumnNameAndShardingValuesMap().get(shardingKeyTaskInstanceId));
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
        initShardingColumnNameMapping(props);

        List<DataSourceGroupShardingNode> dsGroups = parseDataNodes(props);
        initDefaultDataSourceGroup(props, dsGroups);
        initLargeDataDataResourceScopeDsGroups(props, dsGroups);
    }

    private void initShardingColumnNameMapping(Properties props) {
        String shardingColumnNameMapping = getForString(props, PROP_SHARDING_COLUMN_NAME_MAPPING, false);

        if (StringUtils.isNotBlank(shardingColumnNameMapping)) {
            log.info("Init prop {} : {}", PROP_SHARDING_COLUMN_NAME_MAPPING, shardingColumnNameMapping);
            String[] columnNameMappings = shardingColumnNameMapping.split(",");
            for (String columnNameMapping : columnNameMappings) {
                String[] shardKeyAndColumnName = columnNameMapping.split("=");
                String shardKey = shardKeyAndColumnName[0].trim();
                String columnName = shardKeyAndColumnName[1].trim();
                if (shardKey.equals("app_id")) {
                    shardingKeyAppId = columnName;
                } else if (shardKey.equals("task_instance_id")) {
                    shardingKeyTaskInstanceId = columnName;
                } else {
                    log.warn("Invalid shardingColumnNameMapping, shardKey: {}", shardKey);
                }
            }
        }
    }

    private String getForString(Properties props, String key, boolean required) {
        String value = props.getProperty(key);
        if (StringUtils.isBlank(value) && required) {
            throw new IllegalArgumentException("Prop : " + key + " is Required");
        }
        return value;
    }

    private void initDefaultDataSourceGroup(Properties props,
                                            List<DataSourceGroupShardingNode> dataSourceGroupShardingNodes) {
        String defaultDsGroupName = getForString(props, PROP_DEFAULT_DS_GROUP, true);
        this.defaultDsGroup =
            dataSourceGroupShardingNodes.stream()
                .filter(node -> node.getDataSourceGroupName().equals(defaultDsGroupName))
                .findFirst()
                .orElse(null);
        if (defaultDsGroup == null) {
            throw new IllegalArgumentException("Default DataSourceGroup not found");
        }
    }

    private void initLargeDataDataResourceScopeDsGroups(
        Properties props,
        List<DataSourceGroupShardingNode> dataSourceGroupShardingNodes) {

        String expr = getForString(props, PROP_LARGE_DATA_DS_GROUP, false);
        if (StringUtils.isBlank(expr)) {
            return;
        }

        Map<String, DataSourceGroupShardingNode> allDataNodes =
            dataSourceGroupShardingNodes.stream().collect(Collectors.toMap(
                DataSourceGroupShardingNode::getDataSourceGroupName, node -> node
            ));
        String[] resourceScopeAndDsGroupMappings = expr.split(";");
        for (String resourceScopeAndDsGroupMappingExpr : resourceScopeAndDsGroupMappings) {
            parseResourceScopeAndDsGroup(allDataNodes, resourceScopeAndDsGroupMappingExpr);
        }
        isLargeDataResourceScopeDsGroupsConfigured = true;
        log.info("InitLargeDataDataResourceScopeDsGroups -> largeDataResourceScopeDsGroups : {}",
            largeDataResourceScopeDsGroups);
    }

    private void parseResourceScopeAndDsGroup(Map<String, DataSourceGroupShardingNode> allDataNodes,
                                              String resourceScopeAndDsGroupMappingExpr) {
        String[] resourceScopeAndDsGroup = resourceScopeAndDsGroupMappingExpr.split("=");
        String resourceScope = resourceScopeAndDsGroup[0].trim();
        boolean isResourceScopeValid = ResourceScope.checkValid(resourceScope);
        if (!isResourceScopeValid) {
            log.error("InitLargeDataDataResourceScopeDsGroups -> resourceScope is invalid, resourceScope: {}",
                resourceScope);
            throw new IllegalArgumentException("Resource scope " + resourceScope + " is invalid");
        }
        String dsGroup = resourceScopeAndDsGroup[1].trim();
        if (!allDataNodes.containsKey(dsGroup)) {
            log.error("InitLargeDataDataResourceScopeDsGroups -> dsGroup is not exist, dsGroup: {}", dsGroup);
            throw new IllegalArgumentException("DsGroup " + dsGroup + " is not exist");
        }
        largeDataResourceScopeDsGroups.put(resourceScope, allDataNodes.get(dsGroup));
    }

    @Override
    public String getType() {
        return "JOB_INSTANCE_APP";
    }
}
