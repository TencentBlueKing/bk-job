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

package com.tencent.bk.job.common.mongodb.listener;

import com.mongodb.MongoSecurityException;
import com.tencent.bk.job.common.mongodb.exception.JobMongoAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * MongoDB启动监听器，检查 MongoDB 是否就绪
 * 检查包含连通性检查，认证检查
 */
@Slf4j
public class CheckMongoOnStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final MongoTemplate mongoTemplate;

    public CheckMongoOnStartupListener(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("start validate mongodb connection");
        validateMongoConnection();
    }

    /**
     * 通过 { ping: 1 } 命令检查mongodb连通性
     */
    private void validateMongoConnection() {

        try {
            this.mongoTemplate.executeCommand("{ ping: 1 }");
        } catch (UncategorizedMongoDbException e) {
            if (e.getCause() instanceof MongoSecurityException) {
                // 账号密码错误
                log.error("fail to auth user to mongodb, please check username and password", e);
                throw new JobMongoAuthException(e.getCause());
            } else {
                log.error("mongodb connection failed", e);
                throw e;
            }
        } catch (Exception e) {
            log.error("mongodb health check failed, not allow to start job service", e);
            throw e;
        }
    }
}
