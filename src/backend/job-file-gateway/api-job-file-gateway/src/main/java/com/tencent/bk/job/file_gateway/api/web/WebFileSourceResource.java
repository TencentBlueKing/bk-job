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

package com.tencent.bk.job.file_gateway.api.web;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import com.tencent.bk.job.file_gateway.model.resp.web.FileSourceVO;
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

@Api(tags = {"job-file-gateway:web:FileSource"})
@RequestMapping("/web/fileSource/app/{appId}")
@RestController
@WebAPI
public interface WebFileSourceResource {

    @ApiOperation(value = "检查文件源别名是否已存在（可用返回true）", produces = "application/json")
    @GetMapping("/checkAlias/{alias}")
    ServiceResponse<Boolean> checkAlias(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "文件源别名")
        @PathVariable String alias,
        @ApiParam(value = "文件源 ID", required = false)
        @RequestParam(value = "fileSourceId", required = false) Integer fileSourceId
    );

    @ApiOperation(value = "新增文件源", produces = "application/json")
    @PostMapping("")
    ServiceResponse<Integer> saveFileSource(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "创建文件源请求")
        @RequestBody FileSourceCreateUpdateReq fileSourceCreateUpdateReq);

    @ApiOperation(value = "更新文件源", produces = "application/json")
    @PutMapping("")
    ServiceResponse<Integer> updateFileSource(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "更新文件源请求")
        @RequestBody FileSourceCreateUpdateReq fileSourceCreateUpdateReq);

    @ApiOperation(value = "删除文件源", produces = "application/json")
    @DeleteMapping("/ids/{id}")
    ServiceResponse<Integer> deleteFileSource(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "文件源 ID", required = true)
        @PathVariable("id") Integer id);

    @ApiOperation(value = "启用/禁用文件源", produces = "application/json")
    @PutMapping("/ids/{id}/enable")
    ServiceResponse<Boolean> enableFileSource(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "文件源 ID", required = true)
        @PathVariable("id") Integer id,
        @ApiParam(value = "是否开启", required = true)
        @RequestParam("flag") Boolean enableFlag
    );

    @ApiOperation(value = "获取文件源详情", produces = "application/json")
    @GetMapping("/ids/{id}")
    ServiceResponse<FileSourceVO> getFileSourceDetail(
        @ApiParam(value = "用户名，网关自动传入", required = true)
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam(value = "文件源 ID", required = true)
        @PathVariable("id") Integer id);

    @ApiOperation(value = "获取可使用的文件源列表", produces = "application/json")
    @GetMapping("/available/list")
    ServiceResponse<PageData<FileSourceVO>> listAvailableFileSource(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam("凭证ID")
        @RequestParam(value = "credentialId", required = false)
            String credentialId,
        @ApiParam("别名")
        @RequestParam(value = "alias", required = false)
            String alias,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize);

    @ApiOperation(value = "获取可管理的工作台文件源列表", produces = "application/json")
    @GetMapping("/workTable/list")
    ServiceResponse<PageData<FileSourceVO>> listWorkTableFileSource(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username") String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId") Long appId,
        @ApiParam("凭证ID")
        @RequestParam(value = "credentialId", required = false)
            String credentialId,
        @ApiParam("别名")
        @RequestParam(value = "alias", required = false)
            String alias,
        @ApiParam("分页-开始")
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam("分页-每页大小")
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize);

    @ApiOperation(value = "获取文件源类型的静态参数", produces = "application/json")
    @GetMapping("/fileSourceParams")
    ServiceResponse<List<FileSourceStaticParam>> getFileSourceParams(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiParam(value = "业务 ID", required = true)
        @PathVariable("appId")
            Long appId,
        @ApiParam(value = "文件源类型Code，来源于fileSourceType的list接口返回中的code字段", required = true)
        @RequestParam(value = "fileSourceTypeCode")
            String fileSourceTypeCode);
}
