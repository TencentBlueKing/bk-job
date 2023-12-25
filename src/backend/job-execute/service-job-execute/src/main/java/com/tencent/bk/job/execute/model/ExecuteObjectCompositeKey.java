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

import java.util.Objects;

/**
 * 执行对象复合 KEY
 */
@Getter
@Setter
public class ExecuteObjectCompositeKey {
    /**
     * 执行对象复合 KEY 的类型
     */
    private CompositeKeyType compositeKeyType;
    /**
     * 执行对象类型
     */
    private ExecuteObjectTypeEnum executeObjectType;


    /**
     * 执行对象 ID
     */
    private String executeObjectId;


    /**
     * 执行对象对应资源的 ID
     */
    private Long resourceId;
    /**
     * 主机 ID
     */
    private Long hostId;
    /**
     * 容器 ID
     */
    private Long containerId;


    /**
     * 管控区域+ipv4 方式标识主机
     */
    private String cloudIp;

    private ExecuteObjectCompositeKey() {
    }


    public static ExecuteObjectCompositeKey of(ExecuteObjectTypeEnum executeObjectType, String executeObjectId) {
        ExecuteObjectCompositeKey key = new ExecuteObjectCompositeKey();
        key.setCompositeKeyType(CompositeKeyType.EXECUTE_OBJECT_ID);
        key.setExecuteObjectType(executeObjectType);
        key.setExecuteObjectId(executeObjectId);
        return key;
    }

    public static ExecuteObjectCompositeKey ofHost(Long hostId) {
        ExecuteObjectCompositeKey key = new ExecuteObjectCompositeKey();
        key.setCompositeKeyType(CompositeKeyType.RESOURCE_ID);
        key.setResourceId(hostId);
        key.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        key.setHostId(hostId);
        return key;
    }

    public static ExecuteObjectCompositeKey ofHost(String cloudIp) {
        ExecuteObjectCompositeKey key = new ExecuteObjectCompositeKey();
        key.setCompositeKeyType(CompositeKeyType.HOST_CLOUD_IP);
        key.setExecuteObjectType(ExecuteObjectTypeEnum.HOST);
        key.setCloudIp(cloudIp);
        return key;
    }

    public static ExecuteObjectCompositeKey ofContainer(Long containerId) {
        ExecuteObjectCompositeKey key = new ExecuteObjectCompositeKey();
        key.setCompositeKeyType(CompositeKeyType.RESOURCE_ID);
        key.setResourceId(containerId);
        key.setExecuteObjectType(ExecuteObjectTypeEnum.CONTAINER);
        key.setContainerId(containerId);
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteObjectCompositeKey that = (ExecuteObjectCompositeKey) o;
        if (this.getCompositeKeyType() != that.getCompositeKeyType()) {
            return false;
        }
        switch (this.getCompositeKeyType()) {
            case EXECUTE_OBJECT_ID:
                return this.getExecuteObjectId().equals(that.getExecuteObjectId());
            case RESOURCE_ID:
                return this.getExecuteObjectType() == that.getExecuteObjectType()
                    && this.getResourceId().equals(that.getResourceId());
            case HOST_CLOUD_IP:
                return this.getCloudIp().equals(that.getCloudIp());
        }
        return false;
    }

    @Override
    public int hashCode() {
        switch (this.getCompositeKeyType()) {
            case EXECUTE_OBJECT_ID:
                return Objects.hash(compositeKeyType, executeObjectId);
            case RESOURCE_ID:
                return Objects.hash(compositeKeyType, resourceId);
            case HOST_CLOUD_IP:
                return Objects.hash(compositeKeyType, cloudIp);
            default:
                return 0;
        }
    }

    private enum CompositeKeyType {
        EXECUTE_OBJECT_ID(1),
        RESOURCE_ID(2),
        HOST_CLOUD_IP(3);

        private final int value;

        CompositeKeyType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
