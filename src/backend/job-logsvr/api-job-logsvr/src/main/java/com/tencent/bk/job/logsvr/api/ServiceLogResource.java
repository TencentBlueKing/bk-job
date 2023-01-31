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

package com.tencent.bk.job.logsvr.api;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogQueryRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 执行日志服务
 */
@Api(tags = {"Log"})
@RequestMapping("/service/log")
@RestController
@InternalAPI
public interface ServiceLogResource {

    /**
     * 保存执行日志
     * tmp: ipv6版本发布完成后删除
     * @param request 保存日志请求
     */
    @ApiOperation("保存执行日志")
    @PostMapping
    @Deprecated
    InternalResponse<?> saveLog(
        @ApiParam("保存日志请求报文")
        @RequestBody ServiceSaveLogRequest request
    );

    /**
     * 批量保存执行日志
     *
     * @param request 保存日志请求
     */
    @ApiOperation("批量保存执行日志")
    @PostMapping("/batch")
    InternalResponse<?> saveLogs(
        @ApiParam("批量保存日志请求报文")
        @RequestBody ServiceBatchSaveLogRequest request
    );

    @Deprecated
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容API,后续使用hostId查询", deprecatedVersion = "3.7.x")
    @ApiOperation("根据目标服务器IP获取脚本任务对应的执行日志")
    @GetMapping(value = {"/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/ip/{ip}"})
    InternalResponse<ServiceHostLogDTO> getScriptHostLogByIp(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("ip")
        @PathVariable("ip") String ip,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);

    @ApiOperation("根据目标主机ID获取脚本任务对应的执行日志")
    @GetMapping(value = {
        "/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/hostId/{hostId}"
    })
    InternalResponse<ServiceHostLogDTO> getScriptHostLogByHostId(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("hostId")
        @PathVariable("hostId") Long hostId,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);

    @ApiOperation("批量获取脚本任务对应的执行日志")
    @PostMapping(value = {"/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}"})
    InternalResponse<List<ServiceHostLogDTO>> listScriptLogs(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("查询请求")
        @RequestBody ServiceScriptLogQueryRequest query
    );

    @CompatibleImplementation(name = "rolling_execute", explain = "兼容API,后续使用hostId查询", deprecatedVersion = "3.7.x")
    @Deprecated
    @ApiOperation("按照IP获取文件任务对应的执行日志")
    @GetMapping(value = {"/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/ip/{ip}"})
    InternalResponse<ServiceHostLogDTO> getFileHostLogByIp(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("ip")
        @PathVariable("ip") String ip,
        @ApiParam("分发模式,0:upload,1:download")
        @RequestParam(value = "mode", required = false) Integer mode,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);

    @ApiOperation("按照hostId获取文件任务对应的执行日志")
    @GetMapping(value = {
        "/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/hostId/{hostId}"
    })
    InternalResponse<ServiceHostLogDTO> getFileHostLogByHostId(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("hostId")
        @PathVariable("hostId") Long hostId,
        @ApiParam("分发模式,0:upload,1:download")
        @RequestParam(value = "mode", required = false) Integer mode,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);

    @ApiOperation("获取文件任务对应的执行日志")
    @GetMapping("/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}")
    InternalResponse<List<ServiceFileTaskLogDTO>> listFileHostLogs(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam("分发模式,0:upload,1:download")
        @RequestParam(value = "mode", required = false) Integer mode,
        @ApiParam("ip")
        @RequestParam(value = "ip", required = false) String ip,
        @ApiParam("hostId")
        @RequestParam(value = "hostId", required = false) Long hostId
    );

    @ApiOperation("获取文件任务agent对应的执行日志")
    @PostMapping("/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/queryByTaskIds")
    InternalResponse<ServiceHostLogDTO> listFileHostLogsByTaskIds(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam("文件任务ID列表，多个任务ID以;分隔")
        @RequestBody List<String> taskIds
    );

    @ApiOperation("获取文件任务对应的执行日志")
    @PostMapping("/file")
    InternalResponse<ServiceHostLogsDTO> listFileHostLogs(@RequestBody ServiceFileLogQueryRequest request);

    /**
     * 返回日志内容包含关键字的主机ip
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param keyword        查询关键字
     * @return ip
     */
    @ApiOperation("根据脚本任务日志关键字获取对应的ip")
    @GetMapping("/keywordMatch/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}")
    InternalResponse<List<HostDTO>> questHostsByLogKeyword(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam("关键字")
        @RequestParam("keyword") String keyword
    );

}
