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

package com.tencent.bk.job.common.constant;

import java.util.EnumSet;
import java.util.Set;

/**
 * 容器拓扑节点资源类型。cluster / namespace 是拓扑层级，其余为 workload 的具体类型。
 */
public enum KubeTopoNodeTypeEnum {
    CLUSTER("cluster"),
    NAMESPACE("namespace"),

    // ----------------------------------- workload 类型 ------------------------------
    DEPLOYMENT("deployment"),
    DAEMON_SET("daemonSet"),
    STATEFUL_SET("statefulSet"),
    CRON_JOB("cronJob"),
    JOB("job"),
    CUSTOM_RESOURCE("customResource"),
    GAME_STATEFUL_SET("gameStatefulSet"),
    GAME_DEPLOYMENT("gameDeployment"),
    PODS("pods");

    /**
     * workload 层级的节点类型（排除 cluster / namespace）。
     */
    private static final Set<KubeTopoNodeTypeEnum> WORKLOAD_TYPES = EnumSet.of(
        DEPLOYMENT,
        DAEMON_SET,
        STATEFUL_SET,
        CRON_JOB,
        JOB,
        CUSTOM_RESOURCE,
        GAME_STATEFUL_SET,
        GAME_DEPLOYMENT,
        PODS
    );

    private final String value;

    KubeTopoNodeTypeEnum(String val) {
        this.value = val;
    }

    public String getValue() {
        return value;
    }

    public static KubeTopoNodeTypeEnum fromValue(String value) {
        for (KubeTopoNodeTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断给定 kind 是否为合法的 workload 类型。
     */
    public static boolean isValidWorkloadKind(String kind) {
        KubeTopoNodeTypeEnum type = fromValue(kind);
        if (type == null) {
            return false;
        }
        return WORKLOAD_TYPES.contains(type);
    }
}
