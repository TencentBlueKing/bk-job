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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.ServiceInnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceAddInnerCronJobRequestDTO;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;

import java.util.List;
import java.util.Map;

/**
 * @since 2/1/2020 12:15
 */
public interface CronJobService {
    /**
     * 分页查询定时任务列表
     *
     * @param cronJobCondition    查询条件
     * @param baseSearchCondition 搜索参数
     * @return 分页后的定时任务列表
     */
    PageData<CronJobInfoDTO> listPageCronJobInfos(CronJobInfoDTO cronJobCondition,
                                                  BaseSearchCondition baseSearchCondition);

    /**
     * 根据 ID 查询定时任务信息
     *
     * @param cronJobId 定时任务 ID
     * @return 定时任务信息
     */
    CronJobInfoDTO getCronJobInfoById(Long cronJobId);

    /**
     * 根据 IDs 查询定时任务信息，按照传入的id顺序返回结果
     *
     * @param cronJobIdList 定时任务 IDs
     * @return 定时任务信息
     */
    List<CronJobInfoDTO> getOrderedCronJobInfoByIds(List<Long> cronJobIdList);

    /**
     * 根据 IDs 查询定时任务信息，返回Map
     *
     * @param cronJobIdList 定时任务 IDs
     * @return 定时任务信息
     */
    Map<Long, CronJobInfoDTO> getCronJobInfoMapByIds(List<Long> cronJobIdList);

    /**
     * 根据 ID 查询定时任务信息
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 定时任务信息
     */
    CronJobInfoDTO getCronJobInfoById(Long appId, Long cronJobId);

    /**
     * 根据 ID 查询定时任务错误信息，上次执行状态，错误码，错误次数
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 定时任务错误信息
     */
    CronJobInfoDTO getCronJobErrorInfoById(Long appId, Long cronJobId);

    /**
     * 根据 ID 和定时任务 ID 更新定时任务上次执行状态，错误码，错误次数简单信息
     *
     * @param cronJobErrorInfo 定时任务错误信息
     * @return 是否更新成功
     */
    boolean updateCronJobErrorById(CronJobInfoDTO cronJobErrorInfo);

    /**
     * 新增、保存定时任务信息
     *
     * @param cronJobInfo 待新增、保存的定时任务信息
     * @return 定时任务 ID
     */
    Long saveCronJobInfo(CronJobInfoDTO cronJobInfo);

    /**
     * 删除定时任务
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 是否删除成功
     */
    Boolean deleteCronJobInfo(Long appId, Long cronJobId);

    /**
     * 启用、禁用定时任务
     *
     * @param username  用户名
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @return 是否操作成功
     */
    Boolean changeCronJobEnableStatus(String username, Long appId, Long cronJobId, Boolean enable);

    /**
     * 禁用过期任务
     *
     * @param appId          业务 ID
     * @param cronJobId      定时任务 ID
     * @param lastModifyUser 最后修改人
     * @param lastModifyTime 最后修改时间
     * @return 是否操作成功
     */
    Boolean disableExpiredCronJob(Long appId, Long cronJobId, String lastModifyUser, Long lastModifyTime);

    /**
     * 检查定时任务名称是否可用
     *
     * @param appId     业务 ID
     * @param cronJobId 定时任务 ID
     * @param name      定时任务名称
     * @return 是否可用
     */
    Boolean checkCronJobName(Long appId, Long cronJobId, String name);

    /**
     * 根据执行方案 ID 获取定时任务基本信息
     *
     * @param appId  业务 ID
     * @param planId 执行方案 ID
     * @return 定时任务基本信息列表
     */
    List<CronJobInfoDTO> listCronJobByPlanId(Long appId, Long planId);

    /**
     * 根据执行方案 ID 列表批量查询定时任务基本信息
     *
     * @param appId   业务 ID
     * @param planIds 执行方案 ID 列表
     * @return 定时任务基本信息列表
     */
    Map<Long, List<CronJobInfoDTO>> listCronJobByPlanIds(Long appId, List<Long> planIds);

    /**
     * 新增内部定时作业
     *
     * @param request 新增内部定时作业请求信息
     * @return 是否新增成功
     */
    Boolean addInnerJob(ServiceAddInnerCronJobRequestDTO request);

    /**
     * 获取内部定时任务信息
     *
     * @param systemId 系统 ID
     * @param jobKey   作业 Key
     * @return 内部定时任务信息
     */
    ServiceInnerCronJobInfoDTO getInnerJobInfo(String systemId, String jobKey);

    /**
     * 删除内部定时任务
     *
     * @param systemId 系统 ID
     * @param jobKey   作业 Key
     * @return 是否删除成功
     */
    Boolean deleteInnerCronJob(String systemId, String jobKey);

    /**
     * 根据定时任务 ID 批量执行历史
     *
     * @param appId      业务 ID
     * @param cronIdList 定时任务 ID 列表
     * @return 定时任务 ID 与执行历史对应表
     */
    Map<Long, ServiceCronTaskExecuteResultStatistics> getCronJobExecuteHistory(Long appId, List<Long> cronIdList);

    /**
     * 批量更新定时任务信息
     * <p>
     * 只更新 变量 和 启用 字段
     *
     * @param appId                 业务 ID
     * @param batchUpdateCronJobReq 批量更新请求
     * @return 是否更新成功
     */
    Boolean batchUpdateCronJob(Long appId, BatchUpdateCronJobReq batchUpdateCronJobReq);

    /**
     * 带 ID 新建定时任务
     *
     * @param cronJobInfo 定时任务信息
     * @return 定时任务 ID
     */
    Long insertCronJobInfoWithId(CronJobInfoDTO cronJobInfo);

    /**
     * 根据定时任务 ID 查询定时任务名称
     *
     * @param id 定时任务 ID
     * @return 定时任务名称
     */
    String getCronJobNameById(long id);

    /**
     * 根据定时任务 ID 批量拉取定时任务信息
     *
     * @param appId         业务 ID
     * @param cronJobIdList 定时任务 ID 列表
     * @return 定时任务信息列表
     */
    List<CronJobInfoDTO> listCronJobByIds(Long appId, List<Long> cronJobIdList);

    /**
     * 查询业务下是否存在定时任务
     *
     * @param appId 业务 ID
     * @return 是否存在定时任务
     */
    boolean isExistAnyAppCronJob(Long appId);

    Integer countCronJob(Long appId, Boolean active, Boolean cron);
}
