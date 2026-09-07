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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;

/**
 * 由 v4 请求创建执行方案。
 * <p>
 * 这一层的存在目的是让「OpenAPI 直接创建」与「带审批创建（先预检、审批通过后再创建）」共用同一段校验与转换代码。
 * 带审批链路必须复用同一个方法、只切换 {@code dryRun} 取值：若另写一份预检逻辑，两边迟早漂移，
 * 用户会在审批通过后才拿到创建失败。
 */
public interface V4JobPlanCreateService {

    /**
     * 校验并创建执行方案。
     *
     * @param operator 操作人，用于鉴权与记录创建人
     * @param request  v4 创建执行方案请求，方法内会补全 appId / appResourceScope
     * @param dryRun   是否仅预检。为 true 时走完全部校验与鉴权，在执行方案落库之前返回，
     *                 返回的 {@link TaskPlanInfoDTO} 没有 id（未落库）
     * @return dryRun 为 false 时返回已落库的执行方案；为 true 时返回校验通过、待落库的执行方案
     */
    TaskPlanInfoDTO createJobPlan(User operator, V4CreateJobPlanRequest request, boolean dryRun);
}
