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

package com.tencent.bk.job.common.crypto.passwordrotation;

/**
 * 密码轮换进度表 DAO 接口。
 *
 * <p>各微服务使用自己的数据库连接实现此接口，
 * {@link PasswordRotationOrchestrator} 依赖此接口持久化轮换进度。
 */
public interface PasswordRotationProgressDAO {

    /**
     * 加载给定目标密钥指纹 + 表名 + 字段名对应的进度记录。
     * 如果不存在则自动创建一条 PENDING 状态的初始记录并返回。
     *
     * @param targetPasswordFingerprint 目标密钥指纹（{@link com.tencent.bk.job.common.crypto.CryptoConfigService#computeActivePasswordFingerprint()}）
     * @param tableName                 目标表名
     * @param fieldName                 目标字段名
     */
    PasswordRotationProgress loadOrCreate(String targetPasswordFingerprint, String tableName, String fieldName);

    /**
     * 更新中间进度（lastProcessedPk、各计数器、status）
     */
    void updateProgress(PasswordRotationProgress progress);
}
