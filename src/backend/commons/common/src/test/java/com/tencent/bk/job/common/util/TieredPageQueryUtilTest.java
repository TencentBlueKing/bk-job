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

package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TieredPageQueryUtil} 单元测试
 */
@DisplayName("TieredPageQueryUtil 测试")
public class TieredPageQueryUtilTest {

    /**
     * 模拟数据元素，包含一个自增 ID
     */
    private static class MockElement {
        private final long id;

        MockElement(long id) {
            this.id = id;
        }

        long getId() {
            return id;
        }
    }

    /**
     * 统一的分页请求包装（offset 或 keyset 二选一）
     */
    private static class UnifiedPageReq {
        private final Integer start;
        private final Long lastId;
        private final int limit;

        // offset 模式
        UnifiedPageReq(int start, int limit) {
            this.start = start;
            this.lastId = null;
            this.limit = limit;
        }

        // keyset 模式
        UnifiedPageReq(long lastId, int limit) {
            this.start = null;
            this.lastId = lastId;
            this.limit = limit;
        }

        Integer getStart() {
            return start;
        }

        Long getLastId() {
            return lastId;
        }

        int getLimit() {
            return limit;
        }

        boolean isOffsetMode() {
            return start != null;
        }
    }

    /**
     * 模拟分页查询结果
     */
    private record MockPageResult(List<MockElement> data) {
    }

    /**
     * 创建模拟数据源（ID 为 2, 4, 6, ...，即 i*2，避免下标与 ID 相同导致巧合通过）
     */
    private List<MockElement> createDataSource(int size) {
        return LongStream.rangeClosed(1, size)
            .map(i -> i * 2)
            .mapToObj(MockElement::new)
            .collect(Collectors.toList());
    }

    /**
     * 生成期望的 ID 列表（与 createDataSource 对应：2, 4, 6, ..., size*2）
     */
    private List<Long> expectedIds(int size) {
        return LongStream.rangeClosed(1, size)
            .map(i -> i * 2)
            .boxed()
            .collect(Collectors.toList());
    }

    /**
     * 模拟 offset 分页查询
     */
    private MockPageResult mockOffsetQuery(List<MockElement> dataSource, int start, int limit) {
        if (start >= dataSource.size()) {
            return new MockPageResult(Collections.emptyList());
        }
        int end = Math.min(start + limit, dataSource.size());
        return new MockPageResult(new ArrayList<>(dataSource.subList(start, end)));
    }

    /**
     * 模拟 keyset 分页查询（id > lastId）
     */
    private MockPageResult mockKeysetQuery(List<MockElement> dataSource, long lastId, int limit) {
        List<MockElement> filtered = dataSource.stream()
            .filter(e -> e.getId() > lastId)
            .limit(limit)
            .collect(Collectors.toList());
        return new MockPageResult(filtered);
    }

    /**
     * 统一查询（根据请求类型自动选择 offset 或 keyset）
     */
    private MockPageResult mockUnifiedQuery(List<MockElement> dataSource, UnifiedPageReq req) {
        if (req.isOffsetMode()) {
            return mockOffsetQuery(dataSource, req.getStart(), req.getLimit());
        } else {
            return mockKeysetQuery(dataSource, req.getLastId(), req.getLimit());
        }
    }

    /**
     * 使用小阈值的测试配置，方便测试各区域逻辑
     * 浅层区: 0~20, 中层区: 20~50, 深层区: 50+
     * pageSize=5, 浅层区并发2, 中层区并发2, 中层区每任务2页
     * 浅层区最多4页(页[0,4))，中层区最多6页(页[4,10))，可拆分为 ceil(6/2)=3 个任务
     */
    private TieredPageQueryConfig smallConfig() {
        return TieredPageQueryConfig.builder()
            .pageSize(5)
            .shallowThreshold(20)
            .deepThreshold(50)
            .shallowConcurrency(2)
            .middleConcurrency(2)
            .middlePagesPerTask(2)
            .build();
    }

    // ==================== 配置类测试 ====================

    @Nested
    @DisplayName("TieredPageQueryConfig 测试")
    class ConfigTest {

