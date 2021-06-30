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

package com.tencent.bk.job.manage.service.plan;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;

import java.util.List;
import java.util.Set;

/**
 * @since 16/10/2019 19:38
 */
public interface TaskPlanService {
    /**
     * 查询执行方案ID列表
     *
     * @param templateId 模版 ID
     * @return 执行方案ID列表
     */
    List<Long> listTaskPlanIds(Long templateId);
    /**
     * 查询执行方案列表
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 执行方案列表
     */
    List<TaskPlanInfoDTO> listPageTaskPlansBasicInfo(Long appId, Long templateId);

    /**
     * 查询执行方案基础信息列表，支持分页
     *
     * @param taskPlanQuery       执行方案查询条件
     * @param baseSearchCondition 基本查询条件
     * @param favoritePlanId      收藏的执行方案 ID
     * @return 执行方案基础信息列表
     */
    PageData<TaskPlanInfoDTO> listPageTaskPlansBasicInfo(
        TaskPlanQueryDTO taskPlanQuery, BaseSearchCondition baseSearchCondition, List<Long> favoritePlanId
    );

    /**
     * 根据 ID 查询执行方案信息
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param planId     执行方案 ID
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getTaskPlanById(Long appId, Long templateId, Long planId);

    /**
     * 根据 ID 查询执行方案信息
     *
     * @param planId 执行方案 ID
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getTaskPlanById(Long planId);

    /**
     * 根据 ID 查询执行方案信息
     *
     * @param appId  业务 ID
     * @param planId 执行方案 ID
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getTaskPlanById(Long appId, Long planId);

    /**
     * 保存执行方案信息
     *
     * @param taskPlanInfo 待保存的执行方案信息
     * @return 执行方案 ID
     */
    Long saveTaskPlan(TaskPlanInfoDTO taskPlanInfo);

    /**
     * 删除执行方案
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param planId     执行方案 ID
     * @return 是否删除成功
     */
    Boolean deleteTaskPlan(Long appId, Long templateId, Long planId);

    /**
     * 获取调试执行方案（不存在则创建）
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @return 执行方案信息
     */
    TaskPlanInfoDTO getDebugTaskPlan(String username, Long appId, Long templateId);

    /**
     * 根据执行方案 ID 列表批量获取执行方案基本信息
     *
     * @param appId      业务 ID
     * @param planIdList 执行方案 ID 列表
     * @return 执行方案基本信息列表
     */
    List<TaskPlanInfoDTO> listPlanBasicInfoByIds(Long appId, List<Long> planIdList);

    /**
     * 检查执行方案名称是否符合条件
     *
     * @param appId      业务 ID
     * @param templateId 模版 ID
     * @param planId     执行方案 ID
     * @param name       名称
     * @return 是否符合条件
     */
    Boolean checkPlanName(Long appId, Long templateId, Long planId, String name);

    /**
     * 同步执行方案
     *
     * @param appId           业务 ID
     * @param templateId      模版 ID
     * @param planId          执行方案 ID
     * @param templateVersion 模版版本
     * @return 是否符合条件
     */
    Boolean sync(Long appId, Long templateId, Long planId, String templateVersion);

    /**
     * 同步作业模版与执行方案
     *
     * @param taskPlan 执行方案信息
     */
    void syncPlan(TaskPlanInfoDTO taskPlan);

    /**
     * 新增执行方案（仅数据迁移用）
     *
     * @param username       用户名
     * @param appId          业务 ID
     * @param templateId     作业模版 ID
     * @param planId         执行方案 ID
     * @param createTime     创建时间
     * @param lastModifyTime 最后更新时间
     * @param lastModifyUser 最后更新人
     * @return 新增的执行方案 ID
     */
    Long saveTaskPlanForMigration(
        String username, Long appId, Long templateId, Long planId, Long createTime,
        Long lastModifyTime, String lastModifyUser
    );

    /**
     * 保留执行方案 ID 插入执行方案
     *
     * @param planInfo 执行方案数据
     * @return 是否插入成功
     */
    Boolean saveTaskPlanWithId(TaskPlanInfoDTO planInfo);

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
     * 新增执行方案（仅导入导出用）
     *
     * @param taskPlanInfo 执行方案信息
     * @return 新增的执行方案 ID
     */
    Long saveTaskPlanForBackup(TaskPlanInfoDTO taskPlanInfo);

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

    Set<String> listLocalFiles();

    boolean batchUpdatePlanVariable(List<TaskPlanInfoDTO> planInfoList);
}
