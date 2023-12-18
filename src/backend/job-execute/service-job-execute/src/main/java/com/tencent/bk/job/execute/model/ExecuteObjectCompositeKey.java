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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 执行对象复合 KEY
 */
@Getter
@Setter
@ToString
public class ExecuteObjectCompositeKey {
    /**
     * 执行对象实例 ID
     */
    private Long executeObjectId;
    /**
     * 执行对象类型
     */
    private ExecuteObjectTypeEnum executeObjectType;

    /**
     * 主机 ID
     */
    private Long hostId;

    /**
     * ipv4
     */
    private String cloudIp;

    /**
     * 容器 ID
     */
    private Long containerId;

    public static ExecuteObjectCompositeKey of(Long executeObjectId) {
        ExecuteObjectCompositeKey query = new ExecuteObjectCompositeKey();
        query.setExecuteObjectId(executeObjectId);
        return query;
    }

    public static ExecuteObjectCompositeKey ofHost(Long hostId, String cloudIp) {
        ExecuteObjectCompositeKey query = new ExecuteObjectCompositeKey();
        query.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        query.setHostId(hostId);
        return query;
    }

    public static ExecuteObjectCompositeKey ofContainer(Long containerId) {
        ExecuteObjectCompositeKey query = new ExecuteObjectCompositeKey();
        query.setExecuteObjectType(ExecuteObjectTypeEnum.CONTAINER);
        query.setContainerId(containerId);
        return query;
    }

    /**
     * 获取执行对象资源 ID
     */
    public String getResourceId() {
        switch (executeObjectType) {
            case HOST:
                return hostId != null ? String.valueOf(hostId) : null;
            case CONTAINER:
                return containerId != null ? String.valueOf(containerId) : null;
            default:
                return null;
        }
    }
}
