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

package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class CollectionUtilTest {
    @Test
    void testPartitionList() {
        List<String> list = new ArrayList<>();
        list.add("a");
        List<List<String>> partitionLists = CollectionUtil.partitionList(list, 1);
        assertThat(partitionLists).hasSize(1);
        assertThat(partitionLists.get(0)).hasSize(1);

        list.add("b");
        partitionLists = CollectionUtil.partitionList(list, 3);
        assertThat(partitionLists).hasSize(1);
        assertThat(partitionLists.get(0)).hasSize(2);

        list.add("c");
        partitionLists = CollectionUtil.partitionList(list, 2);
        assertThat(partitionLists).hasSize(2);
        assertThat(partitionLists.get(0)).hasSize(2);
        assertThat(partitionLists.get(1)).hasSize(1);

        list.add("d");
        partitionLists = CollectionUtil.partitionList(list, 2);
        assertThat(partitionLists).hasSize(2);
        assertThat(partitionLists.get(0)).hasSize(2);
        assertThat(partitionLists.get(1)).hasSize(2);
    }

    @Nested
    class TestPartitionCollection {
        @Test
        void testPartitionList() {
            List<String> list = new ArrayList<>();
            list.add("a");
            List<List<String>> partitionLists = CollectionUtil.partitionCollection(list, 1);
            assertThat(partitionLists).hasSize(1);
            assertThat(partitionLists.get(0)).hasSize(1);

            list.add("b");
            partitionLists = CollectionUtil.partitionCollection(list, 3);
            assertThat(partitionLists).hasSize(1);
            assertThat(partitionLists.get(0)).hasSize(2);

            list.add("c");
            partitionLists = CollectionUtil.partitionCollection(list, 2);
            assertThat(partitionLists).hasSize(2);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(1)).hasSize(1);

            list.add("d");
            partitionLists = CollectionUtil.partitionCollection(list, 2);
            assertThat(partitionLists).hasSize(2);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(1)).hasSize(2);

            // 测试分区较多的场景
            list = new ArrayList<>();
            for (int i = 1; i <= 2009; i++) {
                list.add("test" + i);
            }
            partitionLists = CollectionUtil.partitionCollection(list, 2);
            assertThat(partitionLists).hasSize(1005);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(0).get(0)).isEqualTo("test1");
            assertThat(partitionLists.get(0).get(1)).isEqualTo("test2");
            assertThat(partitionLists.get(1)).hasSize(2);
            assertThat(partitionLists.get(1004)).hasSize(1);
            assertThat(partitionLists.get(1004).get(0)).isEqualTo("test2009");

            List<String> mergedElements =
                partitionLists.stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
            // 测试分区之后与原始的在数量上一致
            assertThat(mergedElements).hasSize(2009);
        }

        @Test
        void testPartitionHashSet() {
            Set<String> set1 = new HashSet<>();
            set1.add("a");
            List<List<String>> partitionLists = CollectionUtil.partitionCollection(set1, 1);
            assertThat(partitionLists).hasSize(1);
            assertThat(partitionLists.get(0)).hasSize(1);

            Set<String> set2 = new HashSet<>();
            set2.add("a");
            set2.add("b");
            partitionLists = CollectionUtil.partitionCollection(set2, 3);
            assertThat(partitionLists).hasSize(1);

            Set<String> set3 = new HashSet<>();
            set3.add("a");
            set3.add("b");
            set3.add("c");
            partitionLists = CollectionUtil.partitionCollection(set3, 2);
            assertThat(partitionLists).hasSize(2);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(1)).hasSize(1);

            Set<String> set4 = new HashSet<>();
            set4.add("a");
            set4.add("b");
            set4.add("c");
            set4.add("d");
            partitionLists = CollectionUtil.partitionCollection(set4, 2);
            assertThat(partitionLists).hasSize(2);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(1)).hasSize(2);

            Set<String> set5 = new HashSet<>();
            set5.add("a");
            set5.add("b");
            set5.add("c");
            set5.add("d");
            set5.add("e");
            set5.add("f");
            set5.add("g");
            partitionLists = CollectionUtil.partitionCollection(set5, 3);
            assertThat(partitionLists).hasSize(3);
            assertThat(partitionLists.get(0)).hasSize(3);
            assertThat(partitionLists.get(1)).hasSize(3);
            assertThat(partitionLists.get(2)).hasSize(1);

            // 测试分区较多的场景
            Set<String> set6 = new HashSet<>();
            for (int i = 1; i <= 2009; i++) {
                set6.add("test" + i);
            }
            partitionLists = CollectionUtil.partitionCollection(set6, 2);
            assertThat(partitionLists).hasSize(1005);
            assertThat(partitionLists.get(0)).hasSize(2);
            assertThat(partitionLists.get(1)).hasSize(2);
            assertThat(partitionLists.get(1004)).hasSize(1);

            List<String> mergedElements =
                partitionLists.stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
            // 测试分区之后与原始的在数量上一致
            assertThat(mergedElements).hasSize(2009);
        }

        @Test
        void testConvertToMap() {
            assertThat(CollectionUtil.convertToMap(null, TestEntity::getKey, TestEntity::getStringValue)).isEmpty();
            List<TestEntity> entityList = new ArrayList<>();
            entityList.add(new TestEntity(null, null, null));
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).size()).isEqualTo(1);
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsKey(null)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsValue(null)).isTrue();
            Long key = 1L;
            String stringValue = "StringValue";
            Object objectValue = new Object();
            entityList.add(new TestEntity(key, stringValue, objectValue));
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).size()).isEqualTo(2);
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsKey(key)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsValue(stringValue)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).size()).isEqualTo(2);
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).containsKey(key)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).containsValue(objectValue)).isTrue();
            String stringValue2 = "StringValue2";
            Object objectValue2 = new Object();
            entityList.add(new TestEntity(2L, stringValue2, objectValue2));
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).size()).isEqualTo(3);
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsKey(key)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsValue(stringValue)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getStringValue).containsValue(stringValue2)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).size()).isEqualTo(3);
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).containsKey(key)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).containsValue(objectValue)).isTrue();
            assertThat(CollectionUtil.convertToMap(entityList, TestEntity::getKey, TestEntity::getObjectValue).containsValue(objectValue2)).isTrue();
        }

        class TestEntity {
            private final Long key;
            private final String stringValue;
            private final Object objectValue;

            public TestEntity(Long key, String stringValue, Object objectValue) {
                this.key = key;
                this.stringValue = stringValue;
                this.objectValue = objectValue;
            }

            public Long getKey() {
                return key;
            }

            public String getStringValue() {
                return stringValue;
            }

            public Object getObjectValue() {
                return objectValue;
            }
        }
    }
}
