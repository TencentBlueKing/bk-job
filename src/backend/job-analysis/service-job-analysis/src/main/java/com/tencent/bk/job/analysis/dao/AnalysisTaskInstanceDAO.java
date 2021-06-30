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

package com.tencent.bk.job.analysis.dao;

import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceWithTpl;
import org.jooq.DSLContext;

import java.util.List;

public interface AnalysisTaskInstanceDAO {
    Long insertAnalysisTaskInstance(DSLContext dslContext, AnalysisTaskInstanceDTO analysisTaskInstanceDTO);

    int updateAnalysisTaskInstanceById(DSLContext dslContext, AnalysisTaskInstanceDTO analysisTaskInstanceDTO);

    int deleteHistoryAnalysisTaskInstance(DSLContext dslContext, Long appId, Long taskId);

    int deleteAnalysisTaskInstanceById(DSLContext dslContext, Long id);

    AnalysisTaskInstanceDTO getAnalysisTaskInstanceById(DSLContext dslContext, Long id);

    List<AnalysisTaskInstanceWithTpl> listAllAnalysisTaskInstance(DSLContext dslContext);

    List<AnalysisTaskInstanceWithTpl> listActiveAnalysisTaskInstance(DSLContext dslContext, Long appId, Long limit);

    /**
     * 查询最新的分析结果，每个任务一条
     *
     * @param dslContext
     * @param appId
     * @param limit
     * @return 任务分析结果列表
     */
    List<AnalysisTaskInstanceWithTpl> listNewestActiveInstance(DSLContext dslContext, Long appId,
                                                               Long limit);
}
