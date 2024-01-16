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

package com.tencent.bk.job.common.gse.v2.model;

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.gse.util.K8sUtils;
import lombok.Getter;

import java.util.Objects;

/**
 * 执行对象 GSE KEY, 用于跟 GSE 交互
 */
@Getter
public class ExecuteObjectGseKey {
    /**
     * GSE Agent ID
     */
    private String agentId;
    /**
     * 容器 ID（比如 docker id）
     */
    private String containerId;
    /**
     * 完整的唯一 KEY
     */
    private String key;

    private ExecuteObjectGseKey() {
    }

    public static ExecuteObjectGseKey ofHost(String agentId) {
        ExecuteObjectGseKey executeObjectGseKey = new ExecuteObjectGseKey();
        // agentId 指定主机对象
        executeObjectGseKey.agentId = agentId;
        executeObjectGseKey.key = ExecuteObjectTypeEnum.HOST.getValue() + ":" + agentId;
        return executeObjectGseKey;
    }

    public static ExecuteObjectGseKey ofContainer(String agentId, String containerId) {
        ExecuteObjectGseKey executeObjectGseKey = new ExecuteObjectGseKey();
        // agentId+containerId 唯一指定容器对象
        executeObjectGseKey.agentId = agentId;
        executeObjectGseKey.containerId = K8sUtils.removeContainerIdType(containerId);
        executeObjectGseKey.key = ExecuteObjectTypeEnum.CONTAINER.getValue() + ":" + agentId + ":"
            + executeObjectGseKey.getContainerId();
        return executeObjectGseKey;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteObjectGseKey that = (ExecuteObjectGseKey) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return key;
    }
}