        @Test
        @DisplayName("缺少必填参数时应抛出异常")
        void shouldThrowExceptionWhenRequiredParamMissing() {
            // 完全不设置任何参数
            assertThatThrownBy(() -> TieredPageQueryConfig.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be set");

            // 只设置部分参数
            assertThatThrownBy(() -> TieredPageQueryConfig.builder()
                .pageSize(500)
                .shallowThreshold(20000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be set");
        }

        @Test
        @DisplayName("区域判断正确")
        void tierDetectionShouldBeCorrect() {
            TieredPageQueryConfig config = smallConfig();
            assertThat(config.getTier(5)).isEqualTo(TieredPageQueryConfig.Tier.SHALLOW);
            assertThat(config.getTier(20)).isEqualTo(TieredPageQueryConfig.Tier.SHALLOW);
            assertThat(config.getTier(21)).isEqualTo(TieredPageQueryConfig.Tier.MIDDLE);
            assertThat(config.getTier(35)).isEqualTo(TieredPageQueryConfig.Tier.MIDDLE);
            assertThat(config.getTier(50)).isEqualTo(TieredPageQueryConfig.Tier.MIDDLE);
            assertThat(config.getTier(51)).isEqualTo(TieredPageQueryConfig.Tier.DEEP);
            assertThat(config.getTier(100)).isEqualTo(TieredPageQueryConfig.Tier.DEEP);
        }

        @Test
        @DisplayName("非法参数应抛出异常")
        void invalidConfigShouldThrowException() {
            // pageSize 为 0
            assertThatThrownBy(() -> TieredPageQueryConfig.builder()
                .pageSize(0).shallowThreshold(10).deepThreshold(25)
                .shallowConcurrency(2).middleConcurrency(2).middlePagesPerTask(3)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");
            // shallowThreshold 为 0
            assertThatThrownBy(() -> TieredPageQueryConfig.builder()
                .pageSize(5).shallowThreshold(0).deepThreshold(25)
                .shallowConcurrency(2).middleConcurrency(2).middlePagesPerTask(3)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shallowThreshold");
            // deepThreshold <= shallowThreshold
            assertThatThrownBy(() -> TieredPageQueryConfig.builder()
                .pageSize(5).shallowThreshold(100).deepThreshold(50)
                .shallowConcurrency(2).middleConcurrency(2).middlePagesPerTask(3)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deepThreshold");
        }
    }

    // ==================== 浅层区测试 ====================

    @Nested
    @DisplayName("浅层区（Shallow Tier）测试")
    class ShallowTierTest {

        @Test
        @DisplayName("数据量为0时返回空列表")
        void shouldReturnEmptyListWhenCountIsZero() {
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                () -> 0,
                UnifiedPageReq::new,
                req -> new MockPageResult(Collections.emptyList()),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("数据量小于一页时正确返回")
        void shouldReturnDataWhenLessThanOnePage() {
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> dataSource = createDataSource(3);

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                UnifiedPageReq::new,
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            assertThat(result).hasSize(3);
            assertThat(result.stream().map(MockElement::getId).collect(Collectors.toList()))
                .containsExactly(2L, 4L, 6L);
        }

        @Test
        @DisplayName("数据量等于浅层区阈值时使用浅层区策略")
        void shouldUseShallowTierAtThreshold() {
            TieredPageQueryConfig config = smallConfig(); // shallowThreshold=20
            List<MockElement> dataSource = createDataSource(20);

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                UnifiedPageReq::new,
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            assertThat(result).hasSize(20);
            // 验证数据完整且有序
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(20));
        }
    }

    // ==================== 中层区测试 ====================

    @Nested
    @DisplayName("中层区（Middle Tier）测试")
    class MiddleTierTest {

        @Test
        @DisplayName("数据量刚超过浅层区阈值时使用中层区策略")
        void shouldUseMiddleTierJustAboveShallowThreshold() {
            TieredPageQueryConfig config = smallConfig(); // shallowThreshold=20, deepThreshold=50
            List<MockElement> dataSource = createDataSource(21);

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                UnifiedPageReq::new,
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            assertThat(result).hasSize(21);
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(21));
        }

        @Test
        @DisplayName("中层区多任务段间并行、段内串行，验证每个任务的查询参数与结果")
        void shouldRunMultipleMiddleTasksConcurrently() {
            // 使用宽中层区配置: pageSize=5, shallowThreshold=10, deepThreshold=50, middlePagesPerTask=3
            // 50条数据, ID=[2,4,...,100] -> 10页
            // shallowEndPage=2, middleEndPage=10, 中层区页[2,10)共8页
            // 中层区任务数 = ceil(8/3) = 3个任务:
            //   任务0: 页[2,5), anchor offset(10,1)->id=22,
            //          keyset(22,4)->[24,26,28,30], keyset(30,5)->[32,34,36,38,40], keyset(40,5)->[42,44,46,48,50]
            //   任务1: 页[5,8), anchor offset(25,1)->id=52,
            //          keyset(52,4)->[54,56,58,60], keyset(60,5)->[62,64,66,68,70], keyset(70,5)->[72,74,76,78,80]
            //   任务2: 页[8,10), anchor offset(40,1)->id=82,
            //          keyset(82,4)->[84,86,88,90], keyset(90,5)->[92,94,96,98,100]
            TieredPageQueryConfig config = TieredPageQueryConfig.builder()
                .pageSize(5)
                .shallowThreshold(10)
                .deepThreshold(50)
                .shallowConcurrency(1)
                .middleConcurrency(2)
                .middlePagesPerTask(3)
                .build();
            List<MockElement> dataSource = createDataSource(50);
            List<String> offsetQueries = new CopyOnWriteArrayList<>();
            List<String> keysetQueries = new CopyOnWriteArrayList<>();

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                (start, limit) -> {
                    offsetQueries.add("offset:" + start + ":" + limit);
                    return new UnifiedPageReq(start, limit);
                },
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                (lastId, limit) -> {
                    keysetQueries.add("keyset:" + lastId + ":" + limit);
                    return new UnifiedPageReq(lastId, limit);
                }
            );

            // 验证结果列表完整且有序
            assertThat(result).hasSize(50);
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(50));

            // 验证 offset 查询: 浅层区2页 + 中层区3个anchor = 5次
            assertThat(offsetQueries).hasSize(5);
            assertThat(offsetQueries).contains(
                "offset:0:5", "offset:5:5",                    // 浅层区
                "offset:10:1", "offset:25:1", "offset:40:1"    // 中层区3个任务的anchor
            );

            // 验证 keyset 查询: 任务0(3次) + 任务1(3次) + 任务2(2次) = 8次
            // 三个任务段间并行，但各自段内串行
            assertThat(keysetQueries).hasSize(8);
            // 任务0段内串行: keyset(22,4) -> keyset(30,5) -> keyset(40,5)
            assertThat(keysetQueries).contains("keyset:22:4", "keyset:30:5", "keyset:40:5");
            // 任务1段内串行: keyset(52,4) -> keyset(60,5) -> keyset(70,5)
            assertThat(keysetQueries).contains("keyset:52:4", "keyset:60:5", "keyset:70:5");
            // 任务2段内串行: keyset(82,4) -> keyset(90,5)
            assertThat(keysetQueries).contains("keyset:82:4", "keyset:90:5");
        }

        @Test
        @DisplayName("中层区浅层部分使用offset并行，中层部分使用keyset串行，验证每次查询参数与结果")
        void shouldUseCorrectStrategyForEachPart() {
            // 30条数据, ID=[2,4,6,...,60], pageSize=5 -> 6页
            // shallowEndPage=4, middleEndPage=min(10,6)=6, 中层区页[4,6)共2页
            // middlePagesPerTask=2, 任务数=ceil(2/2)=1个中层任务
            // 浅层区: page0 offset(0,5), page1 offset(5,5), page2 offset(10,5), page3 offset(15,5)
            // 中层区: anchor offset(20,1)->id=42, keyset(42,4)->[44,46,48,50], keyset(50,5)->[52,54,56,58,60]
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> dataSource = createDataSource(30);
            // 记录每次查询的参数: "offset:start:limit" 或 "keyset:lastId:limit"
            List<String> offsetQueries = new CopyOnWriteArrayList<>();
            List<String> keysetQueries = new CopyOnWriteArrayList<>();

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                (start, limit) -> {
                    offsetQueries.add("offset:" + start + ":" + limit);
                    return new UnifiedPageReq(start, limit);
                },
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                (lastId, limit) -> {
                    keysetQueries.add("keyset:" + lastId + ":" + limit);
                    return new UnifiedPageReq(lastId, limit);
                }
            );

            // 验证结果列表完整且有序
            assertThat(result).hasSize(30);
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(30));

            // 验证 offset 查询: 浅层区4页 + 中层区1个anchor = 5次
            assertThat(offsetQueries).hasSize(5);
            // 浅层区页并行，顺序不确定，但一定包含这四个
            assertThat(offsetQueries).contains("offset:0:5", "offset:5:5", "offset:10:5", "offset:15:5");
            // 中层区 anchor 查询
            assertThat(offsetQueries).contains("offset:20:1");

            // 验证 keyset 查询: 补齐第一页(lastId=42,limit=4) + 第2页(lastId=50,limit=5) = 2次
            // 中层区段内串行，顺序确定
            assertThat(keysetQueries).hasSize(2);
            assertThat(keysetQueries.get(0)).isEqualTo("keyset:42:4");
            assertThat(keysetQueries.get(1)).isEqualTo("keyset:50:5");
        }
    }

