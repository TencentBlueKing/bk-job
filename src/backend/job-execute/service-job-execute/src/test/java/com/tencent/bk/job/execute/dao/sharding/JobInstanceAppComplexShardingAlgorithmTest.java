package com.tencent.bk.job.execute.dao.sharding;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.sharding.mysql.algorithm.IllegalShardKeyException;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobInstanceAppComplexShardingAlgorithmTest {

    private static JobInstanceAppComplexShardingAlgorithm<Long> shardingAlgorithm;

    private static List<String> availableDsList;
    private static List<String> availableTableList;


    @BeforeAll
    static void init() {
        AppScopeMappingService mockAppScopeMappingService = mock(AppScopeMappingService.class);
        when(mockAppScopeMappingService.getScopeByAppId(399L))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ, "501"));
        when(mockAppScopeMappingService.getScopeByAppId(9991031L))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, "9991031"));
        when(mockAppScopeMappingService.getScopeByAppId(10001L))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ, "10001"));
        when(mockAppScopeMappingService.getScopeByAppId(10002L))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, "10002"));
        ApplicationContext mockApplicationContext = mock(ApplicationContext.class);
        when(mockApplicationContext.getBean(AppScopeMappingService.class)).thenReturn(mockAppScopeMappingService);

        shardingAlgorithm = new JobInstanceAppComplexShardingAlgorithm<>();
        shardingAlgorithm.setApplicationContext(mockApplicationContext);
        Properties props = new Properties();
        props.put("dataNodes", "ds_group=ds_job_instance_search,db_node_count=2,tb_node_count=5;" +
            "ds_group=ds_job_instance_search_biz_a,db_node_count=2,tb_node_count=5;" +
            "ds_group=ds_job_instance_search_biz_set_b,db_node_count=2,tb_node_count=5");
        props.put("defaultDsGroup", "ds_job_instance_search");
        props.put("largeDataDsGroup", "biz:10001=ds_job_instance_search_biz_a;" +
            "biz_set:10002=ds_job_instance_search_biz_set_b");
        props.put("shardingColumnNameMapping", "app_id=app_id,task_instance_id=id");
        shardingAlgorithm.init(props);

        availableDsList = buildAvailableDsList(Arrays.asList("ds_job_instance_search",
            "ds_job_instance_search_biz_a", "ds_job_instance_search_biz_set_b"), 2);
        availableTableList = buildAvailableTableList("task_instance_app", 5);
    }

    private static List<String> buildAvailableDsList(List<String> dsGroupNames, int dbCount) {
        List<String> dsList = new ArrayList<>();
        for (String dsGroup : dsGroupNames) {
            for (int i = 0; i < dbCount; i++) {
                dsList.add(dsGroup + "_" + i);
            }
        }
        return dsList;
    }

    private static List<String> buildAvailableTableList(String tableName, int tbCount) {
        List<String> tableList = new ArrayList<>();
        for (int i = 0; i < tbCount; i++) {
            tableList.add(tableName + "_" + i);
        }

        return tableList;
    }

    @Test
    void doSharding() {
        Map<String, Collection<Long>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(399L));
        columnNameAndShardingValuesMap.put("id", Collections.singletonList(1000001L));
        ComplexKeysShardingValue<Long> shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_1");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_4");

        columnNameAndShardingValuesMap.clear();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(9991031L));
        columnNameAndShardingValuesMap.put("id", Collections.singletonList(1000002L));
        shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_0");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_1");
    }

    @Test
    void shardingLargeDataResourceScope() {
        // 测试根据 id 字段路由
        Map<String, Collection<Long>> columnNameAndShardingValuesMap = new HashMap<>();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(10001L));
        columnNameAndShardingValuesMap.put("id", Collections.singletonList(1000001L));
        ComplexKeysShardingValue<Long> shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_biz_a_0");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_1");

        columnNameAndShardingValuesMap.clear();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(10001L));
        columnNameAndShardingValuesMap.put("id", Collections.singletonList(1000046L));
        shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_biz_a_1");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_1");

        // 测试全分片路由
        columnNameAndShardingValuesMap.clear();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(10001L));
        shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_biz_a_0", "ds_job_instance_search_biz_a_1");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_0", "task_instance_app_1", "task_instance_app_2", "task_instance_app_3",
            "task_instance_app_4");

        columnNameAndShardingValuesMap.clear();
        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(10002L));
        shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThat(shardingAlgorithm.doSharding(availableDsList, shardingValue)).containsOnly(
            "ds_job_instance_search_biz_set_b_0", "ds_job_instance_search_biz_set_b_1");
        assertThat(shardingAlgorithm.doSharding(availableTableList, shardingValue)).containsOnly(
            "task_instance_app_0", "task_instance_app_1", "task_instance_app_2", "task_instance_app_3",
            "task_instance_app_4");
    }


    @Test
    void testInvalidShardingKey() {
        Map<String, Collection<Long>> columnNameAndShardingValuesMap = new HashMap<>();
        ComplexKeysShardingValue<Long> shardingValue = new ComplexKeysShardingValue<>(
            "task_instance_app",
            columnNameAndShardingValuesMap,
            null
        );
        assertThatExceptionOfType(IllegalShardKeyException.class).isThrownBy(() ->
            shardingAlgorithm.doSharding(availableDsList, shardingValue));

        List<Long> appIdList = new ArrayList<>();
        appIdList.add(10001L);
        appIdList.add(10002L);
        columnNameAndShardingValuesMap.put("app_id", appIdList);
        assertThatExceptionOfType(IllegalShardKeyException.class).isThrownBy(() ->
            shardingAlgorithm.doSharding(availableDsList, shardingValue));

        columnNameAndShardingValuesMap.clear();
        List<Long> taskInstanceIdList = new ArrayList<>();
        taskInstanceIdList.add(1010001L);
        taskInstanceIdList.add(1010002L);
        columnNameAndShardingValuesMap.put("id", taskInstanceIdList);
        assertThatExceptionOfType(IllegalShardKeyException.class).isThrownBy(() ->
            shardingAlgorithm.doSharding(availableDsList, shardingValue));
    }

//    @Test
//    @Timeout(1L)
//    void performanceTest() {
//        Map<String, Collection<Long>> columnNameAndShardingValuesMap = new HashMap<>();
//        columnNameAndShardingValuesMap.put("app_id", Collections.singletonList(10002L));
//        ComplexKeysShardingValue<Long> shardingValue;
//        for (long taskInstanceId = 1; taskInstanceId < 100000L; taskInstanceId++) {
//            columnNameAndShardingValuesMap.put("id", Collections.singletonList(taskInstanceId));
//            shardingValue = new ComplexKeysShardingValue<>(
//                "task_instance_app",
//                columnNameAndShardingValuesMap,
//                null
//            );
//            shardingAlgorithm.doSharding(availableDsList, shardingValue);
//            shardingAlgorithm.doSharding(availableTableList, shardingValue);
//        }
//    }


}
