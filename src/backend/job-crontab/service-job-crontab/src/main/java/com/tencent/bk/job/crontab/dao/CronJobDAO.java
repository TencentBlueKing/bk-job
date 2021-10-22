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

package com.tencent.bk.job.crontab.dao;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;

import java.util.List;

/**
 * @since 23/12/2019 17:32
 */
public interface CronJobDAO {

    /**
     * 根据业务 ID 列表批量查询定时任务信息
     *
     * @param cronJobCondition    查询参数
     * @param baseSearchCondition 分页信息
     * @return 分页的定时任务信息列表
     */
    PageData<CronJobInfoDTO> listPageCronJobsByCondition(CronJobInfoDTO cronJobCondition,
                                                         BaseSearchCondition baseSearchCondition);

    /**
     * 根据定时任务 ID 查询定时任务信息
     *
     * @param cronJobId 定时任务 ID
     * @return 定时任务信息
     */
    CronJobInfoDTO getCronJobById(long cronJobId);

    /**
     * 根据定时任务 IDs 查询定时任务信息
     *
     * @param cronJobIdList 定时任务 IDs
     * @return 定时任务信息
     */
    List<CronJobInfoDTO> getCronJobByIds(List<Long> cronJobIdList);

    /**
     * 根据定时任务 ID 查询定时任务信息
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 定时任务信息
     */
    CronJobInfoDTO getCronJobById(long appId, long cronJobId);

    /**
     * 根据定时任务 ID 查询定时任务错误信息，上次执行状态，错误码，错误次数
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 定时任务错误信息
     */
    CronJobInfoDTO getCronJobErrorById(long appId, long cronJobId);

    /**
     * 新增定时任务信息
     *
     * @param cronJob 定时任务信息
     * @return 新增定时任务信息的 ID
     */
    long insertCronJob(CronJobInfoDTO cronJob);

    /**
     * 根据 ID 和定时任务 ID 更新定时任务
     *
     * @param cronJob 定时任务信息
     * @return 是否更新成功
     */
    boolean updateCronJobById(CronJobInfoDTO cronJob);

    /**
     * 根据 ID 和定时任务 ID 更新定时任务错误信息
     *
     * @param cronJobErrorInfo 定时任务错误信息
     * @return 是否更新成功
     */
    boolean updateCronJobErrorById(CronJobInfoDTO cronJobErrorInfo);

    /**
     * 根据定时任务 ID 删除定时任务
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 是否删除成功
     */
    boolean deleteCronJobById(long appId, long cronJobId);

    /**
     * 检查定时任务名称是否可用
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @param name      定时任务名称
     * @return 是否可用
     */
    boolean checkCronJobName(long appId, long cronJobId, String name);

    /**
     * 根据执行方案 ID 获取定时任务基本信息
     *
     * @param appId  业务 ID
     * @param planId 执行方案 ID
     * @return 定时任务基本信息列表
     */
    List<CronJobInfoDTO> listCronJobByPlanId(long appId, long planId);

    /**
     * 保留 ID 插入定时任务
     *
     * @param cronJob 定时任务信息
     * @return 是否插入成功
     */
    boolean insertCronJobWithId(CronJobInfoDTO cronJob);

    /**
     * 根据定时任务 ID 查询定时任务名称
     *
     * @param id 定时任务 ID
     * @return 定时任务名称
     */
    String getCronJobNameById(long id);

    /**
     * 根据定时任务 ID 列表查询定时任务信息列表
     *
     * @param appId         业务 ID
     * @param cronJobIdList 定时任务 ID 列表
     * @return 定时任务信息列表
     */
    List<CronJobInfoDTO> listCronJobByIds(long appId, List<Long> cronJobIdList);

    /**
     * 查询业务下是否存在定时任务
     *
     * @param appId 业务 ID
     * @return 是否存在定时任务
     */
    boolean isExistAnyAppCronJob(Long appId);

    Integer countCronJob(Long appId, Boolean active, Boolean cron);
}
