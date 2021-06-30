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

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * 执行结果分组
 */
@Data
@NoArgsConstructor
public class ExecutionResultGroupDTO {
    /**
     * 执行结果分类
     */
    private Integer resultType;
    /**
     * 用户通过job_success/job_fail自定义的结果分类tag
     */
    private String tag;
    /**
     * Agent任务执行情况
     */
    private List<AgentTaskExecutionDTO> agentTaskExecutionDetail;
    /**
     * 结果分组下的agent任务数
     */
    private int agentTaskSize;

    public ExecutionResultGroupDTO(Integer resultType, String tag) {
        this.resultType = resultType;
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionResultGroupDTO that = (ExecutionResultGroupDTO) o;
        return resultType.equals(that.resultType) &&
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
            return Objects.hash(resultType, "");
        } else {
            return Objects.hash(resultType, tag);
        }
    }
}
