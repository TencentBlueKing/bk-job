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

package com.tencent.bk.job.crontab.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.CronJobCreateUpdateReq;
import com.tencent.bk.job.crontab.model.CronJobLaunchHistoryVO;
import com.tencent.bk.job.crontab.model.CronJobVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = {"job-crontab:web:Cron_Job"})
@RequestMapping("/web/scope/{scopeType}/{scopeId}/cron/job")
@RestController
@WebAPI
public interface WebCronJobResource {

    /**
     * 获取定时任务列表
     *
     * @param username       用户名
     * @param scopeType      资源范围类型
     * @param scopeId        资源范围ID
     * @param cronJobId      定时任务 ID
     * @param planId         执行方案 ID
     * @param name           定时任务名称
     * @param creator        创建人
     * @param lastModifyUser 更新人
     * @param start          分页起始
     * @param pageSize       每页大小
     * @param orderField     排序字段
     * @param order          排序顺序
     * @return 带分页信息的定时任务列表
     */
    @ApiOperation(value = "获取定时任务列表", produces = "application/json")
    @GetMapping
    Response<PageData<CronJobVO>> listCronJobs(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID")
        @RequestParam(value = "cronJobId", required = false)
            Long cronJobId,
        @ApiParam(value = "执行方案 ID")
        @RequestParam(value = "planId", required = false)
            Long planId,
        @ApiParam(value = "定时任务名称")
        @RequestParam(value = "name", required = false)
            String name,
        @ApiParam(value = "创建人")
        @RequestParam(value = "creator", required = false)
            String creator,
        @ApiParam(value = "更新人")
        @RequestParam(value = "lastModifyUser", required = false)
            String lastModifyUser,
        @ApiParam(value = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize,
        @ApiParam(value = "排序字段")
        @RequestParam(value = "orderField", required = false)
            String orderField,
        @ApiParam(value = "排序顺序 0-降序 1-升序")
        @RequestParam(value = "order", required = false)
            Integer order
    );

    /**
     * 获取定时任务执行状态信息列表
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID 列表
     * @return 定时任务执行状态信息列表
     */
    @ApiOperation(value = "获取定时任务执行状态信息列表", produces = "application/json")
    @GetMapping("/statistic")
    Response<List<CronJobVO>> listCronJobStatistic(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID 列表，逗号分隔")
        @RequestParam(value = "cronJobIds")
            List<Long> cronJobId
    );

    /**
     * 获取定时任务信息
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID
     * @return 定时任务信息
     */
    @ApiOperation(value = "获取定时任务信息", produces = "application/json")
    @GetMapping("/{cronJobId}")
    Response<CronJobVO> getCronJobById(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID", required = true) @PathVariable(value = "cronJobId")
            Long cronJobId
    );

    /**
     * 更新定时任务
     *
     * @param username               用户名
     * @param scopeType              资源范围类型
     * @param scopeId                资源范围ID
     * @param cronJobId              定时任务 ID
     * @param cronJobCreateUpdateReq 定时任务信息
     * @return 定时任务 ID
     */
    @ApiOperation(value = "更新定时任务", produces = "application/json")
    @PutMapping("/{cronJobId}")
    Response<Long> saveCronJob(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID，新建时填 0", required = true)
        @PathVariable("cronJobId")
            Long cronJobId,
        @ApiParam(value = "更新的定时任务对象", name = "cronJobCreateUpdateReq", required = true)
        @RequestBody
            CronJobCreateUpdateReq cronJobCreateUpdateReq
    );

    /**
     * 删除定时任务
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID
     * @return 删除是否成功
     */
    @ApiOperation(value = "删除定时任务", produces = "application/json")
    @DeleteMapping("/{cronJobId}")
    Response<Boolean> deleteCronJob(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID", required = true)
        @PathVariable("cronJobId")
            Long cronJobId
    );

    /**
     * 修改定时任务状态
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID
     * @param enable    状态
     * @return 修改是否成功
     */
    @ApiOperation(value = "启用、禁用定时任务", produces = "application/json")
    @PutMapping("/{cronJobId}/status")
    Response<Boolean> changeCronJobEnableStatus(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID", required = true)
        @PathVariable("cronJobId")
            Long cronJobId,
        @ApiParam(value = "启用状态", required = true)
        @RequestParam(value = "enable", required = true)
            Boolean enable
    );

    /**
     * 查询定时任务启动记录
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID
     * @param start     分页起始
     * @param pageSize  每页大小
     * @return 带分页信息的定时任务启动记录列表
     */
    @ApiOperation(value = "定时任务启动记录", produces = "application/json")
    @GetMapping("/{cronJobId}/history")
    Response<PageData<CronJobLaunchHistoryVO>> getCronJobLaunchHistory(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID", required = true)
        @PathVariable("cronJobId")
            Long cronJobId,
        @ApiParam(value = "分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    /**
     * 检查定时任务名称是否可用
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param cronJobId 定时任务 ID
     * @param name      定时任务名称
     * @return 是否可用
     */
    @ApiOperation(value = "检查定时任务名称是否可用", produces = "application/json")
    @GetMapping("/{cronJobId}/check_name")
    Response<Boolean> checkCronJobName(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "定时任务 ID，新建时填 0", required = true)
        @PathVariable("cronJobId") Long cronJobId,
        @ApiParam(value = "名称", required = true)
        @RequestParam(value = "name")
            String name
    );

    /**
     * 批量更新定时任务信息
     * <p>
     * 只更新 变量 和 启用 字段
     *
     * @param username              用户名
     * @param scopeType             资源范围类型
     * @param scopeId               资源范围ID
     * @param batchUpdateCronJobReq 批量更新请求
     * @return 是否更新成功
     */
    @Deprecated
    @ApiOperation(value = "批量更新定时任务信息 只更新 变量 和 启用 字段", produces = "application/json")
    @PostMapping("/batch_update")
    Response<Boolean> batchUpdateCronJob(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "批量更新定时任务请求", name = "batchUpdateCronJobReq", required = true)
        @RequestBody
            BatchUpdateCronJobReq batchUpdateCronJobReq
    );

    /**
     * 根据执行方案 ID 批量拉取定时任务信息
     *
     * @param username  用户名
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @param planId    执行方案 ID
     * @return 定时任务信息列表
     */
    @ApiOperation(value = "根据执行方案 ID 查询定时任务信息", produces = "application/json")
    @GetMapping("/plan/{planId}")
    Response<List<CronJobVO>> getCronJobListByPlanId(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "执行方案 ID", required = true)
        @PathVariable(value = "planId")
            Long planId
    );

    /**
     * 根据执行方案 ID 列表批量拉取定时任务信息
     *
     * @param username   用户名
     * @param scopeType  资源范围类型
     * @param scopeId    资源范围ID
     * @param planIdList 执行方案 ID 列表
     * @return 执行方案 ID 与定时任务信息列表对应表
     */
    @ApiOperation(value = "根据执行方案 ID 列表查询定时任务信息", produces = "application/json")
    @GetMapping("/plans/{planId}")
    Response<Map<Long, List<CronJobVO>>> getCronJobListByPlanIdList(
        @ApiParam(value = "用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "执行方案 ID 列表，逗号分隔", required = true)
        @PathVariable(value = "planId")
            List<Long> planIdList
    );

}
