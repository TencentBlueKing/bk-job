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

package com.tencent.bk.job.common.gse.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Agent 状态枚举，-2:未找到 -1:查询失败 0:初始安装 1:启动中 2:运行中 3:有损状态 4:繁忙状态 5:升级中 6:停止中 7:解除安装
 */
public enum AgentStateStatusEnum {
    /**
     * 未找到
     */
    NOT_FIND(-2),
    /**
     * 查询失败
     */
    QUERY_FAIL(-1),
    /**
     * 初始安装
     */
    INIT_INSTALL(0),
    /**
     * 启动中
     */
    BOOTING(1),
    /**
     * 运行中
     */
    RUNNING(2),
    /**
     * 有损状态
     */
    IN_TROUBLE(3),
    /**
     * 繁忙状态
     */
    BUSY(4),
    /**
     * 升级中
     */
    UPGRADING(5),
    /**
     * 停止中
     */
    STOPPING(6),
    /**
     * 解除安装
     */
    REMOVING(7);
    @JsonValue
    private final int status;

    AgentStateStatusEnum(int status) {
        this.status = status;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AgentStateStatusEnum valOf(int status) {
        for (AgentStateStatusEnum agentStatus : values()) {
            if (agentStatus.status == status) {
                return agentStatus;
            }
        }
        return null;
    }

    public int getValue() {
        return status;
    }
}
