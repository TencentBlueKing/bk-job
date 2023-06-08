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
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;

/**
 * Agent 正常状态
 */
public enum AgentAliveStatusEnum {
    /**
     * 异常
     */
    NOT_ALIVE(0),
    /**
     * 正常
     */
    ALIVE(1),
    /**
     * 未知
     */
    UNKNOWN(2);
    @JsonValue
    private final int status;

    AgentAliveStatusEnum(int status) {
        this.status = status;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AgentAliveStatusEnum valOf(int status) {
        for (AgentAliveStatusEnum agentAliveStatus : values()) {
            if (agentAliveStatus.status == status) {
                return agentAliveStatus;
            }
        }
        return null;
    }

    public static AgentAliveStatusEnum fromAgentState(AgentState agentState) {
        if (agentState == null || agentState.getStatusCode() == null) {
            return UNKNOWN;
        } else if (agentState.getStatusCode().equals(AgentStateStatusEnum.RUNNING.getValue())) {
            return ALIVE;
        } else {
            return NOT_ALIVE;
        }
    }

    public int getStatusValue() {
        return status;
    }
}
