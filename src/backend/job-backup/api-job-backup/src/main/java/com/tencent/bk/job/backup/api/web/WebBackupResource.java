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

package com.tencent.bk.job.backup.api.web;

import com.tencent.bk.job.backup.model.req.CheckPasswordRequest;
import com.tencent.bk.job.backup.model.req.ExportRequest;
import com.tencent.bk.job.backup.model.req.ImportRequest;
import com.tencent.bk.job.backup.model.web.BackupJobInfoVO;
import com.tencent.bk.job.backup.model.web.ExportInfoVO;
import com.tencent.bk.job.backup.model.web.ImportInfoVO;
import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.Response;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * @since 21/7/2020 15:42
 */
@Api(tags = {"job-backup:web:Backup"})
@RequestMapping("/web/app/{appId}/backup")
@RestController
@WebAPI
public interface WebBackupResource {

    @ApiOperation(value = "开始导出", produces = "application/json")
    @PostMapping("/export")
    Response<ExportInfoVO> startExport(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "作业导出请求", required = true) @RequestBody ExportRequest exportRequest
    );

    @ApiOperation(value = "获取导出任务信息", produces = "application/json")
    @GetMapping("/export/{id}")
    Response<ExportInfoVO> getExportInfo(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导出任务 ID", required = true) @PathVariable("id") String jobId
    );

    @ApiOperation(value = "下载导出文件", produces = "application/zip")
    @GetMapping("/export/{id}/download")
    ResponseEntity<StreamingResponseBody> getExportFile(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导出任务 ID", required = true) @PathVariable("id") String jobId
    );

    @ApiOperation(value = "完成导出任务", produces = "application/json")
    @PostMapping("/export/{id}/complete")
    Response<Boolean> completeExport(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导出任务 ID", required = true) @PathVariable("id") String jobId
    );

    @ApiOperation(value = "终止导出任务", produces = "application/json")
    @DeleteMapping("/export/{id}")
    Response<Boolean> abortExport(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导出任务 ID", required = true) @PathVariable("id") String jobId
    );

    @ApiOperation(value = "上传导入文件并获取导入文件信息", produces = "application/json")
    @PostMapping("/import/file")
    Response<ImportInfoVO> getImportFileInfo(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "待导入文件", required = true) @RequestParam("uploadFile") MultipartFile uploadFile
    );

    @ApiOperation(value = "校验密码", produces = "application/json")
    @PostMapping("/import/{id}/password")
    Response<Boolean> checkPassword(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导入任务 ID", required = true) @PathVariable("id") String jobId,
        @ApiParam(value = "密码", required = true) @RequestBody CheckPasswordRequest passwordRequest
    );

    @ApiOperation(value = "开始导入", produces = "application/json")
    @PostMapping("/import/{id}")
    Response<Boolean> startImport(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导入任务 ID", required = true) @PathVariable("id") String jobId,
        @ApiParam(value = "导入任务设置", required = true) @RequestBody ImportRequest importRequest
    );

    @ApiOperation(value = "获取导入信息", produces = "application/json")
    @GetMapping("/import/{id}")
    Response<ImportInfoVO> getImportInfo(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId,
        @ApiParam(value = "导入任务 ID", required = true) @PathVariable("id") String jobId
    );

    @ApiOperation(value = "获取当前用户的导入/导出任务列表", produces = "application/json")
    @GetMapping
    Response<BackupJobInfoVO> getCurrentJob(
        @ApiParam(value = "用户名，网关自动传入") @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true, example = "2") @PathVariable("appId") Long appId
    );
}
