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

package com.tencent.bk.job.crontab.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 定时任务 Resource API
 */
@Api(tags = {"Cron_Job"})
@SmartFeignClient(value = "job-crontab", contextId = "cronJobResource")
@InternalAPI
public interface ServiceCronJobResource {

    /**
     * 根据业务 ID 和定时任务状态拉取定时任务列表
     *
     * @param appId  业务 ID
     * @param enable 定时任务状态
     * @return 定时任务列表
     */
    @GetMapping("/service/app/{appId}/cron/job")
    InternalResponse<List<ServiceCronJobDTO>> listCronJobs(
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "是否开启", required = false, example = "true") @RequestParam("enable") Boolean enable
    );

    /**
     * 根据执行方案 ID 列表批量拉定时任务列表
     *
     * @param appId      业务 ID
     * @param planIdList 执行方案 ID 列表
     * @return 执行方案与定时任务列表对应表
     */
    @GetMapping("/service/app/{appId}/cron/job/plan")
    InternalResponse<Map<Long, List<CronJobVO>>> batchListCronJobByPlanIds(
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "执行方案 ID 列表", required = true) @RequestParam(value = "planId") List<Long> planIdList
    );
}
