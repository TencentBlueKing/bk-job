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

package com.tencent.bk.job.analysis.api.web;

import com.tencent.bk.job.analysis.consts.DimensionEnum;
import com.tencent.bk.job.analysis.consts.DistributionMetricEnum;
import com.tencent.bk.job.analysis.consts.ResourceEnum;
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
import com.tencent.bk.job.analysis.model.web.CommonDistributionVO;
import com.tencent.bk.job.analysis.model.web.CommonStatisticWithRateVO;
import com.tencent.bk.job.analysis.model.web.CommonTrendElementVO;
import com.tencent.bk.job.analysis.model.web.DayDistributionElementVO;
import com.tencent.bk.job.analysis.model.web.PerAppStatisticVO;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.ServiceResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = {"job-analysis:web:统计量统计接口"})
@RequestMapping("/web/statistics")
@RestController
@WebAPI
public interface WebStatisticsResource {

    @ApiOperation(value = "查询某个统计量的统计信息", produces = "application/json")
    @GetMapping("/total/metrics/{metric}")
    ServiceResponse<CommonStatisticWithRateVO> totalStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("统计量Code，取值：(\n" +
            "APP_COUNT:接入业务量\n" +
            "ACTIVE_APP_COUNT:活跃业务量\n" +
            "TASK_TEMPLATE_COUNT:作业模板量\n" +
            "TASK_PLAN_COUNT:执行方案量\n" +
            "SCRIPT_COUNT:脚本量\n" +
            "EXECUTED_TASK_COUNT:累计任务执行量\n" +
            "FAILED_TASK_COUNT:任务执行失败量\n" +
            ")")
        @PathVariable(value = "metric", required = true)
            TotalMetricEnum metric,
        @ApiParam("业务Id列表，不传为全业务")
        @RequestParam(value = "appIdList", required = false)
            List<Long> appIdList,
        @ApiParam("统计日期，例如：2020-12-16，不传默认为今天")
        @RequestParam(value = "date", required = false)
            String date
    );

    @ApiOperation(value = "查询某个统计量的趋势", produces = "application/json")
    @GetMapping("/trends/metrics/{metric}")
    ServiceResponse<List<CommonTrendElementVO>> trends(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("统计量Code，取值：(\n" +
            "APP_COUNT:接入业务量\n" +
            "ACTIVE_APP_COUNT:活跃业务量\n" +
            "TASK_TEMPLATE_COUNT:作业模板量\n" +
            "TASK_PLAN_COUNT:执行方案量\n" +
            "SCRIPT_COUNT:脚本量\n" +
            "EXECUTED_TASK_COUNT:累计任务执行量\n" +
            "FAILED_TASK_COUNT:任务执行失败量\n" +
            ")")
        @PathVariable(value = "metric", required = true)
            TotalMetricEnum metric,
        @ApiParam("业务Id列表，不传为全业务")
        @RequestParam(value = "appIdList", required = false)
            List<Long> appIdList,
        @ApiParam("统计起始日期，例如：2019-01-30，本期需求先不传这个参数，按默认最小值处理")
        @RequestParam(value = "dataStartDate", required = false)
            String dataStartDate,
        @ApiParam("查询起始日期，不传默认为今天，例如：2019-01-30")
        @RequestParam(value = "startDate", required = false)
            String startDate,
        @ApiParam("查询终止日期，不传默认为今天，例如：2019-01-30")
        @RequestParam(value = "endDate", required = false)
            String endDate
    );

    @ApiOperation(value = "查询某个统计量的逐业务统计列表", produces = "application/json")
    @GetMapping("/listByPerApp/metrics/{metric}")
    ServiceResponse<List<PerAppStatisticVO>> listByPerApp(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("统计量Code，取值：(\n" +
            "APP_COUNT:接入业务量\n" +
            "ACTIVE_APP_COUNT:活跃业务量\n" +
            "TASK_TEMPLATE_COUNT:作业模板量\n" +
            "TASK_PLAN_COUNT:执行方案量\n" +
            "SCRIPT_COUNT:脚本量\n" +
            "EXECUTED_TASK_COUNT:累计任务执行量\n" +
            "FAILED_TASK_COUNT:任务执行失败量\n" +
            ")")
        @PathVariable(value = "metric", required = true)
            TotalMetricEnum metric,
        @ApiParam("业务Id列表，不传为全业务")
        @RequestParam(value = "appIdList", required = false)
            List<Long> appIdList,
        @ApiParam("统计日期，不传默认为今天，例如：2020-12-17")
        @RequestParam(value = "date", required = false)
            String date
    );

    @ApiOperation(value = "查询某个统计量的分布信息", produces = "application/json")
    @GetMapping("/distribution/metrics/{metric}")
    ServiceResponse<CommonDistributionVO> distributionStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("统计量Code，取值：(\n" +
            "HOST_SYSTEM_TYPE:主机操作系统类型（结果label取值：LINUX，WINDOWS，AIX，OTHERS）\n" +
            "STEP_TYPE:作业步骤类型（结果label取值：FILE_LOCAL，FILE_SERVER，SCRIPT_MANUAL，SCRIPT_REF，CONFIRM）\n" +
            "SCRIPT_TYPE:脚本类型（结果label取值：Bat，Shell，Perl，Powershell，Python，SQL）\n" +
            "SCRIPT_VERSION_STATUS:脚本版本状态（结果label取值：ONLINE，OFFLINE，PREPARING，FORBIDDEN）\n" +
            "CRON_STATUS:定时任务状态（结果label取值：OPEN，CLOSED）\n" +
            "CRON_TYPE:定时任务类型（结果label取值：SIMPLE，CRON）\n" +
            "TAG:标签（结果label取值：真实标签名，用户数据，无需国际化）\n" +
            "ACCOUNT_TYPE:账号类型（结果label取值：LINUX，WINDOWS，DB）\n" +
            ")")
        @PathVariable(value = "metric", required = true)
            DistributionMetricEnum metric,
        @ApiParam("业务Id列表，不传为全业务")
        @RequestParam(value = "appIdList", required = false)
            List<Long> appIdList,
        @ApiParam("统计日期，例如：2020-12-16，不传默认为今天")
        @RequestParam(value = "date", required = false)
            String date
    );

    @ApiOperation(value = "查询某种资源某个维度下的每日统计详情", produces = "application/json")
    @GetMapping("/resources/{resource}/dimensions/{dimension}")
    ServiceResponse<List<DayDistributionElementVO>> dayDetailStatistics(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam("资源类型，取值：(\n" +
            "EXECUTED_TASK:执行过的任务（包含快速执行脚本、快速分发文件、作业）（对应dimension取值：TASK_STARTUP_MODE，TASK_TYPE，TASK_TIME_CONSUMING" +
            "）\n" +
            "EXECUTED_FAST_SCRIPT:执行过的快速执行脚本（对应dimension取值：SCRIPT_TYPE）\n" +
            "EXECUTED_FAST_FILE:执行过的快速分发文件（对应dimension取值：FILE_TRANSFER_MODE）\n" +
            ")")
        @PathVariable(value = "resource", required = true)
            ResourceEnum resource,
        @ApiParam("维度，取值：(\n" +
            "TASK_STARTUP_MODE:任务启动方式\n" +
            "TASK_TYPE:任务类型\n" +
            "TASK_TIME_CONSUMING:任务耗时\n" +
            "SCRIPT_TYPE:快速执行脚本的脚本类型\n" +
            "FILE_TRANSFER_MODE:快速分发文件的传输模式\n" +
            ")")
        @PathVariable(value = "dimension", required = true)
            DimensionEnum dimension,
        @ApiParam("业务Id列表，不传为全业务")
        @RequestParam(value = "appIdList", required = false)
            List<Long> appIdList,
        @ApiParam("查询起始日期，不传默认为今天，例如：2019-01-30")
        @RequestParam(value = "startDate", required = false)
            String startDate,
        @ApiParam("查询终止日期，不传默认为今天，例如：2019-01-30")
        @RequestParam(value = "endDate", required = false)
            String endDate
    );

    @ApiOperation(value = "查询统计数据起始日期、更新时间等信息（可能的Key：{起始日期:STATISTICS_DATA_START_DATE, " +
        "更新时间:STATISTICS_DATA_UPDATE_TIME}）", produces = "application/json")
    @GetMapping("/info")
    ServiceResponse<Map<String, String>> getStatisticsDataInfo(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username
    );

}
