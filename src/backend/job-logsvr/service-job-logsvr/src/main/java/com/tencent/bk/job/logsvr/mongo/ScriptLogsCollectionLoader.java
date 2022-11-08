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

package com.tencent.bk.job.logsvr.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * job script log mongodb collection loader
 */
@Component
@Slf4j
public class ScriptLogsCollectionLoader extends CollectionLoaderBase {

    private static final String IDX_STEP_ID_EXECUTE_COUNT_HOST_ID = "stepId_executeCount_hostId";
    private static final String IDX_STEP_ID_HASHED = "stepId_hashed";

    @Override
    public MongoCollection<Document> load(MongoTemplate mongoTemplate, String collectionName) {
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        List<String> indexes = getIndexesNames(collection);
        createIndexIfUnavailable(collection, indexes, collectionName);
        shardCollectionIfShardingEnable(mongoTemplate, collectionName);
        return collection;
    }

    private void createIndexIfUnavailable(MongoCollection<Document> collection, List<String> indexes,
                                          String collectionName) {
        log.info("Create index for collection: {} start...", collectionName);
        if (!indexes.contains(IDX_STEP_ID_HASHED)) {
            log.info("Create index {} for collection: {}start...", IDX_STEP_ID_HASHED, collectionName);
            IndexOptions indexOptions = new IndexOptions();
            indexOptions.background(false);
            indexOptions.name(IDX_STEP_ID_HASHED);
            collection.createIndex(Document.parse("{\"stepId\":\"hashed\"}"), indexOptions);
            log.info("Create index {} for collection: {} successfully!", IDX_STEP_ID_HASHED, collectionName);
        }

        if (!indexes.contains(IDX_STEP_ID_EXECUTE_COUNT_HOST_ID)) {
            log.info("Create index {} for collection: {} start...", IDX_STEP_ID_EXECUTE_COUNT_HOST_ID, collectionName);
            IndexOptions indexOptions = new IndexOptions();
            indexOptions.background(false);
            indexOptions.name(IDX_STEP_ID_EXECUTE_COUNT_HOST_ID);
            collection.createIndex(Document.parse("{\"stepId\":1,\"executeCount\":1,\"hostId\":1}"),
                indexOptions);
            log.info("Create index {} for collection: {} successfully!", IDX_STEP_ID_EXECUTE_COUNT_HOST_ID,
                collectionName);
        }
        log.info("Create index for collection : {} successfully!", collectionName);
    }
}