    // ==================== 深层区测试 ====================

    @Nested
    @DisplayName("深层区（Deep Tier）测试")
    class DeepTierTest {

        @Test
        @DisplayName("深层区场景下三区叠加并行，验证每次查询参数与结果")
        void shouldRunAllThreeTiersConcurrently() {
            // 55条数据, ID=[2,4,6,...,110], pageSize=5 -> 11页
            // shallowEndPage=4, middleEndPage=10, deepStartPage=10
            // 浅层区: page0 offset(0,5), page1 offset(5,5), page2 offset(10,5), page3 offset(15,5)
            // 中层区(页[4,10)共6页, middlePagesPerTask=2, 任务数=3):
            //   任务0: 页[4,6), anchor offset(20,1)->id=42,
            //          keyset(42,4)->[44,46,48,50], keyset(50,5)->[52,54,56,58,60]
            //   任务1: 页[6,8), anchor offset(30,1)->id=62,
            //          keyset(62,4)->[64,66,68,70], keyset(70,5)->[72,74,76,78,80]
            //   任务2: 页[8,10), anchor offset(40,1)->id=82,
            //          keyset(82,4)->[84,86,88,90], keyset(90,5)->[92,94,96,98,100]
            // 深层区: anchor offset(50,1)->id=102, keyset(102,5)->[104,106,108,110](4条<5,结束)
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> dataSource = createDataSource(55);
            List<String> offsetQueries = new CopyOnWriteArrayList<>();
            List<String> keysetQueries = new CopyOnWriteArrayList<>();

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                (start, limit) -> {
                    offsetQueries.add("offset:" + start + ":" + limit);
                    return new UnifiedPageReq(start, limit);
                },
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                (lastId, limit) -> {
                    keysetQueries.add("keyset:" + lastId + ":" + limit);
                    return new UnifiedPageReq(lastId, limit);
                }
            );

