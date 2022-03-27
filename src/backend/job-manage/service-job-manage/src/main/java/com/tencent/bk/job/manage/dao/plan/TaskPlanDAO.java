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

package com.tencent.bk.job.manage.dao.plan;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.task.TaskPlanTypeEnum;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanBasicInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;

import java.util.Collection;
import java.util.List;

/**
 * @since 3/10/2019 21:59
 */
public interface TaskPlanDAO {

    /**
     * 根据模版 ID 拉取关联的执行方案ID
     *
     * @param templateId 模版 ID
     * @return 执行方案ID列表
     */
    List<Long> listTaskPlanIds(Long templateId);

    /**
     * 根据模版 ID 拉取关联的执行方案信息
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 执行方案基础信息列表
     */
    List<TaskPlanInfoDTO> listTaskPlans(Long appId, Long templateId);

    /**
     * 查询执行方案基础信息列表，支持分页
     *
     * @param taskPlanQuery       执行方案查询条件
     * @param baseSearchCondition 基本查询条件
     * @param excludePlanIdList   排除列表
     * @return 执行方案基础信息列表
     */
    PageData<TaskPlanInfoDTO> listPageTaskPlans(TaskPlanQueryDTO taskPlanQuery,
                                                BaseSearchCondition baseSearchCondition,
                                                List<Long> excludePlanIdList);

    /**
     * 根据执行方案 ID 查询执行方案数据
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param planId     执行方案 ID
     * @param planType   执行方案类型
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getTaskPlanById(Long appId, Long templateId, Long planId, TaskPlanTypeEnum planType);

    /**
     * 根据执行方案 ID 查询执行方案数据
     *
     * @param planId 执行方案 ID
     * @return 执行方案数据
     */
    TaskPlanInfoDTO getTaskPlanById(Long planId);

    /**
     * 新增执行方案信息
     *
     * @param planInfo 执行方案信息
     * @return 新执行方案 ID
     */
    Long insertTaskPlan(TaskPlanInfoDTO planInfo);

    /**
     * 根据 ID 更新执行方案信息
     *
     * @param planInfo 执行方案信息
     * @return 是否更新成功
     */
    boolean updateTaskPlanById(TaskPlanInfoDTO planInfo);

    /**
     * 根据 ID 删除执行方案
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param planId     执行方案 ID
     * @return 是否删除成功
     */
    boolean deleteTaskPlanById(Long appId, Long templateId, Long planId);

    /**
     * 根据模版 ID 获取调试方案信息
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getDebugTaskPlan(Long appId, Long templateId);

    /**
     * 根据执行方案 ID 列表和查询条件查询执行方案数据
     *
     * @param appId               业务 ID
     * @param planIdList          执行方案 ID 列表
     * @param taskPlanQuery       执行方案查询条件
     * @param baseSearchCondition 通用查询条件
     * @return 符合要求的执行方案数据列表
     */
    List<TaskPlanInfoDTO> listTaskPlanByIds(Long appId, List<Long> planIdList, TaskPlanQueryDTO taskPlanQuery,
                                            BaseSearchCondition baseSearchCondition);

    /**
     * 根据执行方案IDs批量查询执行方案基础信息
     *
     * @param planIds 执行方案IDs
     * @return 执行方案基础信息列表
     */
    List<TaskPlanBasicInfoDTO> listTaskPlanBasicInfoByIds(Collection<Long> planIds);

    /**
     * 检查执行方案名称是否可用
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planId     执行方案 ID
     * @param name       执行方案名称
     * @return 名称是否可用
     */
    boolean checkPlanName(Long appId, Long templateId, Long planId, String name);

    /**
     * 保留执行方案 ID 插入执行方案
     *
     * @param planInfo 执行方案数据
     * @return 是否插入成功
     */
    boolean insertTaskPlanWithId(TaskPlanInfoDTO planInfo);

    /**
     * 根据执行方案 ID 查询执行方案名称
     *
     * @param planId 执行方案 ID
     * @return 执行方案名称
     */
    String getPlanName(long planId);

    /**
     * 根据作业模版 ID 和执行方案 ID 判断是否是调试模版
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param planId     执行方案 ID
     * @return 是否是调试模版
     */
    boolean isDebugPlan(Long appId, Long templateId, Long planId);

    /**
     * 检查执行方案 ID 是否被占用
     *
     * @param planId 执行方案 ID
     * @return 是否占用
     */
    boolean checkPlanId(Long planId);

    /**
     * 根据作业模版 ID 和执行方案名称获取执行方案数据
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @param name       执行方案名称
     * @return 执行方案数据
     */
    TaskPlanInfoDTO getTaskPlanByName(Long appId, Long templateId, String name);

    /**
     * 根据作业模版 ID 批量删除执行方案
     *
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @return 删除是否成功
     */
    boolean deleteTaskPlanByTemplate(Long appId, Long templateId);

    /**
     * 查询指定业务下是否存在有效执行方案
     *
     * @param appId 业务 ID
     * @return 是否存在有效模版
     */
    boolean isExistAnyAppPlan(Long appId);

    Integer countTaskPlans(Long appId);

    List<Long> listAllPlanId();
}
