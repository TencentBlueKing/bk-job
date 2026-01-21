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

package com.tencent.bk.job.analysis.service.ai.context.model;

import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.execute.model.web.vo.ExecuteObjectVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务上下文查询条件
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaskContextQuery {

    /**
     * 作业平台业务ID
     */
    private Long appId;

    /**
     * 任务ID
     */
    private Long taskInstanceId;

    /**
     * 步骤ID
     */
    private Long stepInstanceId;

    /**
     * 执行次数
     */
    private Integer executeCount;

    /**
     * 滚动批次，非滚动步骤不需要传入
     */
    private Integer batch;

    /**
     * 执行对象类型
     */
    private Integer executeObjectType;

    /**
     * 执行对象资源 ID
     */
    private Long executeObjectResourceId;

    /**
     * 文件任务上传下载标识,0-上传,1-下载
     */
    private Integer mode;

    public static TaskContextQuery fromAIAnalyzeErrorReq(Long appId, AIAnalyzeErrorReq req) {
        TaskContextQuery contextQuery = new TaskContextQuery();
        contextQuery.setAppId(appId);
        contextQuery.setTaskInstanceId(req.getTaskInstanceId());
        contextQuery.setStepInstanceId(req.getStepInstanceId());
        contextQuery.setExecuteCount(req.getExecuteCount());
        contextQuery.setBatch(req.getBatch());
        contextQuery.setExecuteObjectType(req.getExecuteObjectType());
        contextQuery.setExecuteObjectResourceId(req.getExecuteObjectResourceId());
        contextQuery.setMode(req.getMode());
        return contextQuery;
    }

    public String getExecuteObjectId() {
        return ExecuteObjectVO.buildExecuteObjectId(executeObjectType, executeObjectResourceId);
    }
}
