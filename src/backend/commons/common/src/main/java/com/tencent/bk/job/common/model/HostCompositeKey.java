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

package com.tencent.bk.job.common.model;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 主机复合 KEY，用于主机的多种表达方式
 */
@Getter
@Setter
public class HostCompositeKey {
    /**
     * Key 类型
     */
    private final HostCompositeKeyType keyType;

    /**
     * 主机唯一 key（目前支持 hostId/cloudIp 两种）
     */
    private final String key;

    public HostCompositeKey(HostCompositeKeyType keyType, String key) {
        this.keyType = keyType;
        this.key = key;
    }

    public static HostCompositeKey ofHost(HostDTO host) {
        if (host.getHostId() != null) {
            // 优先使用 hostId
            return new HostCompositeKey(HostCompositeKeyType.HOST_ID, String.valueOf(host.getHostId()));
        } else if (host.toCloudIp() != null) {
            // 没有 hostId, 使用管控区域 ID + ipv4
            return new HostCompositeKey(HostCompositeKeyType.CLOUD_IP, host.toCloudIp());
        } else {
            throw new IllegalArgumentException("Invalid host, both hostId or cloudIp are empty");
        }
    }


    @Getter
    public enum HostCompositeKeyType {
        /**
         * HostId 作为 KEY
         */
        HOST_ID(1),
        /**
         * 管控区域 ID+ ipv4 作为 KEY
         */
        CLOUD_IP(2);

        private final int value;

        HostCompositeKeyType(int value) {
            this.value = value;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostCompositeKey that = (HostCompositeKey) o;
        if (this.getKeyType() != that.getKeyType()) {
            return false;
        }

        return keyType == ((HostCompositeKey) o).getKeyType() && key.equals(that.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyType, key);
    }

    @Override
    public String toString() {
        return keyType.getValue() + ":" + key;
    }
}
