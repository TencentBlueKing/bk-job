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

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CollectionLoaderBase implements CollectionLoader {
    @Value("${job.logsvr.mongodb.database:joblog}")
    protected String logDb;

    @Value("${job.logsvr.mongodb.shard.enabled:false}")
    protected boolean enableSharding;

    private Map<String, Boolean> collectionShardStatusMap = new ConcurrentHashMap<>();

    @Override
    public MongoCollection<Document> load(MongoTemplate mongoTemplate, String collectionName) {
        return null;
    }

    protected List<String> getIndexesNames(MongoCollection<Document> collection) {
        ListIndexesIterable<Document> documents = collection.listIndexes();

        List<String> indexes = new ArrayList<>();
        for (Document d : documents) {
            String name = d.getString("name");
            indexes.add(name);
        }
        return indexes;
    }

    protected void shardCollectionIfShardingEnable(MongoTemplate mongoTemplate, String collectionName) {
        if (enableSharding) {
            Boolean isCollectionSharded = collectionShardStatusMap.get(collectionName);
            if (isCollectionSharded != null && isCollectionSharded) {
                log.info("Collection: {} is already sharded!", collectionName);
                return;
            }

            log.info("Shard collection {} start...", collectionName);
            MongoDatabase adminDB = mongoTemplate.getMongoDbFactory().getMongoDatabase("admin");
            String collection = logDb + "." + collectionName;
            adminDB.runCommand(new Document("enableSharding", logDb));
            Document shardCmd = new Document("shardCollection", collection)
                .append("key", new Document("stepId", "hashed"));
            adminDB.runCommand(shardCmd);

            collectionShardStatusMap.put(collectionName, true);
            log.info("Shard collection successfully, collectionName: {}", collectionName);
        }
    }
}
