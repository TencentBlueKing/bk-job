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

package com.tencent.bk.job.backup.service;

import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;

import java.util.List;

/**
 * @since 29/7/2020 17:46
 */
public interface TaskPlanService {
    /**
     * 按执行方案 ID 列表拉取执行方案信息
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planIdList 执行方案 ID 列表
     * @return 执行方案信息列表
     */
    List<TaskPlanVO> getTaskPlanByIdList(String username, Long appId, Long templateId, List<Long> planIdList);

    /**
     * 拉作业模版下的全部执行方案信息
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @return 执行方案信息列表
     */
    List<TaskPlanVO> listPlans(String username, Long appId, Long templateId);

    /**
     * 校验 ID 名称是否可用
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planId     执行方案 ID
     * @param name       执行方案名称
     * @return 校验结果
     */
    ServiceIdNameCheckDTO checkIdAndName(Long appId, Long templateId, Long planId, String name);

    /**
     * 新建执行方案
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planInfo   执行方案信息
     * @return 执行方案 ID
     */
    Long savePlan(String username, Long appId, Long templateId, TaskPlanVO planInfo);

    /**
     * 获取执行方案变量信息列表
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planId     执行方案 ID
     * @return 执行方案变量信息列表
     */
    List<ServiceTaskVariableDTO> getPlanVariable(String username, Long appId, Long templateId, Long planId);
}
