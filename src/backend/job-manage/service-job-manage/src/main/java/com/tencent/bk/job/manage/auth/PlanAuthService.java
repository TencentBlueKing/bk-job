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

package com.tencent.bk.job.manage.auth;

import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;

import java.util.List;

/**
 * 执行方案相关操作鉴权接口
 */
public interface PlanAuthService {

    /**
     * 资源范围下创建执行方案鉴权
     *
     * @param username        用户名
     * @param appResourceScope   资源范围
     * @param jobTemplateId   作业模板ID
     * @param jobTemplateName 作业模板名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authCreateJobPlan(String username,
                                 AppResourceScope appResourceScope,
                                 Long jobTemplateId,
                                 String jobTemplateName);

    /**
     * 资源范围下查看执行方案鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @param jobPlanId     执行方案ID
     * @param jobPlanName   执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authViewJobPlan(String username,
                               AppResourceScope appResourceScope,
                               Long jobTemplateId,
                               Long jobPlanId,
                               String jobPlanName);

    /**
     * 资源范围下编辑执行方案鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @param jobPlanId     执行方案ID
     * @param jobPlanName   执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authEditJobPlan(String username,
                               AppResourceScope appResourceScope,
                               Long jobTemplateId,
                               Long jobPlanId,
                               String jobPlanName);

    /**
     * 资源范围下删除执行方案鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @param jobPlanId     执行方案ID
     * @param jobPlanName   执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authDeleteJobPlan(String username,
                                 AppResourceScope appResourceScope,
                                 Long jobTemplateId,
                                 Long jobPlanId,
                                 String jobPlanName);

    /**
     * 资源范围下同步执行方案鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param jobTemplateId 作业模板ID
     * @param jobPlanId     执行方案ID
     * @param jobPlanName   执行方案名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authSyncJobPlan(String username,
                               AppResourceScope appResourceScope,
                               Long jobTemplateId,
                               Long jobPlanId,
                               String jobPlanName);

    /**
     * 资源范围下查看执行方案批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @param jobPlanIdList     执行方案ID列表
     * @return 有权限的执行方案ID
     */
    List<Long> batchAuthViewJobPlan(String username,
                                    AppResourceScope appResourceScope,
                                    List<Long> jobTemplateIdList,
                                    List<Long> jobPlanIdList);

    /**
     * 资源范围下编辑执行方案批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @param jobPlanIdList     执行方案ID列表
     * @return 有权限的执行方案ID
     */
    List<Long> batchAuthEditJobPlan(String username,
                                    AppResourceScope appResourceScope,
                                    List<Long> jobTemplateIdList,
                                    List<Long> jobPlanIdList);

    /**
     * 资源范围下删除执行方案批量鉴权
     *
     * @param username          用户名
     * @param appResourceScope     资源范围
     * @param jobTemplateIdList 作业模板ID列表
     * @param jobPlanIdList     执行方案ID列表
     * @return 有权限的执行方案ID
     */
    List<Long> batchAuthDeleteJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      List<Long> jobTemplateIdList,
                                      List<Long> jobPlanIdList);

    /**
     * 注册执行方案实例
     *
     * @param id        执行方案 ID
     * @param name      资源实例名称
     * @param creator   资源实例创建者
     * @return 是否注册成功
     */
    boolean registerPlan(Long id, String name, String creator);
}
