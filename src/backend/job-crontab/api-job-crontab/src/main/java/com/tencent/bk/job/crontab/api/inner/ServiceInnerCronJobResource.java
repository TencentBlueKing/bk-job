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

package com.tencent.bk.job.crontab.api.inner;

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.crontab.model.inner.ServiceInnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.request.ServiceAddInnerCronJobRequestDTO;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @since 18/2/2020 15:11
 */
@Api(tags = {"Inner_Cron_Job"})
@RequestMapping("/service/inner/cron/job")
@RestController
@EsbAPI
public interface ServiceInnerCronJobResource {

    /**
     * 新增定时任务
     *
     * @param systemId 系统 ID
     * @param jobKey   任务 Key
     * @param request  作业详情
     * @return 是否创建成功
     */
    @PutMapping("/{systemId}/{jobKey}")
    InternalResponse<Boolean> addNewCronJob(
        @PathVariable("systemId") String systemId,
        @PathVariable("jobKey") String jobKey, @RequestBody ServiceAddInnerCronJobRequestDTO request
    );

    /**
     * 根据任务 Key 拉定时任务详情
     *
     * @param systemId 系统 ID
     * @param jobKey   任务 Key
     * @return 定时任务详情
     */
    @GetMapping("/{systemId}/{jobKey}")
    InternalResponse<ServiceInnerCronJobInfoDTO> getCronJobInfoByKey(
        @PathVariable("systemId") String systemId,
        @PathVariable("jobKey") String jobKey
    );

    /**
     * 根据任务 Key 删除定时任务
     *
     * @param systemId 系统 ID
     * @param jobKey   任务 Key
     * @return 删除是否成功
     */
    @DeleteMapping("/{systemId}/{jobKey}")
    InternalResponse<Boolean> deleteCronJob(
        @PathVariable("systemId") String systemId,
        @PathVariable("jobKey") String jobKey
    );

    /**
     * 根据系统 ID 批量拉取定时任务列表
     *
     * @param systemId 系统 ID
     * @return 定时任务列表
     */
    @GetMapping("/{systemId}")
    InternalResponse<List<ServiceInnerCronJobInfoDTO>> listCronJobs(@PathVariable("systemId") String systemId);

}
