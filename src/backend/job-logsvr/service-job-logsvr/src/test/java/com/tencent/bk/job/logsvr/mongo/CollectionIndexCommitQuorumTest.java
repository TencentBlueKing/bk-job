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

package com.tencent.bk.job.logsvr.mongo;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CreateIndexOptions;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * mongodb集合加载器单元测试：通过 Spring 注入配置，用 Mockito 模拟 MongoDB 索引操作。
 */
@ExtendWith(MockitoExtension.class)
public class CollectionIndexCommitQuorumTest {
    private static final String COLLECTION_NAME = "job_log_test";
    private static final String COMMIT_QUORUM_PROPERTY = "job.logsvr.mongodb.index.commit-quorum";

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private MongoCollection<Document> collection;
    @Mock
    private ListIndexesIterable<Document> indexes;
    @Mock
    private MongoCursor<Document> indexCursor;

    @BeforeEach
    void setUp() {
        when(mongoTemplate.getCollection(COLLECTION_NAME)).thenReturn(collection);
        when(collection.listIndexes()).thenReturn(indexes);
        when(indexes.iterator()).thenReturn(indexCursor);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("未配置或留空时沿用 MongoDB 默认仲裁行为")
    void leavesQuorumUnset(boolean configureEmptyValue) {
        String[] properties = configureEmptyValue ? new String[]{COMMIT_QUORUM_PROPERTY + "="} : new String[0];
        try (AnnotationConfigApplicationContext context = createContext(properties)) {
            context.getBean(ScriptLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
            context.getBean(FileLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
        }

        ArgumentCaptor<CreateIndexOptions> options = ArgumentCaptor.forClass(CreateIndexOptions.class);
        verify(collection, times(7)).createIndexes(anyList(), options.capture());
        options.getAllValues().forEach(option -> assertNull(option.getCommitQuorum()));
    }

    @Test
    @DisplayName("脚本日志和文件日志建索引时均使用配置的仲裁模式")
    void appliesConfiguredQuorumToScriptAndFileIndexes() {
        try (AnnotationConfigApplicationContext context = createContext(COMMIT_QUORUM_PROPERTY + "=majority")) {
            context.getBean(ScriptLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
            context.getBean(FileLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
        }

        ArgumentCaptor<CreateIndexOptions> options = ArgumentCaptor.forClass(CreateIndexOptions.class);
        verify(collection, times(7)).createIndexes(anyList(), options.capture());
        options.getAllValues().forEach(option ->
            assertEquals(new BsonString("majority"), option.getCommitQuorum().toBsonValue()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    @DisplayName("数字仲裁值以整数传给 MongoDB")
    void passesNumericQuorumAsAnInteger(int quorum) {
        try (AnnotationConfigApplicationContext context = createContext(COMMIT_QUORUM_PROPERTY + "=" + quorum)) {
            context.getBean(ScriptLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
        }

        ArgumentCaptor<CreateIndexOptions> options = ArgumentCaptor.forClass(CreateIndexOptions.class);
        verify(collection, times(3)).createIndexes(anyList(), options.capture());
        options.getAllValues().forEach(option ->
            assertEquals(new BsonInt32(quorum), option.getCommitQuorum().toBsonValue()));
    }

    @Test
    @DisplayName("配置仲裁后仍跳过已存在的索引")
    void skipsIndexesThatAlreadyExist() {
        when(indexCursor.hasNext()).thenReturn(true, false);
        when(indexCursor.next()).thenReturn(new Document("name", "stepId_hashed"));

        try (AnnotationConfigApplicationContext context = createContext(COMMIT_QUORUM_PROPERTY + "=majority")) {
            context.getBean(ScriptLogsCollectionLoader.class).load(mongoTemplate, COLLECTION_NAME);
        }

        verify(collection, times(2)).createIndexes(anyList(), any(CreateIndexOptions.class));
        verify(collection, never()).createIndexes(
            argThat(indexes -> indexes.stream()
                .anyMatch(index -> "stepId_hashed".equals(index.getOptions().getName()))),
            any(CreateIndexOptions.class));
    }

    private AnnotationConfigApplicationContext createContext(String... properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of(properties).applyTo(context);
        context.register(ScriptLogsCollectionLoader.class, FileLogsCollectionLoader.class);
        context.refresh();
        return context;
    }
}
