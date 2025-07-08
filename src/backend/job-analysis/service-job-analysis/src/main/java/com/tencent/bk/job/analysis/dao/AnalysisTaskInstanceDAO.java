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

package com.tencent.bk.job.analysis.dao;

import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceWithTpl;

import java.util.List;

public interface AnalysisTaskInstanceDAO {
    Long insertAnalysisTaskInstance(AnalysisTaskInstanceDTO analysisTaskInstanceDTO);

    int updateAnalysisTaskInstanceById(AnalysisTaskInstanceDTO analysisTaskInstanceDTO);

    int deleteHistoryAnalysisTaskInstance(Long appId, Long taskId);

    int deleteAnalysisTaskInstanceById(Long id);

    AnalysisTaskInstanceDTO getAnalysisTaskInstanceById(Long id);

    List<AnalysisTaskInstanceWithTpl> listAllAnalysisTaskInstance();

    List<AnalysisTaskInstanceWithTpl> listActiveAnalysisTaskInstance(Long appId, Long limit);

    /**
     * 查询最新的分析结果，每个任务一条
     *
     * @param appId
     * @param limit
     * @return 任务分析结果列表
     */
    List<AnalysisTaskInstanceWithTpl> listNewestActiveInstance(Long appId, Long limit);
}