            // 验证结果列表完整且有序
            assertThat(result).hasSize(55);
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(55));

            // 验证 offset 查询: 浅层区4页 + 中层区3个anchor + 深层区1个anchor = 8次
            assertThat(offsetQueries).hasSize(8);
            assertThat(offsetQueries).contains(
                "offset:0:5", "offset:5:5", "offset:10:5", "offset:15:5",  // 浅层区
                "offset:20:1", "offset:30:1", "offset:40:1",               // 中层区3个任务的anchor
                "offset:50:1"                                               // 深层区 anchor
            );

            // 验证 keyset 查询: 中层区任务0(2次) + 任务1(2次) + 任务2(2次) + 深层区1次 = 7次
            // 注意：中层区和深层区是并行的，但各自内部是串行的
            assertThat(keysetQueries).hasSize(7);
            // 中层区任务0段内串行: keyset(42,4) -> keyset(50,5)
            assertThat(keysetQueries).contains("keyset:42:4", "keyset:50:5");
            // 中层区任务1段内串行: keyset(62,4) -> keyset(70,5)
            assertThat(keysetQueries).contains("keyset:62:4", "keyset:70:5");
            // 中层区任务2段内串行: keyset(82,4) -> keyset(90,5)
            assertThat(keysetQueries).contains("keyset:82:4", "keyset:90:5");
            // 深层区串行: keyset(102,5)
            assertThat(keysetQueries).contains("keyset:102:5");
        }

        @Test
        @DisplayName("大数据量深层区串行拉取结果完整且有序")
        void shouldHandleLargeDataSetInDeepTier() {
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> dataSource = createDataSource(100);

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                UnifiedPageReq::new,
                req -> mockUnifiedQuery(dataSource, req),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            assertThat(result).hasSize(100);
            List<Long> ids = result.stream().map(MockElement::getId).collect(Collectors.toList());
            assertThat(ids).containsExactlyElementsOf(expectedIds(100));
        }
    }

    // ==================== 异常处理测试 ====================

    @Nested
    @DisplayName("异常处理")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("count 查询失败时返回空列表")
        void shouldReturnEmptyListWhenCountQueryFails() {
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                () -> {
                    throw new RuntimeException("count query failed");
                },
                UnifiedPageReq::new,
                req -> new MockPageResult(Collections.emptyList()),
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("浅层区某页查询失败时其他页数据不受影响")
        void shouldReturnPartialResultsWhenShallowPageFails() {
            TieredPageQueryConfig config = smallConfig();
            List<MockElement> dataSource = createDataSource(10);

            List<MockElement> result = TieredPageQueryUtil.queryAll(
                config,
                null,
                dataSource::size,
                UnifiedPageReq::new,
                req -> {
                    if (req.isOffsetMode() && req.getStart() == 5) {
                        throw new RuntimeException("page query failed for start=5");
                    }
                    return mockUnifiedQuery(dataSource, req);
                },
                MockPageResult::data,
                MockElement::getId,
                UnifiedPageReq::new
            );

            // 第2页（start=5）失败，第1页（0-4）的5条数据应该正常返回
            assertThat(result).hasSize(5);
        }
    }
}
