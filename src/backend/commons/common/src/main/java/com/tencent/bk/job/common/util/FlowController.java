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

import java.util.Map;

public interface FlowController {

    /**
     * 更新流控配置
     *
     * @param configMap (资源Id->每秒最大速率)
     * @return 成功更新的配置数量
     */
    int updateConfig(Map<String, Long> configMap);

    /**
     * 获取当前资源流控配置
     *
     * @return 每秒最大速率
     */
    Long getCurrentConfig(String resourceId);

    /**
     * 获取当前流控配置
     *
     * @return configMap (资源Id->每秒最大速率)
     */
    Map<String, Long> getCurrentConfig();

    /**
     * 获取当前资源使用速率
     *
     * @return 当前资源使用速率
     */
    Long getCurrentRate(String resourceId);

    /**
     * 获取当前资源使用速率Map
     *
     * @return 当前资源使用速率Map
     */
    Map<String, Long> getCurrentRateMap();

    /**
     * 阻塞直到可执行
     *
     * @param resourceId 资源Id
     */
    void acquire(String resourceId);

    /**
     * 尝试获取
     *
     * @param resourceId 资源Id
     * @return 是否可用
     */
    boolean tryAcquire(String resourceId);
}
