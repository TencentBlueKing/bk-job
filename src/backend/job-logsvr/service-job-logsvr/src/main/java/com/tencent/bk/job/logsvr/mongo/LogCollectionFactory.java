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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mongodb.client.MongoCollection;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * mongodb job log collection factory
 */
@Component
@Slf4j
public class LogCollectionFactory {
    private MongoTemplate mongoTemplate;
    private LogCollectionLoaderFactory loaderFactory;


    private final LoadingCache<String, MongoCollection<Document>> collectionCache =
        CacheBuilder.newBuilder().maximumSize(30).expireAfterAccess(2, TimeUnit.HOURS).build(new CacheLoader<String,
            MongoCollection<Document>>() {
        @Override
        public MongoCollection<Document> load(String collectionName) {
            if (StringUtils.isBlank(collectionName)) {
                return null;
            }
            CollectionLoader collectionLoader = loaderFactory.getCollectionLoader(collectionName);
            if (collectionLoader == null) {
                return null;
            }
            return collectionLoader.load(mongoTemplate, collectionName);
        }
    });

    @Autowired
    public LogCollectionFactory(MongoTemplate mongoTemplate, LogCollectionLoaderFactory loaderFactory) {
        this.mongoTemplate = mongoTemplate;
        this.loaderFactory = loaderFactory;
    }


    public MongoCollection<Document> getCollection(String collectionName) {
        MongoCollection<Document> collection;
        try {
            collection = collectionCache.get(collectionName);
        } catch (ExecutionException e) {
            log.error("Fail to get collection", e);
            return mongoTemplate.getCollection(collectionName);
        }
        if (collection == null) {
            log.error("Collection {} is not exist!", collectionName);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return collection;
    }

}
