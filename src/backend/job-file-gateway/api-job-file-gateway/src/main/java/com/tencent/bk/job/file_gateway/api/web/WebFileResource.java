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
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.file_gateway.model.req.common.ExecuteActionReq;
import com.tencent.bk.job.file_gateway.model.resp.common.FileNodesVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = {"job-file-gateway:web:File"})
@RequestMapping("/web/file/scope/{scopeType}/{scopeId}/fileSourceIds/{fileSourceId}")
@RestController
@WebAPI
public interface WebFileResource {

    // 子路径与FileWorker子路径保持一致
    @ApiOperation(value = "获取文件源某个节点下的子节点列表", produces = "application/json")
    @GetMapping("/listFileNode")
    Response<FileNodesVO> listFileNode(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "文件源ID", required = true)
        @PathVariable(value = "fileSourceId", required = true)
            Integer fileSourceId,
        @ApiParam(value = "文件路径（不传默认为根目录）", required = false)
        @RequestParam(value = "path", required = false)
            String path,
        @ApiParam(value = "搜索字段：文件名称", required = false)
        @RequestParam(value = "name", required = false)
            String name,
        @ApiParam(value = "分页-开始", required = false)
        @RequestParam(value = "start", required = false)
            Integer start,
        @ApiParam(value = "分页-每页大小", required = false)
        @RequestParam(value = "pageSize", required = false)
            Integer pageSize
    );

    @ApiOperation(value = "执行动作", produces = "application/json")
    @PostMapping("/executeAction")
    Response<Boolean> executeAction(
        @ApiParam("用户名，网关自动传入")
        @RequestHeader("username")
            String username,
        @ApiIgnore
        @RequestAttribute(value = "appResourceScope")
            AppResourceScope appResourceScope,
        @ApiParam(value = "资源范围类型", required = true)
        @PathVariable(value = "scopeType")
            String scopeType,
        @ApiParam(value = "资源范围ID", required = true)
        @PathVariable(value = "scopeId")
            String scopeId,
        @ApiParam(value = "文件源ID", required = true, example = "2")
        @PathVariable(value = "fileSourceId", required = true)
            Integer fileSourceId,
        @ApiParam(value = "执行动作请求体", required = true)
        @RequestBody ExecuteActionReq req
    );
}
