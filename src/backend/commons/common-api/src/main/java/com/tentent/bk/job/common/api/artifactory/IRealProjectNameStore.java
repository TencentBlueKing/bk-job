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

package com.tentent.bk.job.common.api.artifactory;

/**
 * 保存创建好的真实项目名称接口
 */
public interface IRealProjectNameStore {

    /**
     * 等待存储服务准备就绪
     *
     * @param maxWaitSeconds 最大等待时间
     * @return 存储服务是否准备就绪
     */
    boolean waitUntilStoreServiceReady(Integer maxWaitSeconds);

    /**
     * 保存真实项目名称
     *
     * @param saveKey         用于存储真实项目名称的Key
     * @param realProjectName 真实项目名称
     */
    void saveRealProjectName(String saveKey, String realProjectName);

    /**
     * 查询真实项目名称
     *
     * @param saveKey 用于存储真实项目名称的Key
     */
    String queryRealProjectName(String saveKey);
}
