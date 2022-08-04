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

import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Agent任务执行结果分组
 */
@Data
@NoArgsConstructor
public class AgentTaskResultGroupBaseDTO implements Comparable<AgentTaskResultGroupBaseDTO> {
    /**
     * 任务状态
     *
     * @see AgentTaskStatusEnum
     */
    private Integer status;
    /**
     * 用户脚本输出的自定义分组tag
     */
    private String tag;
    /**
     * Agent任务总数
     */
    private int totalAgentTasks;

    public AgentTaskResultGroupBaseDTO(Integer status, String tag) {
        this.status = status;
        this.tag = tag;
    }

    public AgentTaskResultGroupBaseDTO(AgentTaskResultGroupBaseDTO baseResultGroup) {
        this.status = baseResultGroup.getStatus();
        this.tag = baseResultGroup.getTag();
        this.totalAgentTasks = baseResultGroup.getTotalAgentTasks();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentTaskResultGroupBaseDTO that = (AgentTaskResultGroupBaseDTO) o;
        return status.equals(that.status) &&
            tagEquals(tag, that.tag);
    }

    private boolean tagEquals(String thisTag, String thatTag) {
        String tag1 = thisTag == null ? "" : thisTag;
        String tag2 = thatTag == null ? "" : thatTag;
        return tag1.equals(tag2);
    }

    @Override
    public int hashCode() {
        if (this.tag == null) {
            return Objects.hash(status, "");
        } else {
            return Objects.hash(status, tag);
        }
    }

    @Override
    public int compareTo(AgentTaskResultGroupBaseDTO that) {
        int result = status.compareTo(that.status);
        if (result != 0) {
            return result;
        }
        String tag1 = this.tag == null ? "" : this.tag;
        String tag2 = that.tag == null ? "" : that.tag;
        return tag1.compareTo(tag2);
    }
}
