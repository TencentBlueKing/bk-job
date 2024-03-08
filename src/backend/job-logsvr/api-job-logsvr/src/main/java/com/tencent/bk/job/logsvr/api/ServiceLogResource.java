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
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceBatchSaveLogRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileLogQueryRequest;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogQueryRequest;
import com.tentent.bk.job.common.api.feign.annotation.SmartFeignClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 执行日志服务
 */
@Api(tags = {"Log"})
@SmartFeignClient(value = "job-logsvr", contextId = "logResource")
@InternalAPI
public interface ServiceLogResource {

    /**
     * 批量保存执行日志
     *
     * @param request 保存日志请求
     */
    @ApiOperation("批量保存执行日志")
    @PostMapping("/service/log/batch")
    InternalResponse<?> saveLogs(
        @ApiParam("批量保存日志请求报文")
        @RequestBody ServiceBatchSaveLogRequest request
    );

    @ApiOperation("根据目标主机ID获取脚本任务对应的执行日志")
    @GetMapping(value = {
        "/service/log/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/hostId/{hostId}"
    })
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用 hostId 的查询方式")
    InternalResponse<ServiceExecuteObjectLogDTO> getScriptHostLogByHostId(
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

    @ApiOperation("根据目标执行对象ID获取脚本任务对应的执行日志")
    @GetMapping(value = {
        "/service/log/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/executeObject" +
            "/{executeObjectId}"
    })
    InternalResponse<ServiceExecuteObjectLogDTO> getScriptLogByExecuteObjectId(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("executeObjectId")
        @PathVariable("executeObjectId") String executeObjectId,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);

    @ApiOperation("批量获取脚本任务对应的执行日志")
    @PostMapping(value = {"/service/log/script/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry" +
        "/{executeCount}"})
    InternalResponse<List<ServiceExecuteObjectLogDTO>> listScriptExecuteObjectLogs(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("查询请求")
        @RequestBody ServiceScriptLogQueryRequest query
    );

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容历史数据使用 hostId 的查询方式")
    @ApiOperation("按照hostId获取文件任务对应的执行日志")
    @GetMapping(value = {
        "/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/hostId/{hostId}"
    })
    InternalResponse<ServiceExecuteObjectLogDTO> getFileHostLogByHostId(
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

    @ApiOperation("按照执行对象ID获取文件任务对应的执行日志")
    @GetMapping(value = {
        "/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/executeObject" +
            "/{executeObjectId}"
    })
    InternalResponse<ServiceExecuteObjectLogDTO> getFileLogByExecuteObjectId(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("executeObjectId")
        @PathVariable("executeObjectId") String executeObjectId,
        @ApiParam("分发模式,0:upload,1:download")
        @RequestParam(value = "mode", required = false) Integer mode,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch);


    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "使用 listFileExecuteObjectLogs() 替换，发布完成后可删除")
    @ApiOperation("获取文件任务对应的执行日志")
    @GetMapping("/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}")
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

    @ApiOperation("获取文件任务对应的执行日志")
    @PostMapping(
        "/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/queryByTaskIds")
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "返回的协议内容有问题，发布完成后可删除")
    InternalResponse<ServiceExecuteObjectLogDTO> listFileLogsByTaskIds(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam("文件任务ID列表")
        @RequestBody List<String> taskIds
    );

    @ApiOperation("根据任务 ID 批量获取文件任务对应的执行日志")
    @PostMapping(
        "/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}" +
            "/listTaskFileLogsByTaskIds")
    InternalResponse<List<ServiceFileTaskLogDTO>> listTaskFileLogsByTaskIds(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("滚动执行批次，非滚动任务传入null")
        @RequestParam(value = "batch", required = false) Integer batch,
        @ApiParam("文件任务ID列表")
        @RequestBody List<String> taskIds
    );

    @ApiOperation("获取文件任务对应的执行日志")
    @PostMapping("/file")
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布之后不再被使用，可删除")
    InternalResponse<ServiceExecuteObjectLogsDTO> listFileHostLogs(@RequestBody ServiceFileLogQueryRequest request);

    @ApiOperation("批量获取文件任务对应的执行日志")
    @PostMapping(
        value = {"/service/log/file/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}"})
    InternalResponse<List<ServiceExecuteObjectLogDTO>> listFileExecuteObjectLogs(
        @ApiParam("作业创建时间")
        @PathVariable("jobCreateDate") String jobCreateDate,
        @ApiParam("步骤实例ID")
        @PathVariable("stepInstanceId") Long stepInstanceId,
        @ApiParam("执行次数")
        @PathVariable("executeCount") Integer executeCount,
        @ApiParam("查询请求")
        @RequestBody ServiceFileLogQueryRequest request
    );

    /**
     * 返回日志内容包含关键字的主机ip
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param keyword        查询关键字
     * @return ip
     */
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA)
    @Deprecated
    @ApiOperation("根据脚本任务日志关键字获取对应的ip")
    @GetMapping("/service/log/keywordMatch/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}")
    InternalResponse<List<HostDTO>> queryHostsByLogKeyword(
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

    /**
     * 返回日志内容包含关键字的执行对象 ID 集合
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param keyword        查询关键字
     * @return 执行对象 ID 集合
     */
    @ApiOperation("根据脚本任务日志关键字获取对应的执行对象ID集合")
    @GetMapping("/service/log/jobCreateDate/{jobCreateDate}/step/{stepInstanceId}/retry/{executeCount}/executeObject")
    InternalResponse<List<String>> queryExecuteObjectsByLogKeyword(
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
